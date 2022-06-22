package io.quarkiverse.quinoa.test.devmode;

import static org.hamcrest.Matchers.is;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ClassicDevModeTest {
    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset(
                            "quarkus.quinoa=true\n" +
                                    "quarkus.quinoa.ui-dir=src/main/webui\n"),
                            "application.properties"))
            .setCodeGenSources("webui");

    @Test
    public void testWebUI() {
        RestAssured.when().get("/").then()
                .statusCode(200)
                .header("Content-Encoding", Matchers.nullValue())
                .body(is("dev\n"));
        RestAssured.when().get("/some-page.html").then()
                .statusCode(200)
                .body(is("Hello Quinoa"));
        test.modifyFile("webui/public/some-page.html", s -> s.replace("Quinoa", "Quinoa with DevMode"));
        RestAssured.when().get("/some-page.html").then()
                .statusCode(200)
                .body(is("Hello Quinoa with DevMode"));
    }
}
