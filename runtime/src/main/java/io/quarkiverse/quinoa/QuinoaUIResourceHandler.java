package io.quarkiverse.quinoa;

import static io.quarkiverse.quinoa.QuinoaRecorder.compressIfNeeded;
import static io.quarkiverse.quinoa.QuinoaRecorder.isIgnored;
import static io.quarkiverse.quinoa.QuinoaRecorder.next;
import static io.quarkiverse.quinoa.QuinoaRecorder.resolvePath;
import static io.quarkiverse.quinoa.QuinoaRecorder.shouldHandleMethod;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
        this.uiResources = new HashSet<>(uiResources.size());
        for (String uiResource : uiResources) {
            final String encoded = encodeURI(uiResource);
            this.uiResources.add(encoded);
            LOG.debugf("Quinoa UI encoded: '%s'", encoded);
        }
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
        LOG.debugf("Quinoa is checking: '%s'", resourcePath);
        if (!isIgnored(resourcePath, config.ignoredPathPrefixes) && uiResources.contains(resourcePath)) {
            LOG.debugf("Quinoa is serving: '%s'", resourcePath);
            compressIfNeeded(config, ctx, resourcePath);
            handler.handle(ctx);
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

    /**
     * Duplicate code from OmniFaces project under apache license:
     * https://github.com/omnifaces/omnifaces/blob/develop/license.txt
     * <p>
     * URI-encode the given string using UTF-8. URIs (paths and filenames) have different encoding rules as compared to
     * URL query string parameters. {@link URLEncoder} is actually only for www (HTML) form based query string parameter
     * values (as used when a webbrowser submits a HTML form). URI encoding has a lot in common with URL encoding, but
     * the space has to be %20 and some chars doesn't necessarily need to be encoded.
     *
     * @param string The string to be URI-encoded using UTF-8.
     * @return The given string, URI-encoded using UTF-8, or <code>null</code> if <code>null</code> was given.
     */
    private static String encodeURI(String string) {
        if (string == null) {
            return null;
        }

        return URLEncoder.encode(string, StandardCharsets.UTF_8)
                .replaceAll("+", "%20")
                .replaceAll("%21", "!")
                .replaceAll("%27", "'")
                .replaceAll("%28", "(")
                .replaceAll("%29", ")")
                .replaceAll("%2F", "/")
                .replaceAll("%7E", "~");
    }
}
