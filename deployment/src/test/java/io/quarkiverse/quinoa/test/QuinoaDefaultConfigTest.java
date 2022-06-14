package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaDefaultConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create()
            .toQuarkusUnitTest()
            .overrideConfigKey("quarkus.rest.path", "/foo/bar")
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo("Quinoa is ignoring paths starting with: /foo/bar/, /q/");
                });
            });

    @Test
    public void testQuinoa() {
        assertThat(Path.of("target/quinoa-build/index.html")).isRegularFile()
                .hasContent("test");
        assertThat(Path.of("src/test/webui/node_modules/installed")).isRegularFile()
                .hasContent("hello");
    }
}
