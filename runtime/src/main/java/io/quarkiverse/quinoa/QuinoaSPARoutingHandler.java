package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;
import static io.quarkiverse.quinoa.QuinoaRecorder.shouldHandleMethod;

import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class QuinoaSPARoutingHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaSPARoutingHandler.class);
    private final ClassLoader currentClassLoader;
    private final List<String> ignoredPathPrefixes;

    public QuinoaSPARoutingHandler(List<String> ignoredPathPrefixes) {
        this.ignoredPathPrefixes = ignoredPathPrefixes;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!shouldHandleMethod(ctx)) {
            next(currentClassLoader, ctx);
            return;
        }
        String path = resolvePath(ctx);
        if (!Objects.equals(path, "/") && !isIgnored(path, ignoredPathPrefixes)) {
            String mountPoint = ctx.mountPoint() != null ? ctx.mountPoint() : "/";
            String routePath = ctx.currentRoute().getPath() != null ? ctx.currentRoute().getPath() : "/";
            String target;
            if (mountPoint.endsWith("/")) {
                target = mountPoint.substring(0, mountPoint.length() - 1) + routePath;
            } else {
                target = mountPoint + routePath;
            }
            LOG.debugf("Quinoa is re-routing SPA request '%s' to '%s'", ctx.normalizedPath(), target);
            ctx.reroute(target);
        } else {
            next(currentClassLoader, ctx);
        }
    }
}
