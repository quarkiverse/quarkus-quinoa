package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaSSRSPAMutualExclusivityTest {

    private static final String NAME = "ssr-spa-mutual-exclusivity";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.enable-spa-routing", "true")
            .overrideConfigKey("quarkus.quinoa.enable-ssr-mode", "true")
            .assertException(e -> {
                assertThat(e.getMessage())
                        .contains("cannot have both", "enable-spa-routing", "enable-ssr-mode");
            });

    @Test
    public void testMutualExclusivity() {
        // Exception should be thrown during build
    }
}
