package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.test.QuinoaPrepareWebUI.prepareLockfile;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerYarnConfigBinaryOnWindowsTest {

    private static final String OSNAME = System.getProperty("os.name");
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setBeforeAllCustomizer(QuinoaPackageManagerYarnConfigBinaryOnWindowsTest::beforeAllCustomizer)
            .setAfterAllCustomizer(QuinoaPackageManagerYarnConfigBinaryOnWindowsTest::afterAllCustomizer)
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/webui")
            .overrideConfigKey("quarkus.quinoa.always-install", "true")
            .setLogRecordPredicate(log -> true)
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo("Running Quinoa package manager install command: %s");
                    assertThat(s.getParameters()[0]).isEqualTo("yarn.cmd install");
                });
            })
            .assertException(e -> assertThat(e).isNotNull());

    private static void afterAllCustomizer() {
        QuinoaPrepareWebUI.deleteLockfiles();
        System.setProperty("os.name", OSNAME);
        System.clearProperty("CI");
    }

    private static void beforeAllCustomizer() {
        prepareLockfile("yarn.lock");
        System.setProperty("os.name", "Windows One");
        System.setProperty("CI", "false");
    }

    @Test
    public void testQuinoa() {
        // test exception
    }

}
