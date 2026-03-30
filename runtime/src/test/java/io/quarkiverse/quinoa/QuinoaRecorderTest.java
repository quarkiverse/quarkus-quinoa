package io.quarkiverse.quinoa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class QuinoaRecorderTest {

    @Test
    void handledMethodsContainsOnlyGetHeadOptions() {
        var methods = QuinoaRecorder.HANDLED_METHODS.stream()
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
        assertTrue(QuinoaRecorder.isIgnored("/api/something", prefixes));
        assertTrue(QuinoaRecorder.isIgnored("/api", prefixes));
        assertTrue(QuinoaRecorder.isIgnored("/q/dev", prefixes));
        assertFalse(QuinoaRecorder.isIgnored("/other", prefixes));
        assertFalse(QuinoaRecorder.isIgnored("/", prefixes));
    }

    @Test
    void isIgnoredDoesNotMatchPartialSegment() {
        List<String> prefixes = List.of("/api");
        // /api-docs should NOT match /api prefix (different path segment)
        assertFalse(QuinoaRecorder.isIgnored("/api-docs", prefixes));
        // /api/something should match
        assertTrue(QuinoaRecorder.isIgnored("/api/something", prefixes));
    }

    @Test
    void matchesPathSeparatedPrefix() {
        assertTrue(QuinoaRecorder.matchesPathSeparatedPrefix("/api/something", "/api"));
        assertTrue(QuinoaRecorder.matchesPathSeparatedPrefix("/api", "/api"));
        assertFalse(QuinoaRecorder.matchesPathSeparatedPrefix("/api-docs", "/api"));
        assertFalse(QuinoaRecorder.matchesPathSeparatedPrefix("/other", "/api"));
        assertTrue(QuinoaRecorder.matchesPathSeparatedPrefix("/anything", "/"));
    }
}
