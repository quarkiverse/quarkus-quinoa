package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class QuinoaInvalidBuildDirTest {
    private static final String NAME = "invalid-build-dir";

    @RegisterExtension
    static final QuarkusExtensionTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusExtensionTest()
            .overrideConfigKey("quarkus.quinoa.build-dir", "dist")
            .assertException(t -> {
                assertThat(t.getMessage()).startsWith("Quinoa build directory not found: '");
                assertThat(t.getMessage()).endsWith("dist'");
                assertThat((ConfigurationException) t).satisfies(e -> {
                    assertThat(e.getConfigKeys()).containsExactly("quarkus.quinoa.build-dir");
                });
            });

    @Test
    public void testQuinoaError() {
        // Will asset log records
    }
}
