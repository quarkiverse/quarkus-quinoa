package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType;
import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaCIConfigTest {

    private static final String NAME = "ci";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .initialLockfile(PackageManagerType.YARN.getLockFile())
            .ci(null)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.ci", "true")
            .assertException(e -> {
                assertThat(e)
                        .hasMessage("Error in Quinoa while running package manager ci command: yarn.cmd install --immutable");
            })
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage().contains(
                            "The lockfile would have been modified by this install, which is explicitly forbidden.")));

    @Test
    public void testQuinoa() {
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }
}
