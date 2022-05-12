package io.quarkiverse.quinoa.test.devmode;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Disabled // See https://github.com/quarkusio/quarkus/pull/25504
public class ClassicDevModeTest {
    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("application-classic.properties", "application.properties"))
            .setCodeGenSources("webui");

    @Test
    public void testWebUI() {
        RestAssured.when().get("/").then()
                .statusCode(200)
                .body(is("dev\n"));
        RestAssured.when().get("/some-page.html").then()
                .statusCode(200)
                .body(is("Hello Quinoa"));
        test.modifyFile("webui/src/some-page.html", s -> s.replace("Quinoa", "Quinoa with DevMode"));
        RestAssured.when().get("/some-page.html").then()
                .statusCode(200)
                .body(is("Hello Quinoa with DevMode"));
    }
}
