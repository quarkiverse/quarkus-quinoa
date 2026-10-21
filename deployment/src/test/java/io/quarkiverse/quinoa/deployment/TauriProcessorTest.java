package io.quarkiverse.quinoa.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.quinoa.deployment.config.TauriConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

class TauriProcessorTest {

    private final TauriProcessor processor = new TauriProcessor();

    @TempDir
    Path buildDir;

    @Test
    void findsRunnerInBuildDirWhenPackageDirIsQuarkusApp() throws IOException {
        Path runner = Files.createFile(buildDir.resolve("app-1.0-runner"));

        Path resolved = processor.resolveNativeImageBinary(config(null), outputTarget(buildDir.resolve("quarkus-app")));

        assertThat(resolved).isEqualTo(runner);
    }

    @Test
    void findsRunnerInCustomPackageOutputDir() throws IOException {
        Path packageDir = Files.createDirectories(buildDir.resolve("custom"));
        Path runner = Files.createFile(packageDir.resolve("app-1.0-runner"));

        Path resolved = processor.resolveNativeImageBinary(config(null), outputTarget(packageDir));

        assertThat(resolved).isEqualTo(runner);
    }

    @Test
    void resolvesConfiguredRelativeBinaryFromBuildDir() {
        Path resolved = processor.resolveNativeImageBinary(config("native/my-app"),
                outputTarget(buildDir.resolve("quarkus-app")));

        assertThat(resolved).isEqualTo(buildDir.resolve("native/my-app"));
    }

    @Test
    void returnsNullWhenNoRunnerFound() {
        Path resolved = processor.resolveNativeImageBinary(config(null), outputTarget(buildDir.resolve("quarkus-app")));

        assertThat(resolved).isNull();
    }

    private OutputTargetBuildItem outputTarget(Path packageOutputDir) {
        return new OutputTargetBuildItem(buildDir, packageOutputDir, "app-1.0", "app-1.0", false, new Properties(),
                Optional.empty());
    }

    private static TauriConfig config(String nativeImageBinary) {
        return (TauriConfig) Proxy.newProxyInstance(TauriConfig.class.getClassLoader(), new Class<?>[] { TauriConfig.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("nativeImageBinary")) {
                        return Optional.ofNullable(nativeImageBinary);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
