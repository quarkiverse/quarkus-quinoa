package io.quarkiverse.quinoa.deployment.framework.override;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.json.JsonObject;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.PackageManagerCommandConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;

public class ReactFramework extends GenericFramework {

    public ReactFramework() {
        super("build", "start", 3000);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson) {
        return new QuinoaConfigDelegate(super.override(delegate, packageJson)) {

            @Override
            public PackageManagerCommandConfig packageManagerCommand() {
                return new PackageManagerCommandConfigDelegate(super.packageManagerCommand()) {
                    @Override
                    public Map<String, String> devEnv() {
                        // BROWSER=NONE so the browser is not automatically opened with React
                        Map<String, String> envs = new HashMap<>(super.testEnv());
                        envs.put("BROWSER", "NONE");
                        return envs;
                    }
                };
            }
        };
    }
}
