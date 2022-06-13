package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.test.QuinoaPrepareWebUI.prepare;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerNpmConfigBinaryOnWindowsTest {

    private static final String OSNAME = System.getProperty("os.name");
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setBeforeAllCustomizer(QuinoaPackageManagerNpmConfigBinaryOnWindowsTest::beforeAllCustomizer)
            .setAfterAllCustomizer(QuinoaPackageManagerNpmConfigBinaryOnWindowsTest::afterAllCustomizer)
            .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/webui")
            .overrideConfigKey("quarkus.quinoa.always-install", "true")
            .setLogRecordPredicate(log -> true)
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo("Running Quinoa package manager install command: %s");
                    assertThat(s.getParameters()[0]).isEqualTo("npm.cmd install");
                });
            })
            .assertException(e -> assertThat(e).isNotNull());

    private static void afterAllCustomizer() {
        QuinoaPrepareWebUI.clean();
        System.setProperty("os.name", OSNAME);
    }

    private static void beforeAllCustomizer() {
        prepare();
        System.setProperty("os.name", "Windows One");
    }

    @Test
    public void testQuinoa() {
        // test exception
    }

}
