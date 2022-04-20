package io.quarkiverse.quinoa.testing;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class QuinoaTestProfiles {

    public static class Enable implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.quinoa", "true");
        }
    }

    public static class EnableAndRunTests implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.quinoa", "true",
                    "quarkus.quinoa.run-tests", "true");
        }
    }
}
