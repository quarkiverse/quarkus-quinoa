package io.quarkiverse.quinoa.deployment.framework.override;

import java.util.Optional;

import jakarta.json.JsonObject;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.PackageManagerCommandConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;

public class AngularFramework extends GenericFramework {

    public AngularFramework() {
        super("dist", "start", 4200);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig originalConfig, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized) {
        final String devScript = detectedDevScript.orElse(getDefaultDevScriptName());
        return new QuinoaConfigDelegate(super.override(originalConfig, packageJson, detectedDevScript, isCustomized)) {
            @Override
            public Optional<String> buildDir() {
                // Angular builds a custom directory "dist/[appname]"
                String applicationName = packageJson.map(p -> p.getString("name")).orElse("quinoa");
                final String fullBuildDir = String.format("%s/%s", getDefaultBuildDir(), applicationName);
                return Optional.of(originalConfig.buildDir().orElse(fullBuildDir));
            }

            @Override
            public PackageManagerCommandConfig packageManagerCommand() {
                return new PackageManagerCommandConfigDelegate(super.packageManagerCommand()) {
                    @Override
                    public Optional<String> dev() {
                        final String extraArgs = isCustomized ? "" : " -- --disable-host-check --hmr";
                        return Optional.of(originalConfig.packageManagerCommand().dev()
                                .orElse("run " + devScript + extraArgs));
                    }

                    @Override
                    public Optional<String> test() {
                        final String extraArgs = isCustomized ? "" : " -- --no-watch --no-progress --browsers=ChromeHeadlessCI";
                        return Optional.of(originalConfig.packageManagerCommand().test()
                                .orElse(DEFAULT_TEST_COMMAND + extraArgs));
                    }
                };
            }
        };
    }
}
