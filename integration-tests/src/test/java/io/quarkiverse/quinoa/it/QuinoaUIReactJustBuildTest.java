package io.quarkiverse.quinoa.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(TestProfiles.ReactJustBuildTests.class)
public class QuinoaUIReactJustBuildTest {

    @TestHTTPResource("/")
    URL root;

    @TestHTTPResource("/logo192.png")
    URL logo;

    @TestHTTPResource("/api/quinoa")
    URL api;

    @Test
    public void testIndexFromResources() {
        given()
                .when().get(root)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("NOT QUINOA"));
    }

    @Test
    public void testLogo404() {
        given()
                .when().get(logo)
                .then()
                .statusCode(404);
    }

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get(api)
                .then()
                .statusCode(200)
                .body(is("Hello Quinoa"));
    }

    @Test
    public void testHelloEndpointPost() {
        given()
                .body("bowl")
                .contentType(ContentType.TEXT)
                .when().post(api)
                .then()
                .statusCode(200)
                .body(is("Hello Quinoa bowl"));
    }
}
