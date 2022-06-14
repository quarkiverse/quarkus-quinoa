package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.LockFile.PNPM;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerPNPMConfigBinaryOnWindowsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create()
            .initialLockFile(PNPM)
            .osName("Windows One")
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.always-install", "true")
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo("Running Quinoa package manager install command: %s");
                    assertThat(s.getParameters()[0]).isEqualTo("pnpm.cmd install");
                });
            })
            .assertException(e -> assertThat(e).isNotNull());

    @Test
    public void testQuinoa() {
        // test exception
    }

}
