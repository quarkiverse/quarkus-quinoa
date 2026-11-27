package io.quarkiverse.quinoa.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Percent-encoded request paths must be decoded before being matched against the (decoded) ignored path prefixes
 * (https://github.com/quarkiverse/quarkus-quinoa/issues/830).
 */
public class QuinoaSPARoutingEncodedPathTest {

    private static final String NAME = "spa-routing-encoded-path";

    @RegisterExtension
    static final QuarkusExtensionTest config = QuinoaQuarkusUnitTest.create(NAME)
            .toQuarkusExtensionTest()
            .overrideConfigKey("quarkus.quinoa.ignored-path-prefixes", "/café,/日本語")
            .overrideConfigKey("quarkus.quinoa.enable-spa-routing", "true");

    @Test
    public void encodedIgnoredPathsAreNotReroutedToIndex() {
        given().urlEncodingEnabled(false).when().get("/caf%C3%A9").then().statusCode(404);
        given().urlEncodingEnabled(false).when().get("/caf%C3%A9/foo").then().statusCode(404);
        given().urlEncodingEnabled(false).when().get("/%E6%97%A5%E6%9C%AC%E8%AA%9E/foo").then().statusCode(404);
    }

    @Test
    public void encodedSpaPathsAreReroutedToIndex() {
        given().urlEncodingEnabled(false).when().get("/some%20page").then().statusCode(200).body(containsString("test"));
        // not the same path segment as the ignored "/café" prefix
        given().urlEncodingEnabled(false).when().get("/caf%C3%A9s").then().statusCode(200).body(containsString("test"));
    }
}
