package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.test.QuinoaPrepareWebUI.prepareLockfile;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerYarnConfigBinaryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setBeforeAllCustomizer(() -> prepareLockfile("yarn.lock"))
            .setAfterAllCustomizer(QuinoaPrepareWebUI::deleteLockfiles)
            .overrideConfigKey("quarkus.quinoa.package-manager", "yarn.binary")
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/webui")
            .overrideConfigKey("quarkus.quinoa.always-install", "true")
            .setLogRecordPredicate(log -> true)
            .assertException(e -> {
                assertThat(e).hasMessage("Input/Output error while executing command.");
                assertThat(e.getCause()).hasMessageContaining("yarn.binary");
            });

    @Test
    public void testQuinoa() {
        // test logs
    }

}
