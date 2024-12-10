package io.quarkiverse.quinoa.deployment.framework.override;

import static io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig.DEFAULT_DEV_SCRIPT_NAME;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.DEFAULT_BUILD_DIR;

import java.nio.file.Path;
import java.util.Optional;

import jakarta.json.JsonObject;

import io.quarkiverse.quinoa.deployment.config.DevServerConfig;
import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.DevServerConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.PackageManagerCommandConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;
import io.quarkiverse.quinoa.deployment.framework.FrameworkConfigOverrideFactory;

public class GenericFramework implements FrameworkConfigOverrideFactory {

    public static FrameworkConfigOverrideFactory UNKNOWN_FRAMEWORK = new GenericFramework(DEFAULT_BUILD_DIR,
            DEFAULT_DEV_SCRIPT_NAME);
    private final String defaultBuildDir;
    private final String defaultScriptName;
    private final Optional<Integer> defaultDevServerPort;

    protected GenericFramework(String defaultBuildDir, String defaultScriptName, Optional<Integer> defaultDevServerPort) {
        this.defaultBuildDir = defaultBuildDir;
        this.defaultScriptName = defaultScriptName;
        this.defaultDevServerPort = defaultDevServerPort;
    }

    protected GenericFramework(String defaultBuildDir, String defaultScriptName) {
        this(defaultBuildDir, defaultScriptName, Optional.empty());
    }

    protected GenericFramework(String defaultBuildDir, String defaultScriptName, int defaultDevServerPort) {
        this(defaultBuildDir, defaultScriptName, Optional.of(defaultDevServerPort));
    }

    public static GenericFramework generic(String buildDir, String scriptName, int devServerPort) {
        return new GenericFramework(buildDir, scriptName, devServerPort);
    }

    @Override
    public String getDefaultBuildDir() {
        return defaultBuildDir;
    }

    @Override
    public String getDefaultDevScriptName() {
        return defaultScriptName;
    }

    @Override
    public QuinoaConfig override(QuinoaConfig originalConfig, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized, Path uiDir) {
        final String devScript = detectedDevScript.orElse(defaultScriptName);
        return new QuinoaConfigDelegate(originalConfig) {
            @Override
            public Optional<String> buildDir() {
                return Optional.of(super.buildDir().orElse(defaultBuildDir));
            }

            @Override
            public DevServerConfig devServer() {
                return new DevServerConfigDelegate(super.devServer()) {
                    @Override
                    public Optional<Integer> port() {
                        return Optional.ofNullable(
                                super.port().orElse(
                                        defaultDevServerPort.orElse(null)));
                    }
                };
            }

            @Override
            public PackageManagerCommandConfig packageManagerCommand() {
                return new PackageManagerCommandConfigDelegate(super.packageManagerCommand()) {
                    @Override
                    public Optional<String> dev() {
                        return Optional.of(super.dev().orElse("run " + devScript));
                    }
                };
            }
        };
    }
}