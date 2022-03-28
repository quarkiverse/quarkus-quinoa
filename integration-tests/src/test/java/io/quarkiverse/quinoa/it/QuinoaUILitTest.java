package io.quarkiverse.quinoa.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(QuinoaTestProfiles.LitTests.class)
public class QuinoaUILitTest {

    @Test
    public void testUIIndex() {
        given()
                .when().get("/")
                .then()
                .statusCode(200)
                .body(containsString("<title>Quinoa Lit App</title>"));
        given()
                .when().get("/index.html")
                .then()
                .statusCode(200)
                .body(containsString("<title>Quinoa Lit App</title>"));
    }
}
