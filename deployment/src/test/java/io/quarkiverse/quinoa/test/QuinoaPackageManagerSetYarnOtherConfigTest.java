package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.LockFile.YARN;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerSetYarnOtherConfigTest {
    private static final String NAME = "package-manager-set-yarn-other";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .initialLockfile(YARN)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.package-manager", "yarn.binary")
            .assertException(e -> {
                assertThat(e).hasMessage("Error in Quinoa while running package manager install command: yarn.binary install");
            });

    @Test
    public void testQuinoa() {
        // test logs
    }

}
