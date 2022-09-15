package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.systemBinary;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerSetYarnConfigTest {
    private static final String NAME = "package-manager-set-yarn";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.package-manager", systemBinary("yarn"))
            .overrideConfigKey("quarkus.quinoa.frozen-lockfile", "false")
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage().equals("Running Quinoa package manager build command: %s") &&
                            s.getParameters()[0].equals(systemBinary("yarn") + " run build")));

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }

}
