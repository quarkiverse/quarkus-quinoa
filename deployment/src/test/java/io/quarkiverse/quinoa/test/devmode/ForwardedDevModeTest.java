package io.quarkiverse.quinoa.test.devmode;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class ForwardedDevModeTest {
    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset(
                            "quarkus.quinoa=true\n" +
                                    "quarkus.quinoa.ui-dir=src/main/webui\n" +
                                    "quarkus.quinoa.dev-server.port=3000\n"),
                            "application.properties"))
            .setCodeGenSources("webui");

    @Test
    public void testWebUI() {
        RestAssured.when().get("/").then()
                .statusCode(200)
                .header("Content-Encoding", Matchers.nullValue())
                .body(is("live-coding\n"));
        given()
                .body("{}")
                .contentType(ContentType.JSON)
                .when().post("/api/something").then()
                .statusCode(404);
    }
}
