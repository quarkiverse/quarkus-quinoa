package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.LockFile.YARN;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerYarnConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create()
            .initialLockFile(YARN)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.always-install", "true")
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo("Running Quinoa package manager build command: %s");
                    assertThat(s.getParameters()[0]).isEqualTo("yarn run build");
                });
            });

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(Path.of("src/test/webui/node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }

}
