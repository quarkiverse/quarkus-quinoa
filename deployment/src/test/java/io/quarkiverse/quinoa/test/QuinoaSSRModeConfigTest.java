package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusExtensionTest;

public class QuinoaSSRModeConfigTest {

    private static final String NAME = "ssr-mode-config";

    @RegisterExtension
    static final QuarkusExtensionTest config = QuinoaQuarkusUnitTest.create(NAME)
            .toQuarkusExtensionTest()
            .overrideConfigKey("quarkus.quinoa.enable-ssr-mode", "true");

    @Test
    public void testSSRModeConfigLoads() {
        // Test that SSR mode config doesn't break the build
        assertThat(Path.of("target/quinoa/build/index.html")).isRegularFile()
                .hasContent("test");
    }
}
