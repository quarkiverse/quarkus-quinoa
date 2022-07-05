package io.quarkiverse.quinoa.it;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class QuinoaCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .setupStandaloneExtensionTest("io.quarkiverse.quinoa:quarkus-quinoa")
            .languages(JAVA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.assertThatGeneratedFile(JAVA, "src/main/webui/package.json")
                .exists();
        codestartTest.assertThatGeneratedTreeMatchSnapshots(JAVA, "src/main/webui");
        codestartTest.assertThatGeneratedFile(JAVA, ".gitignore")
                .satisfies(checkContains("node_modules/"));
    }

    @Test
    void testBuild() throws Throwable {
        codestartTest.buildAllProjects();
    }

}
