package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;
import static io.vertx.ext.web.handler.StaticHandler.DEFAULT_INDEX_PAGE;

import java.util.List;

import org.jboss.logging.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

class QuinoaDevProxyHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaDevProxyHandler.class);

    private final Vertx vertx;
    private final int port;
    private final WebClient client;
    private final List<String> ignoredPathPrefixes;
    private final ClassLoader currentClassLoader;

    QuinoaDevProxyHandler(final Vertx vertx, int port, final List<String> ignoredPathPrefixes) {
        this.vertx = vertx;
        this.port = port;
        this.client = WebClient.create(vertx);
        this.ignoredPathPrefixes = ignoredPathPrefixes;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(final RoutingContext ctx) {
        String path = resolvePath(ctx);
        if (isIgnored(path, ignoredPathPrefixes)) {
            next(currentClassLoader, ctx);
            return;
        }
        final HttpServerRequest request = ctx.request();
        // FIXME: path might need to be encoded
        String uri = path;
        if (uri.endsWith("/")) {
            // We directly check the index because some NodeJS servers have a directory listing on root paths when there is no index.
            // This way if the index is found, we return it, else we can continue with other Quarkus routes (e.g: META-INF/resources/index.html).
            uri += DEFAULT_INDEX_PAGE;
        }
        final String query = request.query();
        if (query != null) {
            uri += "?" + query;
        }
        client.request(request.method(), port, request.localAddress().host(), uri)
                .send(new Handler<AsyncResult<HttpResponse<Buffer>>>() {
                    @Override
                    public void handle(AsyncResult<HttpResponse<Buffer>> event) {
                        if (event.succeeded()) {
                            final int statusCode = event.result().statusCode();
                            if (statusCode == 200) {
                                forwardResponse(event, request, ctx);
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

    private void forwardError(AsyncResult<HttpResponse<Buffer>> event, int statusCode, RoutingContext ctx) {
        final Buffer body = event.result().body();
        final HttpServerResponse response = ctx.response().setStatusCode(statusCode);
        if (body != null) {
            response.send(body);
        } else {
            response.send();
        }
    }

    private void forwardResponse(AsyncResult<HttpResponse<Buffer>> event, HttpServerRequest request, RoutingContext ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Quinoa is forwarding: '%s'", request.uri());
        }
        final HttpServerResponse response = ctx.response();
        response.headers().addAll(event.result().headers());
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
