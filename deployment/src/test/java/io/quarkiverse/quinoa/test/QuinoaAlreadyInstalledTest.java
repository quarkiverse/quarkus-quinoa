package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.systemBinary;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaAlreadyInstalledTest {
    private static final String NAME = "already-installed";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .nodeModules()
            .toQuarkusUnitTest()
            .assertLogRecords(l -> {
                assertThat(l)
                        .noneMatch(s -> s.getMessage().equals("Running Quinoa package manager install command: %s") &&
                                s.getParameters()[0].equals(systemBinary("npm") + " install"));
            }).assertException(e -> {
                assertThat(e).hasMessage(
                        "Error in Quinoa while running package manager build command: " + systemBinary("npm") + " run build");
            });

    @Test
    public void testQuinoa() {
        // Will assert exception
    }

}
