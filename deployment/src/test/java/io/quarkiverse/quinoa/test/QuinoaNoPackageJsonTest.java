package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusExtensionTest;

public class QuinoaNoPackageJsonTest {
    private static final String NAME = "no-package-json";

    @RegisterExtension
    static final QuarkusExtensionTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusExtensionTest()
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/empty-webui")
            .assertException(e -> {
                assertThat(e).hasMessage("No package.json found in Web UI directory: 'src/test/empty-webui'");
            });

    @Test
    public void testQuinoaError() {
        // Will asset log records
    }
}
