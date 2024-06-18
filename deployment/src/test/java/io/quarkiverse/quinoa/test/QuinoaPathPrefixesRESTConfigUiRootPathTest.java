package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPathPrefixesRESTConfigUiRootPathTest {

    private static final String NAME = "resteasy-reactive-path-config-ui-root-path";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.http.root-path", "/root/path")
            .overrideConfigKey("quarkus.quinoa.ui-root-path", "/foo")
            .overrideConfigKey("quarkus.rest.path", "/foo/reactive")
            .overrideConfigKey("quarkus.resteasy.path", "/foo/classic")
            .overrideConfigKey("quarkus.http.non-application-root-path", "/bar/non")
            .overrideConfigKey("quarkus.quinoa.enable-spa-routing", "true")
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage()
                            // ignored paths are always relative to the ui root path
                            .equals("Quinoa SPA routing handler is ignoring paths starting with: /classic, /reactive"))
                    .anyMatch(s -> s.getMessage()
                            .equals("Quinoa is available at: /root/path/foo/")));

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa/build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }
}
