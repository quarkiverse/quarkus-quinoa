package io.quarkiverse.quinoa.test;

import static io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest.getWebUITestDirPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaPathPrefixesRESTConfigTest {

    private static final String NAME = "resteasy-reactive-path-config";

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create(NAME)
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.resteasy-reactive.path", "/foo/reactive")
            .overrideConfigKey("quarkus.resteasy.path", "/foo/classic")
            .overrideConfigKey("quarkus.http.non-application-root-path", "/bar/non")
            .assertLogRecords(l -> assertThat(l)
                    .anyMatch(s -> s.getMessage()
                            .equals("Quinoa is ignoring paths starting with: /foo/classic/, /foo/reactive/, /bar/non/")));

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(getWebUITestDirPath(NAME).resolve("node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }
}
