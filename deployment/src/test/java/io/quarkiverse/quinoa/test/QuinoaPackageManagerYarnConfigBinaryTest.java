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
            .overrideConfigKey("quarkus.quinoa.force-install", "true")
            .assertException(e -> {
                assertThat(e).hasMessage("Error in Quinoa while running package manager install command: yarn.binary install");
            });

    @Test
    public void testQuinoa() {
        // test logs
    }

}
