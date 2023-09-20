package io.quarkiverse.quinoa.deployment.framework.override;

import static io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig.DEFAULT_DEV_SCRIPT_NAME;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.DEFAULT_BUILD_DIR;

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
    private final String buildDir;
    private final String scriptName;
    private final Optional<Integer> devServerPort;

    protected GenericFramework(String buildDir, String scriptName, Optional<Integer> devServerPort) {
        this.buildDir = buildDir;
        this.scriptName = scriptName;
        this.devServerPort = devServerPort;
    }

    protected GenericFramework(String buildDir, String scriptName) {
        this(buildDir, scriptName, Optional.empty());
    }

    protected GenericFramework(String buildDir, String scriptName, int devServerPort) {
        this(buildDir, scriptName, Optional.of(devServerPort));
    }

    public static GenericFramework generic(String buildDir, String scriptName, int devServerPort) {
        return new GenericFramework(buildDir, scriptName, devServerPort);
    }

    @Override
    public String getFrameworkBuildDir() {
        return buildDir;
    }

    @Override
    public String getFrameworkDevScriptName() {
        return scriptName;
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson) {
        return new QuinoaConfigDelegate(delegate) {
            @Override
            public Optional<String> buildDir() {
                return Optional.of(super.buildDir().orElse(buildDir));
            }

            @Override
            public DevServerConfig devServer() {
                return new DevServerConfigDelegate(super.devServer()) {
                    @Override
                    public Optional<Integer> port() {
                        return Optional.ofNullable(
                                super.port().orElse(
                                        devServerPort.orElse(null)));
                    }
                };
            }

            @Override
            public PackageManagerCommandConfig packageManagerCommand() {
                return new PackageManagerCommandConfigDelegate(super.packageManagerCommand()) {
                    @Override
                    public Optional<String> dev() {
                        return Optional.of(super.dev().orElse("run " + scriptName));
                    }
                };
            }
        };
    }
}
