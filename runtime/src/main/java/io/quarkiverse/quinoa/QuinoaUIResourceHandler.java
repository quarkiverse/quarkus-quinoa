package io.quarkiverse.quinoa;

import static io.vertx.ext.web.handler.StaticHandler.DEFAULT_INDEX_PAGE;

import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;

class QuinoaUIResourceHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaUIResourceHandler.class);

    private final Set<String> uiResources;
    private final Handler<RoutingContext> staticHandler;
    private final boolean enableSPARouting;
    private final ClassLoader currentClassLoader;

    QuinoaUIResourceHandler(String directory, Set<String> uiResources, boolean enableSPARouting) {
        this.uiResources = uiResources;
        this.staticHandler = directory != null ? StaticHandler.create(FileSystemAccess.ROOT, directory)
                : StaticHandler.create(QuinoaRecorder.META_INF_WEB_UI);
        this.enableSPARouting = enableSPARouting;
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(RoutingContext ctx) {
        String rel = ctx.mountPoint() == null ? ctx.normalizedPath()
                : ctx.normalizedPath().substring(
                        // let's be extra careful here in case Vert.x normalizes the mount points at some point
                        ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
        if (uiResources.contains(rel)
                || (uiResources.contains("/" + DEFAULT_INDEX_PAGE) && Objects.equals(rel, "/"))) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Quinoa is serving: '%s'", rel);
            }
            staticHandler.handle(ctx);
        } else if (enableSPARouting) {
            ctx.reroute("/");
        } else {
            // make sure we don't lose the correct TCCL to Vert.x...
            Thread.currentThread().setContextClassLoader(currentClassLoader);
            ctx.next();
        }
    }
}
