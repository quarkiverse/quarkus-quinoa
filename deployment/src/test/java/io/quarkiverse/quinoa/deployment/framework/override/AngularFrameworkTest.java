package io.quarkiverse.quinoa.deployment.framework.override;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class AngularFrameworkTest {

    @ParameterizedTest
    @CsvSource({
            "builder-browser-output-str, dist/acme-frontend",
            "builder-application-output-str, dist/acme-frontend/browser",
            "builder-application-output-obj-no-browser, dist/acme-frontend/browser",
            "builder-application-output-obj-browser, dist/acme-frontend/ui",
    })
    void testBuilderOutputPath(String jsonResource, String expectedBuildDir) {
        final String projectName = "acme-frontend";
        // given
        JsonObject givenBuilder = readTestJson(jsonResource);

        // when
        String actual = AngularFramework.getBuildDir(givenBuilder, projectName);

        // then
        assertThat(actual).isEqualTo(expectedBuildDir);
    }

    private JsonObject readTestJson(String jsonResource) {
        ClassLoader classLoader = AngularFrameworkTest.class.getClassLoader();
        InputStream jsonStream = classLoader.getResourceAsStream("frameworks/angular/" + jsonResource + ".json");
        return Json.createReader(jsonStream).readObject();
    }

}
