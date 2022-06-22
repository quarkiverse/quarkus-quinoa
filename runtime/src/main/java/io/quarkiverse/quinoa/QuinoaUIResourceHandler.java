package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.compressIfNeeded;
import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;
import static io.quarkiverse.quinoa.QuinoaRecorder.shouldHandleMethod;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;

class QuinoaUIResourceHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(QuinoaUIResourceHandler.class);

    private final QuinoaHandlerConfig config;
    private final Set<String> uiResources;
    private final Handler<RoutingContext> handler;
    private final ClassLoader currentClassLoader;

    QuinoaUIResourceHandler(final QuinoaHandlerConfig config, final String directory, final Set<String> uiResources) {
        this.config = config;
        this.uiResources = uiResources;
        handler = createStaticHandler(config, directory);
        currentClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!shouldHandleMethod(ctx)) {
            next(currentClassLoader, ctx);
            return;
        }
        final String path = resolvePath(ctx);
        if (isIgnored(path, config.ignoredPathPrefixes)) {
            next(currentClassLoader, ctx);
            return;
        }
        final String resourcePath = path.endsWith("/") ? path + config.indexPage : path;
        if (!isIgnored(resourcePath, config.ignoredPathPrefixes) && uiResources.contains(resourcePath)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Quinoa is serving: '%s'", resourcePath);
            }
            handler.handle(ctx);
            compressIfNeeded(config, ctx, resourcePath);
        } else {
            next(currentClassLoader, ctx);
        }
    }

    private static Handler<RoutingContext> createStaticHandler(QuinoaHandlerConfig config, String directory) {
        final StaticHandler staticHandler = directory != null ? StaticHandler.create(FileSystemAccess.ROOT, directory)
                : StaticHandler.create(QuinoaRecorder.META_INF_WEB_UI);
        staticHandler.setDefaultContentEncoding(StandardCharsets.UTF_8.name());
        staticHandler.setIndexPage(config.indexPage);
        staticHandler.setCachingEnabled(config.prodMode);
        return staticHandler;
    }

}
