package io.quarkiverse.quinoa.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(TestProfiles.ReactTests.class)
@WithPlaywright
public class QuinoaUIReactTest {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/index.html")
    URL url;

    @TestHTTPResource("/something")
    URL url404;

    @TestHTTPResource("/api/quinoa")
    URL api;

    @Test
    public void testUIIndex() {
        final Page page = context.newPage();
        Response response = page.navigate(url.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("React App", title);

        // Make sure the component loaded and hits the backend
        final ElementHandle quinoaEl = page.waitForSelector(".quinoa.loaded");
        String greeting = quinoaEl.innerText();
        Assertions.assertEquals("Hello Quinoa", greeting);
    }

    @Test
    public void test404Endpoint() {
        given()
                .when().get(url404)
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
