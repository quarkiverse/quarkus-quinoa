package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.isWindows;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall;
import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerInstallTest {
    private static final String NAME = "package-manager-install";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.package-manager-install", "true")
            .overrideConfigKey("quarkus.quinoa.package-manager-install.node-version", "16.17.0")
            .overrideConfigKey("quarkus.quinoa.package-manager-install.install-dir", "target/node-" + NAME)
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage().equals("Running Quinoa package manager build command: %s") &&
                            ((String) s.getParameters()[0])
                                    .endsWith(convertToWindowsPathIfNeeded(
                                            NAME + "/node/node_modules/npm/bin/npm-cli.js run build"))));

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
        assertThat(Path.of("target/node-" + NAME)).isDirectory();
        assertThat(Path.of("target/node-" + NAME + "/node")).isDirectory();
    }

    private static String convertToWindowsPathIfNeeded(String path) {
        return isWindows() ? PackageManagerInstall.convertToWindowsPathIfNeeded(path) : path;
    }

}
