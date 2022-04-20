package io.quarkiverse.quinoa.it;

import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import io.quarkiverse.quinoa.testing.QuarkusPlaywrightManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(TestProfiles.VueTests.class)
@QuarkusTestResource(QuarkusPlaywrightManager.class)
public class QuinoaUIVueTest {

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
        Assertions.assertEquals("hello-quinoa", title);

        // Make sure the component loaded and hits the backend
        String greeting = page.innerText(".hello h1");
        Assertions.assertEquals("Welcome to Your Vue Quinoa App", greeting);
    }
}
