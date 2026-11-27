package io.quarkiverse.quinoa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

class QuinoaHandlersTest {

    @Test
    void handledMethodsContainsOnlyGetHeadOptions() {
        var methods = QuinoaHandlers.HANDLED_METHODS.stream()
                .map(Object::toString)
                .toList();
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("HEAD"));
        assertTrue(methods.contains("OPTIONS"));
        assertFalse(methods.contains("POST"));
        assertFalse(methods.contains("PUT"));
        assertFalse(methods.contains("DELETE"));
        assertFalse(methods.contains("PATCH"));
    }

    @Test
    void isIgnoredMatchesPrefix() {
        List<String> prefixes = List.of("/api", "/q");
        assertTrue(QuinoaHandlers.isIgnored("/api/something", prefixes));
        assertTrue(QuinoaHandlers.isIgnored("/api", prefixes));
        assertTrue(QuinoaHandlers.isIgnored("/q/dev", prefixes));
        assertFalse(QuinoaHandlers.isIgnored("/other", prefixes));
        assertFalse(QuinoaHandlers.isIgnored("/", prefixes));
    }

    @Test
    void isIgnoredDoesNotMatchPartialSegment() {
        List<String> prefixes = List.of("/api");
        // /api-docs should NOT match /api prefix (different path segment)
        assertFalse(QuinoaHandlers.isIgnored("/api-docs", prefixes));
        // /api/something should match
        assertTrue(QuinoaHandlers.isIgnored("/api/something", prefixes));
    }

    @Test
    void matchesPathSeparatedPrefix() {
        assertTrue(QuinoaHandlers.matchesPathSeparatedPrefix("/api/something", "/api"));
        assertTrue(QuinoaHandlers.matchesPathSeparatedPrefix("/api", "/api"));
        assertFalse(QuinoaHandlers.matchesPathSeparatedPrefix("/api-docs", "/api"));
        assertFalse(QuinoaHandlers.matchesPathSeparatedPrefix("/other", "/api"));
        assertTrue(QuinoaHandlers.matchesPathSeparatedPrefix("/anything", "/"));
    }

    @Test
    void decodedPathDecodesPercentEncoding() {
        assertEquals("/café/my api", QuinoaHandlers.decodedPath(ctx("/caf%C3%A9/my%20api", null, null)));
        // '+' is not a space in a path
        assertEquals("/a+b", QuinoaHandlers.decodedPath(ctx("/a+b", null, null)));
        assertEquals("/plain/path", QuinoaHandlers.decodedPath(ctx("/plain/path", null, null)));
    }

    @Test
    void decodedPathIsNullWhenPathCannotBeDecoded() {
        assertNull(QuinoaHandlers.decodedPath(ctx("/bad%zz", null, null)));
        assertNull(QuinoaHandlers.decodedPath(ctx("/literal?question", null, null)));
    }

    @Test
    void resolvePathIsDecodedAndRelativeToMountPointAndRoute() {
        assertEquals("/my page", QuinoaHandlers.resolvePath(ctx("/my%20page", null, null)));
        assertEquals("/my page", QuinoaHandlers.resolvePath(ctx("/root/my%20page", "/root/", null)));
        assertEquals("/my page", QuinoaHandlers.resolvePath(ctx("/root/ui/my%20page", "/root/", "/ui/")));
        assertEquals("/", QuinoaHandlers.resolvePath(ctx("/ui", null, "/ui")));
        assertNull(QuinoaHandlers.resolvePath(ctx("/bad%zz", null, null)));
    }

    @Test
    void isIgnoredMatchesDecodedPrefixes() {
        // configured prefixes are decoded (Quarkus config paths can't contain '%')
        List<String> prefixes = List.of("/café", "/日本語");
        assertTrue(QuinoaHandlers.isIgnored(QuinoaHandlers.decodedPath(ctx("/caf%C3%A9", null, null)), prefixes));
        assertTrue(QuinoaHandlers.isIgnored(QuinoaHandlers.decodedPath(ctx("/caf%C3%A9/foo", null, null)), prefixes));
        assertTrue(QuinoaHandlers.isIgnored(
                QuinoaHandlers.decodedPath(ctx("/%E6%97%A5%E6%9C%AC%E8%AA%9E/foo", null, null)), prefixes));
        assertFalse(QuinoaHandlers.isIgnored(QuinoaHandlers.decodedPath(ctx("/caf%C3%A9s", null, null)), prefixes));
    }

    /**
     * Minimal {@link RoutingContext} exposing only what the path helpers use.
     */
    private static RoutingContext ctx(String normalizedPath, String mountPoint, String routePath) {
        final Route route = (Route) Proxy.newProxyInstance(QuinoaHandlersTest.class.getClassLoader(),
                new Class<?>[] { Route.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getPath" -> routePath;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (RoutingContext) Proxy.newProxyInstance(QuinoaHandlersTest.class.getClassLoader(),
                new Class<?>[] { RoutingContext.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "normalizedPath" -> normalizedPath;
                    case "mountPoint" -> mountPoint;
                    case "currentRoute" -> route;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
