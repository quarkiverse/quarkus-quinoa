package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;

import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class QuinoaSPARoutingHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaSPARoutingHandler.class);
    private final List<String> ignoredPathPrefixes;
    private final ClassLoader currentClassLoader;

    public QuinoaSPARoutingHandler(List<String> ignoredPathPrefixes) {
        this.ignoredPathPrefixes = ignoredPathPrefixes;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(RoutingContext ctx) {
        String path = resolvePath(ctx);
        if (!Objects.equals(path, "/") && !isIgnored(path, ignoredPathPrefixes)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Quinoa is re-routing SPA request '%s' to '/'", ctx.normalizedPath());
            }
            ctx.reroute("/");
        } else {
            next(ctx);
        }
    }

    private void next(RoutingContext ctx) {
        // make sure we don't lose the correct TCCL to Vert.x...
        Thread.currentThread().setContextClassLoader(currentClassLoader);
        ctx.next();
    }
}
