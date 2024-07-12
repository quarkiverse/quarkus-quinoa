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

@QuarkusTest
@TestProfile(TestProfiles.AngularEsBuildTests.class)
@WithPlaywright
public class QuinoaUIAngularEsBuildTest {

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/")
    URL index;

    @TestHTTPResource("/some-route")
    URL someRoute;

    @TestHTTPResource("/bar/foo/baz/not-found")
    URL url404;

    @TestHTTPResource("/bar/foo/api/quinoa")
    URL api;

    @TestHTTPResource("/image%20with%20spaces.svg")
    URL imageWithSpaces;

    @Test
    public void testUIIndex() {
        checkUrl(index);
        checkUrl(someRoute);
    }

    private void checkUrl(URL url) {
        final Page page = context.newPage();
        Response response = page.navigate(url.toString());
        Assertions.assertEquals("OK", response.statusText());

        page.waitForLoadState();

        String title = page.title();
        Assertions.assertEquals("QuinoaApp", title);

        // Make sure the component loaded and hits the backend
        String h1Title = page.innerText("h1");
        Assertions.assertEquals("Hello, quinoa-app", h1Title);

        final ElementHandle quinoaEl = page.waitForSelector(".quinoa");
        String greeting = quinoaEl.innerText();
        Assertions.assertEquals("Hello Quinoa", greeting);
    }

    /**
     * Test an image with spaces "a b.png" get encoded "a%20b.png".
     *
     * @see <a href="https://github.com/quarkiverse/quarkus-quinoa/issues/481">GitHub Issue 481</a>
     */
    @Test
    public void testUIEncodedPath() {
        final Page page = context.newPage();
        Response response = page.navigate(imageWithSpaces.toString());
        Assertions.assertEquals("OK", response.statusText());
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
}
