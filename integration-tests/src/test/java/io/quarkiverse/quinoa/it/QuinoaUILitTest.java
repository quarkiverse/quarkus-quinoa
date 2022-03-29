package io.quarkiverse.quinoa.it;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(QuinoaTestProfiles.LitTests.class)
public class QuinoaUILitTest {

    @TestHTTPResource("index.html")
    URL url;

    @Test
    public void testUIIndex() {

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setArgs(List.of("--headless", "--disable-gpu", "--no-sandbox")));

            BrowserContext context = browser.newContext();

            Page page = context.newPage();
            Response response = page.navigate(url.toString());
            Assertions.assertEquals("OK", response.statusText());

            page.waitForLoadState();

            String title = page.title();
            Assertions.assertEquals("Quinoa Lit App", title);

            // Make sure the component loaded and hits the backend
            String greeting = page.innerText(".greeting");
            Assertions.assertEquals("Hello Quinoa and World", greeting);
        }
    }
}
