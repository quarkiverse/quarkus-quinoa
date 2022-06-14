package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaNoDirTest {

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create().toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/no-webui")
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo(
                            "Quinoa directory not found 'quarkus.quinoa.ui-dir=%s'. It is recommended to remove the quarkus-quinoa extension if not used.");
                    assertThat(s.getParameters()[0]).isEqualTo("src/test/no-webui");
                });
            });

    @Test
    public void testQuinoaError() {
        // Will assert log records
    }
}
