package io.quarkiverse.quinoa.it;

import static io.restassured.RestAssured.given;

import java.net.URL;

import org.hamcrest.Matchers;
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

@QuarkusTest
@TestProfile(TestProfiles.UiRootPathTests.class)
@WithPlaywright
public class QuinoaUiRootPathTest {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL urlRoot;

    @TestHTTPResource("/ui/ignored")
    URL url404Ignored;

    @TestHTTPResource("/ui/ignored/sub-path")
    URL url404IgnoredSubPath;

    // this path starts with /ignored, but is not a sub path of /ignored
    @TestHTTPResource("/ui/ignored-not-ignored")
    URL urlNotIgnored;

    @TestHTTPResource("/ui")
    URL url;

    @TestHTTPResource("/ui/some-route")
    URL urlRoute;

    @Test
    public void testRoot() {
        // the root is the index.html in /META-INF/resources and not Quinoa
        given()
                .when().get(urlRoot)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("NOT QUINOA"));
    }

    @Test
    public void test404Endpoint() {
        given()
                .when().get(url404Ignored)
                .then()
                .statusCode(404);
        given()
                .when().get(url404IgnoredSubPath)
                .then()
                .statusCode(404);
    }

    @Test
    public void testUIIndex() {
        final Page page = context.newPage();
        Response response = page.navigate(url.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("Quinoa Lit App", title);

        // Make sure the component loaded and hits the backend
        final ElementHandle quinoaEl = page.waitForSelector(".greeting");
        String greeting = quinoaEl.innerText();
        Assertions.assertEquals("Hello Quinoa and World and bar", greeting);
    }

    @Test
    public void testRoute() {
        final Page page = context.newPage();
        Response response = page.navigate(urlRoute.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("Quinoa Lit App", title);
    }

    @Test
    public void testNotIgnored() {
        final Page page = context.newPage();
        Response response = page.navigate(urlNotIgnored.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("Quinoa Lit App", title);
    }
}
