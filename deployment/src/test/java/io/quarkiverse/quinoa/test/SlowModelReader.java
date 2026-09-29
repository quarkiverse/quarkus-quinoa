package io.quarkiverse.quinoa.test;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * Makes the OpenAPI document slow to build, so that a Web UI build running in parallel would not find the schema.
 */
public class SlowModelReader implements OASModelReader {

    public static final String PATH = "/quinoa-waited";

    @Override
    public OpenAPI buildModel() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return OASFactory.createOpenAPI()
                .paths(OASFactory.createPaths().addPathItem(PATH, OASFactory.createPathItem()));
    }
}
