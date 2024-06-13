package io.quarkiverse.quinoa;

import static io.quarkus.vertx.http.runtime.RouteConstants.ROUTE_ORDER_DEFAULT;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuinoaRecorder {
    private static final Logger LOG = Logger.getLogger(QuinoaRecorder.class);
    public static final String META_INF_WEB_UI = "META-INF/webui";
    public static final int QUINOA_ROUTE_ORDER = 1100;
    public static final int QUINOA_SPA_ROUTE_ORDER = ROUTE_ORDER_DEFAULT + 30_000;
    public static final Set<HttpMethod> HANDLED_METHODS = Set.of(HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.GET);

    public Handler<RoutingContext> quinoaProxyDevHandler(final QuinoaDevProxyHandlerConfig handlerConfig, Supplier<Vertx> vertx,
            String host, int port, boolean websocket) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Quinoa dev proxy-handler is ignoring paths starting with: "
                    + String.join(", ", handlerConfig.ignoredPathPrefixes));
        }
        return new QuinoaDevProxyHandler(handlerConfig, vertx.get(), host, port, websocket);
    }

    public Handler<RoutingContext> quinoaSPARoutingHandler(List<String> ignoredPathPrefixes) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Quinoa SPA routing handler is ignoring paths starting with: " + String.join(", ", ignoredPathPrefixes));
        }
        return new QuinoaSPARoutingHandler(ignoredPathPrefixes);
    }

    static String resolvePath(RoutingContext ctx) {
        return (ctx.mountPoint() == null) ? ctx.normalizedPath()
                : ctx.normalizedPath().substring(
                        // let's be extra careful here in case Vert.x normalizes the mount points at
                        // some point
                        ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
    }

    static boolean isIgnored(final String path, final List<String> ignoredPathPrefixes) {
        if (ignoredPathPrefixes.stream().anyMatch(path::startsWith)) {
            LOG.debugf("Quinoa is ignoring path (quarkus.quinoa.ignored-path-prefixes): " + path);
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
        String contentType = MimeMapping.getMimeTypeForFilename(path);
        return contentType != null && config.compressMediaTypes.contains(contentType);
    }

    static boolean shouldHandleMethod(RoutingContext ctx) {
        return HANDLED_METHODS.contains(ctx.request().method());
    }

    static void next(ClassLoader cl, RoutingContext ctx) {
        // make sure we don't lose the correct TCCL to Vert.x...
        Thread.currentThread().setContextClassLoader(cl);
        ctx.next();
    }

}
