package io.quarkiverse.quinoa;

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

    private final Integer port;
    private final WebClient client;
    private final ClassLoader currentClassLoader;

    QuinoaDevProxyHandler(Vertx vertx, Integer port) {
        this.port = port;
        this.client = WebClient.create(vertx);
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(final RoutingContext ctx) {
        final HttpServerRequest request = ctx.request();
        client.request(request.method(), port, request.localAddress().host(), request.uri())
                .send(new Handler<AsyncResult<HttpResponse<Buffer>>>() {
                    @Override
                    public void handle(AsyncResult<HttpResponse<Buffer>> event) {
                        if (event.succeeded()) {
                            final int statusCode = event.result().statusCode();
                            if (statusCode == 200) {
                                ctx.response().headers().addAll(event.result().headers());
                                ctx.response().send(event.result().body());
                            } else if (statusCode == 404) {
                                // make sure we don't lose the correct TCCL to Vert.x...
                                Thread.currentThread().setContextClassLoader(currentClassLoader);
                                ctx.next();
                            } else {
                                ctx.response().setStatusCode(statusCode).send(event.result().body());
                            }
                        } else {
                            ctx.response().setStatusCode(500);
                            ctx.response().send();
                        }

                    }
                });
    }
}
