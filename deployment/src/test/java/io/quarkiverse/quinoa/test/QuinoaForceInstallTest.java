package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.systemBinary;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaForceInstallTest {
    private static final String NAME = "force-install";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .nodeModules()
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.force-install", "true")
            .assertLogRecords(l -> {
                assertThat(l)
                        .anyMatch(s -> s.getMessage().equals("Running Quinoa package manager install command: %s") &&
                                s.getParameters()[0].equals(systemBinary("npm") + " install"));
                assertThat(l)
                        .anyMatch(s -> s.getMessage().equals("Running Quinoa package manager build command: %s") &&
                                s.getParameters()[0].equals(systemBinary("npm") + " run build"));
            });

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }

}
