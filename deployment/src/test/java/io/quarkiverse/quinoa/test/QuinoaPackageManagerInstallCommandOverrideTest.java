package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static io.quarkiverse.quinoa.test.QuinoaPackageManagerInstallTest.computeBinary;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerInstallCommandOverrideTest {
    private static final String NAME = "package-manager-install-command-override";
    public static final String INSTALL_DIR = "target/node-" + NAME;

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.package-manager-install", "true")
            .overrideConfigKey("quarkus.quinoa.package-manager-install.node-version", "18.17.0")
            .overrideConfigKey("quarkus.quinoa.package-manager-install.install-dir", INSTALL_DIR)
            .overrideConfigKey("quarkus.quinoa.package-manager-command.build", "run build-something")
            .overrideConfigKey("quarkus.quinoa.package-manager-command.build-env.BUILD", "yeahhh")
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage().equals("Running Quinoa package manager build command: %s") &&
                            ((String) s.getParameters()[0])
                                    .endsWith(computeBinary(INSTALL_DIR)
                                            + " run build-something")));

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("yeahhh");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
        assertThat(Path.of(INSTALL_DIR)).isDirectory();
        assertThat(Path.of(INSTALL_DIR + "/node")).isDirectory();
    }

}
