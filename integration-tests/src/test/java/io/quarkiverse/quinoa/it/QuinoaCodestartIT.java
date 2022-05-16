package io.quarkiverse.quinoa.it;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.ArtifactCoords;

public class QuinoaCodestartIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .standaloneExtensionCatalog()
            .extension(
                    ArtifactCoords.fromString("io.quarkiverse.quinoa:quarkus-quinoa:" + System.getProperty("project.version")))
            .languages(JAVA)
            .build();

    static {
        if (!System.getProperties().containsKey("project.version")) {
            throw new IllegalStateException("project.version property is required");
        }
    }

    @Test
    void testContent() throws Throwable {
        codestartTest.assertThatGeneratedFile(JAVA, "src/main/webui/package.json");
        codestartTest.assertThatGeneratedTreeMatchSnapshots(JAVA, "src/main/webui");
    }

    @Test
    void testBuild() throws Throwable {
        codestartTest.buildAllProjects();
    }

}
