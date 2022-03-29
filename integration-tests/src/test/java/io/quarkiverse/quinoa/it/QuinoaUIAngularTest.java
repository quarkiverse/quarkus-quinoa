package io.quarkiverse.quinoa.it;

import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(QuinoaTestProfiles.AngularTests.class)
@QuarkusTestResource(PlaywrightManager.class)
public class QuinoaUIAngularTest {

    @PlaywrightManager.InjectPlaywright
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
        Assertions.assertEquals("QuinoaApp", title);

        // Make sure the component loaded and hits the backend
        String greeting = page.innerText(".content span");
        Assertions.assertEquals("quinoa-app app is running!", greeting);
    }
}
