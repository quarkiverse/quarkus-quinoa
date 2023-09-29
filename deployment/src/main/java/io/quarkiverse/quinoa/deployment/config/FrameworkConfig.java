package io.quarkiverse.quinoa.deployment.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface FrameworkConfig {

    /**
     * When true, the UI Framework will be auto-detected if possible
     */
    @WithDefault("true")
    boolean detection();
}
