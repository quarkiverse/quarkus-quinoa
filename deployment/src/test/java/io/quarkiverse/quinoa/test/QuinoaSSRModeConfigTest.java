package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class QuinoaSSRModeConfigTest {

    private static final String NAME = "ssr-mode-config";

    @RegisterExtension
    static final QuarkusExtensionTest config = QuinoaQuarkusUnitTest.create(NAME)
            .toQuarkusExtensionTest()
            .overrideConfigKey("quarkus.quinoa.enable-ssr-mode", "true")
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage()
                            .equals("Quinoa is in SSR mode: the build output is not served as static resources")));

    @Test
    public void testSSRModeConfigLoads() {
        // Test that SSR mode config doesn't break the build
        assertThat(Path.of("target/quinoa/build/index.html")).isRegularFile()
                .hasContent("test");
    }

    @Test
    public void testSSRModeBuildOutputIsNotServed() {
        // The SSR build output needs a Node.js server, Quarkus must not publish it as static resources
        RestAssured.when().get("/").then().statusCode(404);
        RestAssured.when().get("/index.html").then().statusCode(404);
        RestAssured.when().get("/some-page.html").then().statusCode(404);
    }
}
