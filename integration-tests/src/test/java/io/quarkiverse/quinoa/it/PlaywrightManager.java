package io.quarkiverse.quinoa.it;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class PlaywrightManager implements QuarkusTestResourceLifecycleManager {

    private BrowserContext context;
    private Playwright playwright;

    @Override
    public Map<String, String> start() {
        playwright = com.microsoft.playwright.Playwright.create();
        Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setArgs(List.of("--headless", "--disable-gpu", "--no-sandbox")));
        context = browser.newContext();
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(context,
                new TestInjector.AnnotatedAndMatchesType(InjectPlaywright.class, BrowserContext.class));
        testInjector.injectIntoFields(playwright,
                new TestInjector.AnnotatedAndMatchesType(InjectPlaywright.class, Playwright.class));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface InjectPlaywright {
    }

}
