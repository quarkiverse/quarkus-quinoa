package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class QuinoaNextSSRBuildDirHintTest {
    private static final String NAME = "next-ssr-build-dir-hint";

    // A server-rendered Next.js build only produces '.next', the static export build dir 'out' is missing
    @RegisterExtension
    static final QuarkusExtensionTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusExtensionTest()
            .overrideConfigKey("quarkus.quinoa.package-manager-command.build", "run build-next-ssr")
            .overrideConfigKey("quarkus.quinoa.build-dir", "out")
            .assertException(t -> {
                assertThat(t.getMessage()).startsWith("Quinoa build directory not found: '");
                assertThat(t.getMessage()).contains("out'. A '.next' directory exists in the Web UI directory",
                        "add output: 'export' to the Next config file",
                        "set quarkus.quinoa.enable-ssr-mode=true");
                assertThat((ConfigurationException) t).satisfies(e -> {
                    assertThat(e.getConfigKeys()).containsExactlyInAnyOrder("quarkus.quinoa.build-dir",
                            "quarkus.quinoa.enable-ssr-mode");
                });
            });

    @Test
    public void testQuinoaError() {
        // Will assert the exception
    }
}
