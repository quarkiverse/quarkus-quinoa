package io.quarkiverse.quinoa.deployment.framework;

import java.util.Optional;

import jakarta.json.JsonObject;

import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;

public interface FrameworkConfigOverrideFactory {

    String getDefaultBuildDir();

    String getDefaultDevScriptName();

    QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson, Optional<String> detectedDevScript,
            boolean isCustomized);
}
