package io.quarkiverse.quinoa;

import static io.quarkus.vertx.http.runtime.RouteConstants.ROUTE_ORDER_DEFAULT;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.MimeMapping;
import io.vertx.ext.web.RoutingContext;

/**
 * Runtime factories and helpers for the Quinoa route handlers.
 * <p>
 * The factory methods are invoked from {@code ServiceRegistrar} service lambdas registered by the deployment
 * processors, which is why they are static and only take simple immutable values or services as parameters.
 */
public final class QuinoaHandlers {
    private static final Logger LOG = Logger.getLogger(QuinoaHandlers.class);
    public static final String META_INF_WEB_UI = "META-INF/webui";
    public static final int QUINOA_ROUTE_ORDER = 1100;
    public static final int QUINOA_SPA_ROUTE_ORDER = ROUTE_ORDER_DEFAULT + 30_000;
    public static final Set<HttpMethod> HANDLED_METHODS = Set.of(HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.GET);

    private QuinoaHandlers() {
    }

    public static QuinoaRouteHandler quinoaProxyDevHandler(final QuinoaDevProxyHandlerConfig handlerConfig, Vertx vertx,
            QuinoaNetworkConfiguration network) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Quinoa dev proxy-handler is ignoring paths starting with: "
                    + String.join(", ", handlerConfig.ignoredPathPrefixes));
        }
        return new QuinoaDevProxyHandler(handlerConfig, vertx, network);
    }

    public static QuinoaRouteHandler quinoaSPARoutingHandler(List<String> ignoredPathPrefixes) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Quinoa SPA routing handler is ignoring paths starting with: " + String.join(", ", ignoredPathPrefixes));
        }
        return new QuinoaSPARoutingHandler(ignoredPathPrefixes);
    }

    public static void logUiRootPath(final String resolvedUiRootPath) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Quinoa is available at: " + resolvedUiRootPath);
        }
    }

    static String resolvePath(RoutingContext ctx) {
        // quarkus.http.root-path
        String path = (ctx.mountPoint() == null) ? ctx.normalizedPath()
                : ctx.normalizedPath().substring(
                        // let's be extra careful here in case Vert.x normalizes the mount points at
                        // some point
                        ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
        // quarkus.quinoa.ui-root-path
        String routePath = ctx.currentRoute().getPath();
        String resolvedPath = (routePath == null) ? path
                : path.substring(routePath.endsWith("/") ? routePath.length() - 1 : routePath.length());
        // use "/" when the path is empty
        // e.g. this happens when the request path is "/example" and the root path is "/example"
        return resolvedPath.isEmpty() ? "/" : resolvedPath;
    }

    static boolean matchesPathSeparatedPrefix(String path, String pathSeparatedPrefix) {
        if (path.startsWith(pathSeparatedPrefix)) {
            String restPath = path.substring(pathSeparatedPrefix.length());
            // the path matches the path separated prefix if the rest path is empty or starts with "/"
            // note that the pathSeparatedPrefix never ends in "/" except if it equals "/" exactly
            return restPath.isEmpty() || restPath.startsWith("/") || Objects.equals(pathSeparatedPrefix, "/");
        }
        return false;
    }

    static boolean isIgnored(final String path, final List<String> ignoredPathPrefixes) {
        if (ignoredPathPrefixes.stream().anyMatch(prefix -> matchesPathSeparatedPrefix(path, prefix))) {
            LOG.debug("Quinoa is ignoring path (quarkus.quinoa.ignored-path-prefixes): " + path);
            return true;
        }
        return false;
    }

    static void compressIfNeeded(QuinoaDevProxyHandlerConfig config, RoutingContext ctx, String path) {
        if (config.enableCompression && isCompressed(config, path)) {
            // VertxHttpRecorder is adding "Content-Encoding: identity" to all requests if
            // compression is enabled.
            // Handlers can remove the "Content-Encoding: identity" header to enable
            // compression.
            ctx.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
        }
    }

    private static boolean isCompressed(QuinoaDevProxyHandlerConfig config, String path) {
        if (config.compressMediaTypes.isEmpty()) {
            return false;
        }
        String contentType = MimeMapping.mimeTypeForFilename(path);
        return contentType != null && config.compressMediaTypes.contains(contentType);
    }

    static boolean shouldHandleMethod(RoutingContext ctx) {
        return HANDLED_METHODS.contains(ctx.request().method());
    }

    static void next(ClassLoader cl, RoutingContext ctx) {
        // make sure we don't lose the correct thread context to Vert.x...
        Thread.currentThread().setContextClassLoader(cl);
        ctx.next();
    }

}
