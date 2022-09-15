package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaNoDirTest {
    private static final String NAME = "no-webui-dir";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/no-webui")
            .assertLogRecords(l -> assertThat(l).anyMatch(s -> s.getMessage().equals(
                    "Quinoa directory not found 'quarkus.quinoa.ui-dir=%s'. It is recommended to remove the quarkus-quinoa extension if not used.")
                    &&
                    s.getParameters()[0].equals("src/test/no-webui")));

    @Test
    public void testQuinoaError() {
        // Will assert log records
    }
}
