package io.quarkiverse.quinoa.deployment.config;

import io.smallrye.config.WithDefault;

public interface FrameworkConfig {

    /**
     * When true, the UI Framework will be auto-detected if possible
     */
    @WithDefault("true")
    boolean detection();
}
