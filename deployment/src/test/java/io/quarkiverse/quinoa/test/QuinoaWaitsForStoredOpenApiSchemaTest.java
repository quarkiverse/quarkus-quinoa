package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

public class QuinoaWaitsForStoredOpenApiSchemaTest {

    private static final String NAME = "openapi-store-schema";

    // the build command copies openapi.yaml to the build output, and fails if it is not written yet
    @RegisterExtension
    static final QuarkusExtensionTest config = QuinoaQuarkusUnitTest.create(NAME).toQuarkusExtensionTest()
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-smallrye-openapi", Version.getVersion())))
            .withApplicationRoot(jar -> jar.addClass(SlowModelReader.class))
            .overrideConfigKey("mp.openapi.model.reader", SlowModelReader.class.getName())
            .overrideConfigKey("quarkus.smallrye-openapi.store-schema-directory",
                    getWebUITestDirPath(NAME).toAbsolutePath().toString())
            .overrideConfigKey("quarkus.quinoa.package-manager-command.build", "run build-openapi");

    @Test
    public void testWebUIBuildReadsStoredSchema() {
        assertThat(Path.of("target/quinoa/build/openapi.yaml")).isRegularFile()
                .content().contains(SlowModelReader.PATH);
    }
}
