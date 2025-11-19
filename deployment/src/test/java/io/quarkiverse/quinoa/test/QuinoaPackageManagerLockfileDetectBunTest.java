package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.systemBinary;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType;
import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerLockfileDetectBunTest {
    private static final String NAME = "package-manager-lockfile-detect-bun";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .initialLockfile(PackageManagerType.BUN.getLockFile())
            .toQuarkusUnitTest()
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> "Running Quinoa package manager build command: %s".equals(s.getMessage()) &&
                            (systemBinary(PackageManagerType.BUN.getBinary()) + " run build").equals(s.getParameters()[0])));

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa/build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }
}
