package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.compressIfNeeded;
import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.shouldHandleMethod;

import java.util.List;

import io.vertx.ext.web.client.WebClientOptions;
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

    private final String host;
    private final int port;
    private final WebClient client;
    private final QuinoaDevWebSocketProxyHandler wsUpgradeHandler;
    private final ClassLoader currentClassLoader;
    private final QuinoaDevProxyHandlerConfig config;

    QuinoaDevProxyHandler(final QuinoaDevProxyHandlerConfig config, final Vertx vertx,  boolean tls, boolean tlsAllowInsecure,String host, int port,
            boolean websocket) {
        this.host = host;
        this.port = port;
        WebClientOptions options = new WebClientOptions();
        if(tls){
            options.setSsl(true);
            if(tlsAllowInsecure){
                options.setTrustAll(true);
                options.setVerifyHost(false);
            }
        }
        this.client = WebClient.create(vertx, options);
        this.wsUpgradeHandler = websocket ? new QuinoaDevWebSocketProxyHandler(vertx, host, port) : null;
        this.config = config;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(final RoutingContext ctx) {
        if (!shouldHandleMethod(ctx)) {
            next(currentClassLoader, ctx);
            return;
        }
        String path = ctx.normalizedPath();
        if (isIgnored(path, config.ignoredPathPrefixes)) {
            next(currentClassLoader, ctx);
            return;
        }

        final String resourcePath = path.endsWith("/") ? path + config.indexPage : path;
        if (isIgnored(resourcePath, config.ignoredPathPrefixes)) {
            next(currentClassLoader, ctx);
            return;
        }

        if (isUpgradeToWebSocket(ctx)) {
            if (this.wsUpgradeHandler != null) {
                wsUpgradeHandler.handle(ctx);
            } else {
                next(currentClassLoader, ctx);
            }
        } else {
            handleHttpRequest(ctx, resourcePath);
        }
    }

    private static boolean isUpgradeToWebSocket(RoutingContext ctx) {
        return ctx.request().headers().contains("Upgrade")
                && "websocket".equalsIgnoreCase(ctx.request().headers().get("Upgrade"));
    }

    private void handleHttpRequest(final RoutingContext ctx, final String resourcePath) {
        final HttpServerRequest request = ctx.request();
        final MultiMap headers = request.headers();
        final String uri = computeResourceURI(resourcePath, request);

        // Disable compression in the forwarded request
        headers.remove("Accept-Encoding");
        client.request(request.method(), port, host, uri)
                .putHeaders(headers)
                .send(event -> {
                    if (event.succeeded()) {
                        final int statusCode = event.result().statusCode();
                        switch (statusCode) {
                            case 200:
                                if (config.devServerDirectForwarding || shouldForward(ctx, event.result())) {
                                    forwardResponse(event, request, ctx, resourcePath);
                                } else {
                                    next(currentClassLoader, ctx);
                                }

                                break;
                            case 404:
                                next(currentClassLoader, ctx);
                                break;
                            default:
                                forwardError(event, statusCode, ctx);
                        }
                    } else {
                        error(event, ctx);
                    }
                });
    }

    private boolean shouldForward(RoutingContext ctx, HttpResponse<Buffer> result) {
        final List<String> contentType = result.headers().getAll(HttpHeaders.CONTENT_TYPE);
        if (contentType != null && contentType.stream().anyMatch(s -> s.contains("text/html"))) {
            final String path = QuinoaRecorder.resolvePath(ctx);
            // We forward if the server returns a html, and it was intended:
            // - if the path ends with .html
            // - if the path is empty (root)
            return path.endsWith(".html") || path.equals("/") || path.isEmpty();
        }
        return true;
    }

    private String computeResourceURI(String path, HttpServerRequest request) {
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
        LOG.debugf("Quinoa is forwarding: '%s'", request.uri());
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
        final String error = String.format("Quinoa failed to forward request '%s', see logs.", ctx.request().uri());
        ctx.response().setStatusCode(500);
        ctx.response().send(error);
        LOG.error(error, event.cause());
    }
}
