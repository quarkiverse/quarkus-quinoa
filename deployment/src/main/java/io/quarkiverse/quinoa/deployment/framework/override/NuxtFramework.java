package io.quarkiverse.quinoa.deployment.framework.override;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.PackageManagerCommandConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;
import jakarta.json.JsonObject;

public class NuxtFramework extends GenericFramework {

    public NuxtFramework() {
        super("dist", "dev", 3000);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript,
            boolean isCustomized) {
        return new QuinoaConfigDelegate(super.override(delegate, packageJson, detectedDevScript, isCustomized)) {

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
