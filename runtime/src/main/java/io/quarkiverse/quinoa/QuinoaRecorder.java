package io.quarkiverse.quinoa;

import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuinoaRecorder {
    private static final Logger LOG = Logger.getLogger(QuinoaRecorder.class);
    public static final String META_INF_WEB_UI = "META-INF/webui";

    public Handler<RoutingContext> quinoaProxyDevHandler(Supplier<Vertx> vertx, final Integer port) {
        return new QuinoaDevProxyHandler(vertx.get(), port);
    }

    public Handler<RoutingContext> quinoaHandler(final String directory, final Set<String> uiResources,
            boolean enableSPARouting) throws IOException {
        return new QuinoaUIResourceHandler(directory, uiResources, enableSPARouting);
    }

}
