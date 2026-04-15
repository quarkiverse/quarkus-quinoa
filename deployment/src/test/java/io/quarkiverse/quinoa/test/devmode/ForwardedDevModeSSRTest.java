package io.quarkiverse.quinoa.test.devmode;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class ForwardedDevModeSSRTest {

    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset(
                            "quarkus.quinoa=true\n" +
                                    "quarkus.quinoa.ui-dir=src/main/ssr-webui\n" +
                                    "quarkus.quinoa.dev-server.port=3000\n" +
                                    "quarkus.quinoa.enable-ssr-mode=true\n"),
                            "application.properties"))
            .setCodeGenSources("ssr-webui");

    @Test
    public void testSSRRouting() {
        // Root path is forwarded normally
        when().get("/").then()
                .statusCode(200)
                .body(containsString("ssr-root-page"));

        // SSR route: extensionless paths whose dev server response is text/html must be
        // forwarded. Without enable-ssr-mode Quinoa would drop the HTML response and
        // fall through to Quarkus (returning 404).
        when().get("/trainings").then()
                .statusCode(200)
                .body(containsString("ssr-trainings-page"));

        // Static Next.js assets are forwarded regardless of SSR mode
        when().get("/_next/static/main.js").then()
                .statusCode(200);

        // Paths the dev server does not know about fall through to Quarkus
        when().get("/unknown-path").then()
                .statusCode(404);
    }
}