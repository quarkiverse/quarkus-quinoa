package io.quarkiverse.quinoa.deployment.framework.override;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;

class NextFrameworkTest {

    @Test
    void testDefaultIsStaticExport() {
        final QuinoaConfig config = override(Map.of());
        assertThat(config.enableSSRMode()).isFalse();
        assertThat(config.buildDir()).contains("out");
    }

    @Test
    void testSSRModeUsesNextBuildDir() {
        final QuinoaConfig config = override(Map.of("enableSSRMode", true));
        assertThat(config.enableSSRMode()).isTrue();
        assertThat(config.buildDir()).contains(".next");
    }

    @Test
    void testBuildDirWinsInStaticExport() {
        final QuinoaConfig config = override(Map.of("buildDir", Optional.of("custom")));
        assertThat(config.enableSSRMode()).isFalse();
        assertThat(config.buildDir()).contains("custom");
    }

    @Test
    void testBuildDirWinsInSSRMode() {
        final QuinoaConfig config = override(Map.of("enableSSRMode", true, "buildDir", Optional.of("custom")));
        assertThat(config.enableSSRMode()).isTrue();
        assertThat(config.buildDir()).contains("custom");
    }

    @Test
    void testDevIndexPageIsRoot() {
        assertThat(override(Map.of()).devServer().indexPage()).contains("/");
        assertThat(override(Map.of("enableSSRMode", true)).devServer().indexPage()).contains("/");
        assertThat(override(Map.of("indexPage", Optional.of("main.html"))).devServer().indexPage())
                .contains("main.html");
    }

    @Test
    void testDevServerDefaults() {
        final QuinoaConfig config = override(Map.of());
        assertThat(config.devServer().port()).contains(3000);
        assertThat(config.packageManagerCommand().dev()).contains("run dev");
    }

    private static QuinoaConfig override(Map<String, Object> userValues) {
        return new NextFramework().override(stub(QuinoaConfig.class, userValues), Optional.empty(), Optional.of("dev"),
                false, Path.of("."));
    }

    /**
     * Stubs a config interface: the given values by method name, otherwise empty/false, and nested config groups
     * stubbed the same way.
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Map<String, Object> values) {
        return (T) Proxy.newProxyInstance(NextFrameworkTest.class.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> {
                    if (values.containsKey(method.getName())) {
                        return values.get(method.getName());
                    }
                    final Class<?> returnType = method.getReturnType();
                    if (returnType == Optional.class) {
                        return Optional.empty();
                    }
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == Map.class) {
                        return Map.of();
                    }
                    if (returnType.isInterface()) {
                        return stub(returnType, values);
                    }
                    return null;
                });
    }
}
