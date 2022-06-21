package io.quarkiverse.quinoa.it;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class QuinoaCodestartIT {

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
    }

    @Test
    void testBuild() throws Throwable {
        codestartTest.buildAllProjects();
    }

}
