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
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson) {
        return new QuinoaConfigDelegate(super.override(delegate, packageJson)) {
            @Override
            public Optional<String> buildDir() {
                // Angular builds a custom directory "dist/[appname]"
                String applicationName = packageJson.map(p -> p.getString("name")).orElse("quinoa");
                final String fullBuildDir = String.format("%s/%s", getFrameworkBuildDir(), applicationName);
                return Optional.of(delegate.buildDir().orElse(fullBuildDir));
            }

            @Override
            public PackageManagerCommandConfig packageManagerCommand() {
                return new PackageManagerCommandConfigDelegate(super.packageManagerCommand()) {
                    @Override
                    public Optional<String> dev() {
                        return Optional.of(delegate.packageManagerCommand().dev()
                                .orElse("run " + getFrameworkDevScriptName() + " -- --disable-host-check"));
                    }

                    @Override
                    public Optional<String> test() {
                        return Optional.of(delegate.packageManagerCommand().test()
                                .orElse(DEFAULT_BUILD_COMMAND + " -- --no-watch --no-progress --browsers=ChromeHeadlessCI"));
                    }
                };
            }
        };
    }
}
