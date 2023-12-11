package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.NODE_BINARY;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.NPM_PATH;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.normalizePath;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.quotePathWithSpaces;
import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerInstallTest {
    private static final String NAME = "package-manager-install";
    public static final String INSTALL_DIR = "target/node-" + NAME;

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.package-manager-install", "true")
            .overrideConfigKey("quarkus.quinoa.package-manager-install.node-version", "20.10.0")
            .overrideConfigKey("quarkus.quinoa.package-manager-install.install-dir", INSTALL_DIR)
            .assertLogRecords(l -> {
                assertThat(l)
                        .anyMatch(s -> s.getMessage().equals("Running Quinoa package manager build command: %s") &&
                                ((String) s.getParameters()[0])
                                        .endsWith(computeBinary(INSTALL_DIR) + " run build"));
                assertThat(l)
                        .anyMatch(s -> s.getMessage().equals("v20.10.0"));
            });

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa/build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
        assertThat(Path.of(INSTALL_DIR)).isDirectory();
        assertThat(Path.of(INSTALL_DIR + "/node")).isDirectory();
    }

    static String computeBinary(String installDir) {
        return NODE_BINARY + " "
                + quotePathWithSpaces(normalizePath(
                        Path.of(installDir, NPM_PATH).toAbsolutePath().toString()));
    }

}
