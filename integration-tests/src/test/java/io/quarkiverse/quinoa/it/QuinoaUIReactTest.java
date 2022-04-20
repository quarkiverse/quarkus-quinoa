package io.quarkiverse.quinoa.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import io.quarkiverse.quinoa.testing.QuarkusPlaywrightManager;
import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(QuinoaTestProfiles.Enable.class)
@QuarkusTestResource(QuarkusPlaywrightManager.class)
public class QuinoaUIReactTest {

    @QuarkusPlaywrightManager.InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL url;

    @Test
    public void testUIIndex() {
        final Page page = context.newPage();
        Response response = page.navigate(url.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("React App", title);

        // Make sure the component loaded and hits the backend
        String greeting = page.innerText(".quinoa");
        Assertions.assertEquals("Hello Quinoa", greeting);
    }

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/api/quinoa")
                .then()
                .statusCode(200)
                .body(is("Hello Quinoa"));
    }
}
