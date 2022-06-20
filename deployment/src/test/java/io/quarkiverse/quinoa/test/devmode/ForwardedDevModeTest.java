package io.quarkiverse.quinoa.test.devmode;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ForwardedDevModeTest {
    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("application-forwarded.properties", "application.properties"))
            .setCodeGenSources("webui");

    @Test
    public void testWebUI() {
        RestAssured.when().get("/").then()
                .statusCode(200)
                .body(is("live-coding\n"));
    }
}
