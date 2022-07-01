package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.compressIfNeeded;
import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;
import static io.quarkiverse.quinoa.QuinoaRecorder.shouldHandleMethod;

import java.util.List;

import org.jboss.logging.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

class QuinoaDevProxyHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaDevProxyHandler.class);
    private final List<String> HEADERS_TO_FORWARD = List.of(
            HttpHeaders.ACCEPT_RANGES.toString(),
            HttpHeaders.CONTENT_RANGE.toString(),
            HttpHeaders.CONTENT_LENGTH.toString(),
            HttpHeaders.CONTENT_TYPE.toString());
    private final int port;
    private final WebClient client;
    private final ClassLoader currentClassLoader;
    private final QuinoaHandlerConfig config;

    QuinoaDevProxyHandler(final QuinoaHandlerConfig config, final Vertx vertx, int port) {
        this.port = port;
        this.client = WebClient.create(vertx);
        this.config = config;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(final RoutingContext ctx) {
        if (!shouldHandleMethod(ctx)) {
            next(currentClassLoader, ctx);
            return;
        }
        String path = resolvePath(ctx);
        if (isIgnored(path, config.ignoredPathPrefixes)) {
            next(currentClassLoader, ctx);
            return;
        }
        final HttpServerRequest request = ctx.request();
        final String resourcePath = path.endsWith("/") ? path + config.indexPage : path;
        if (isIgnored(resourcePath, config.ignoredPathPrefixes)) {
            next(currentClassLoader, ctx);
            return;
        }
        final String uri = computeURI(resourcePath, request);
        final MultiMap headers = request.headers();
        // Workaround for issue https://github.com/quarkiverse/quarkus-quinoa/issues/91
        // See https://www.npmjs.com/package/connect-history-api-fallback#htmlacceptheaders
        // When no Accept header is provided, the historyApiFallback is disabled
        headers.remove("Accept");
        // Disable compression in the forwarded request
        headers.remove("Accept-Encoding");
        client.request(request.method(), port, request.localAddress().host(), uri)
                .putHeaders(headers)
                .send(new Handler<AsyncResult<HttpResponse<Buffer>>>() {
                    @Override
                    public void handle(AsyncResult<HttpResponse<Buffer>> event) {
                        if (event.succeeded()) {
                            final int statusCode = event.result().statusCode();
                            if (statusCode == 200) {
                                forwardResponse(event, request, ctx, resourcePath);
                            } else if (statusCode == 404) {
                                next(currentClassLoader, ctx);
                            } else {
                                forwardError(event, statusCode, ctx);
                            }
                        } else {
                            error(event, ctx);
                        }
                    }
                });
    }

    private String computeURI(String path, HttpServerRequest request) {
        String uri = path;
        final String query = request.query();
        if (query != null) {
            uri += "?" + query;
        }
        return uri;
    }

    private void forwardError(AsyncResult<HttpResponse<Buffer>> event, int statusCode, RoutingContext ctx) {
        final Buffer body = event.result().body();
        final HttpServerResponse response = ctx.response().setStatusCode(statusCode);
        if (body != null) {
            response.send(body);
        } else {
            response.send();
        }
    }

    private void forwardResponse(AsyncResult<HttpResponse<Buffer>> event, HttpServerRequest request, RoutingContext ctx,
            String resourcePath) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Quinoa is forwarding: '%s'", request.uri());
        }
        final HttpServerResponse response = ctx.response();
        for (String header : HEADERS_TO_FORWARD) {
            response.headers().add(header, event.result().headers().getAll(header));
        }
        compressIfNeeded(config, ctx, resourcePath);
        final Buffer body = event.result().body();
        if (body != null) {
            response.send(body);
        } else {
            response.send();
        }

    }

    private void error(AsyncResult<HttpResponse<Buffer>> event, RoutingContext ctx) {
        ctx.response().setStatusCode(500);
        ctx.response().send("Quinoa failed to forward request, see logs.");
        LOG.error("Quinoa failed to forward request, see logs.", event.cause());
    }
}
