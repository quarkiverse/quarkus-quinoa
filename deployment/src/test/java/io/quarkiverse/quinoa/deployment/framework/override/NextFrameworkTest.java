package io.quarkiverse.quinoa.deployment.framework.override;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;

class NextFrameworkTest {

    @Test
    void testIsStaticExport_whenOutputExport() {
        JsonObject json = Json.createObjectBuilder().add("output", "export").build();
        assertThat(NextFramework.isStaticExport(Optional.of(json))).isTrue();
    }

    @Test
    void testIsStaticExport_whenOutputIsOtherValue() {
        JsonObject json = Json.createObjectBuilder().add("output", "standalone").build();
        assertThat(NextFramework.isStaticExport(Optional.of(json))).isFalse();
    }

    @Test
    void testIsStaticExport_whenNoOutputField() {
        JsonObject json = Json.createObjectBuilder()
                .add("scripts", Json.createObjectBuilder().add("dev", "next dev"))
                .build();
        assertThat(NextFramework.isStaticExport(Optional.of(json))).isFalse();
    }

    @Test
    void testIsStaticExport_whenEmpty() {
        assertThat(NextFramework.isStaticExport(Optional.empty())).isFalse();
    }

    @Test
    void testBuildDirIsNextForAppRouter() {
        JsonObject json = readFixture("next-exact");
        boolean staticExport = NextFramework.isStaticExport(Optional.of(json));
        assertThat(staticExport).isFalse();
        // App Router → .next build dir
        String buildDir = staticExport ? NextFramework.EXPORT_BUILD_DIR : NextFramework.SSR_BUILD_DIR;
        assertThat(buildDir).isEqualTo(".next");
    }

    @Test
    void testBuildDirIsOutForStaticExport() {
        JsonObject json = readFixture("next-with-export");
        boolean staticExport = NextFramework.isStaticExport(Optional.of(json));
        assertThat(staticExport).isTrue();
        // Static export → out build dir
        String buildDir = staticExport ? NextFramework.EXPORT_BUILD_DIR : NextFramework.SSR_BUILD_DIR;
        assertThat(buildDir).isEqualTo("out");
    }

    private JsonObject readFixture(String name) {
        InputStream stream = NextFrameworkTest.class.getClassLoader()
                .getResourceAsStream("frameworks/" + name + ".json");
        return Json.createReader(stream).readObject();
    }
}