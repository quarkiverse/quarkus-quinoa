package io.quarkiverse.quinoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.quinoa.deployment.testing.QuinoaQuarkusUnitTest;
import io.quarkus.test.QuarkusUnitTest;

public class QuinoaIgnorePathPrefixesConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = QuinoaQuarkusUnitTest.create().toQuarkusUnitTest()
            .overrideConfigKey("quarkus.quinoa.ignored-path-prefixes", "/foo/bar,/api,q,a/b")
            .assertLogRecords(l -> {
                assertThat(l).anySatisfy(s -> {
                    assertThat(s.getMessage()).isEqualTo("Quinoa is ignoring paths starting with: /foo/bar, /api, /q, /a/b");
                });
            });

    @Test
    public void testQuinoa() {
        // Will assert log records
    }

}
