package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.LockFile.YARN;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerYarnConfigBinaryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create()
            .initialLockFile(YARN)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.package-manager", "yarn.binary")
            .overrideConfigKey("quarkus.quinoa.always-install", "true")
            .assertException(e -> {
                assertThat(e).hasMessage("Input/Output error while executing command.");
                assertThat(e.getCause()).hasMessageContaining("yarn.binary");
            });

    @Test
    public void testQuinoa() {
        // test logs
    }

}
