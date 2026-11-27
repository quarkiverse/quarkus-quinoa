package io.quarkiverse.quinoa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

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
}
