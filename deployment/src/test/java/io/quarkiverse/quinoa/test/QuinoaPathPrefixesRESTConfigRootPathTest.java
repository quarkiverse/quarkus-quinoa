package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPathPrefixesRESTConfigRootPathTest {

    private static final String NAME = "resteasy-reactive-path-config-root-path";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.http.root-path", "/root/path")
            .overrideConfigKey("quarkus.resteasy-reactive.path", "/foo/reactive")
            .overrideConfigKey("quarkus.resteasy.path", "/foo/classic")
            .overrideConfigKey("quarkus.http.non-application-root-path", "/bar/non")
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage()
                            // note how /bar/non is not part of the ignored paths
                            // this is because /bar/non is not relative to /root/path
                            // also note that quarkus.rest.path, and quarkus.resteasy.path are always relative to the root path even if they start with a slash
                            .equals("Quinoa is ignoring paths starting with: /foo/classic, /foo/reactive"))
                    .anyMatch(s -> s.getMessage()
                            .equals("Quinoa is available at: /root/path/")));

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa/build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }
}
