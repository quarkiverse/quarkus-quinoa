package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.isWindows;
import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.systemBinary;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaAbsoluteUIDirTest {
    private static final String NAME = "test-webui-absolute-dir";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(getUIDir().resolve("webui")).toQuarkusUnitTest()
            .overrideConfigKey("quarkus.package.output-directory", getUIDir().resolve("target").toAbsolutePath().toString())
            .assertLogRecords(l -> {
                assertThat(l)
                        .anyMatch(s -> s.getMessage().equals("Running Quinoa package manager build command: %s") &&
                                s.getParameters()[0].equals(systemBinary("npm") + " run build"));
                assertThat(l)
                        .anyMatch(s -> s.getMessage().equals("Quinoa is ignoring paths starting with: /q/"));
            });

    static {
        try {
            Files.createDirectories(getUIDir().resolve("target"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testQuinoa() {
        assertThat(getUIDir().resolve(Path.of("target/quinoa/build/index.html"))).isRegularFile()
                .hasContent("test");
        assertThat(getUIDir().resolve("webui").resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }

    private static Path getUIDir() {
        final Path tmpDir = isWindows() ? Path.of("C:/Temp") : Path.of("/tmp");
        return tmpDir.resolve(NAME);
    }

}
