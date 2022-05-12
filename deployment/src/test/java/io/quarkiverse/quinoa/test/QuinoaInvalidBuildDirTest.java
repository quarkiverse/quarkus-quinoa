package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaInvalidBuildDirTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setBeforeAllCustomizer(QuinoaPrepareWebUI::prepare)
            .setAfterAllCustomizer(QuinoaPrepareWebUI::clean)
            .setLogRecordPredicate(log -> true)
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/webui")
            .overrideConfigKey("quarkus.quinoa.build-dir", "dist")
            .assertException(t -> {
                assertThat(t.getMessage()).isEqualTo("Quinoa build directory not found: 'dist'");
                assertThat((ConfigurationException) t).satisfies(e -> {
                    assertThat(e.getConfigKeys()).containsExactly("quarkus.quinoa.build-dir");
                });
            });

    @Test
    public void testQuinoaError() {
        // Will asset log records
    }
}
