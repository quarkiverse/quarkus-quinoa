package io.quarkiverse.quinoa.test;

import static org.apache.commons.io.file.PathUtils.copyFileToDirectory;
import static org.apache.commons.io.file.PathUtils.delete;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerNPMConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setBeforeAllCustomizer(() -> prepareLockFiles("package-lock.json"))
            .setAfterAllCustomizer(QuinoaPackageManagerNPMConfigTest::deleteLockFiles)
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/webui")
            .overrideConfigKey("quarkus.quinoa.always-install", "true")
            .setLogRecordPredicate(log -> true)
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo("Running Quinoa package manager build command: %s");
                    assertThat(s.getParameters()[0]).isEqualTo("npm run build");
                });
            });

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(Path.of("src/test/webui/node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }

    public static void prepareLockFiles(String toUse) {
        deleteLockFiles();
        try {
            copyFileToDirectory(Path.of("src/test/resources/lockfiles/", toUse), Path.of("src/test/webui"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void deleteLockFiles() {
        try {
            delete(Path.of("src/test/webui/package-lock.json"));
            delete(Path.of("src/test/webui/pnpm-lock.yaml"));
            delete(Path.of("src/test/webui/yarn.lock"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
