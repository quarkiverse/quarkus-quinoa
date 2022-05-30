package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;

import java.util.List;

import org.jboss.logging.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

class QuinoaDevProxyHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaDevProxyHandler.class);

    private final int port;
    private final WebClient client;
    private final List<String> ignoredPathPrefixes;
    private final ClassLoader currentClassLoader;

    QuinoaDevProxyHandler(final Vertx vertx, int port, final List<String> ignoredPathPrefixes) {
        this.port = port;
        this.client = WebClient.create(vertx);
        this.ignoredPathPrefixes = ignoredPathPrefixes;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(final RoutingContext ctx) {
        String path = resolvePath(ctx);
        if (isIgnored(path, ignoredPathPrefixes)) {
            next(ctx);
            return;
        }
        final HttpServerRequest request = ctx.request();
        client.request(request.method(), port, request.localAddress().host(), request.uri())
                .send(new Handler<AsyncResult<HttpResponse<Buffer>>>() {
                    @Override
                    public void handle(AsyncResult<HttpResponse<Buffer>> event) {
                        if (event.succeeded()) {
                            final int statusCode = event.result().statusCode();
                            if (statusCode == 200) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debugf("Quinoa is forwarding: '%s'", request.uri());
                                }
                                ctx.response().headers().addAll(event.result().headers());
                                ctx.response().send(event.result().body());
                            } else if (statusCode == 404) {
                                next(ctx);
                            } else {
                                ctx.response().setStatusCode(statusCode).send(event.result().body());
                            }
                        } else {
                            ctx.response().setStatusCode(500);
                            ctx.response().send("Quinoa failed to forward request, see logs.");
                            LOG.error("Quinoa failed to forward request, see logs.", event.cause());
                        }

                    }
                });
    }

    private void next(RoutingContext ctx) {
        // make sure we don't lose the correct TCCL to Vert.x...
        Thread.currentThread().setContextClassLoader(currentClassLoader);
        ctx.next();
    }
}
