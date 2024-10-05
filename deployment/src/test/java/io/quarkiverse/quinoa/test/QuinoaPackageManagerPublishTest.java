package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPackageManagerPublishTest {
    private static final String NAME = "package-manager-publish";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.just-build", "true")
            .overrideConfigKey("quarkus.quinoa.publish", "true")
            .assertLogRecords(l -> {
                assertThat(l)
                        .anyMatch(s -> s.getMessage().startsWith("Running Quinoa package manager publish command"));
            });

    @Test
    public void testQuinoa() {
        // target/quinoa/build dir shall not exist while just-build activated
        assertThat(getWebUITestDirPath(NAME).resolve("quinoa-app-1.0.0.tgz")).exists();
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }

}
