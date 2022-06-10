package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;
import static io.quarkiverse.quinoa.QuinoaRecorder.shouldHandleMethod;

import java.util.Objects;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class QuinoaSPARoutingHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaSPARoutingHandler.class);
    private final ClassLoader currentClassLoader;
    private final QuinoaHandlerConfig config;

    public QuinoaSPARoutingHandler(final QuinoaHandlerConfig config) {
        this.config = config;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!shouldHandleMethod(ctx)) {
            next(currentClassLoader, ctx);
            return;
        }
        String path = resolvePath(ctx);
        if (!Objects.equals(path, "/") && !isIgnored(path, config.ignoredPathPrefixes)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Quinoa is re-routing SPA request '%s' to '/'", ctx.normalizedPath());
            }
            ctx.reroute("/");
        } else {
            next(currentClassLoader, ctx);
        }
    }
}
