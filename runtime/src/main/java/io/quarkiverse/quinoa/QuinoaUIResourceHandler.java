package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;
import static io.quarkiverse.quinoa.QuinoaRecorder.shouldHandleMethod;
import static io.vertx.ext.web.handler.StaticHandler.DEFAULT_INDEX_PAGE;

import java.util.List;
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
    private final List<String> ignoredPathPrefixes;
    private final ClassLoader currentClassLoader;

    QuinoaUIResourceHandler(final String directory, final Set<String> uiResources, final List<String> ignoredPathPrefixes) {
        this.uiResources = uiResources;
        this.staticHandler = directory != null ? StaticHandler.create(FileSystemAccess.ROOT, directory)
                : StaticHandler.create(QuinoaRecorder.META_INF_WEB_UI);
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
        if (!isIgnored(path, ignoredPathPrefixes) && isUIResource(path)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Quinoa is serving: '%s'", path);
            }
            staticHandler.handle(ctx);
        } else {
            next(currentClassLoader, ctx);
        }
    }

    private boolean isUIResource(String path) {
        return uiResources.contains(path) || (path.endsWith("/") && uiResources.contains(path + DEFAULT_INDEX_PAGE));
    }

}
