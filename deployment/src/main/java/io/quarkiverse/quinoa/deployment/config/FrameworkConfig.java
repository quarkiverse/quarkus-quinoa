package io.quarkiverse.quinoa.deployment.config;

import java.util.Objects;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface FrameworkConfig {

    static boolean isEqual(FrameworkConfig f1, FrameworkConfig f2) {
        if (!Objects.equals(f1.detection(), f2.detection())) {
            return false;
        }
        return true;
    }

    /**
     * When true, the UI Framework will be auto-detected if possible
     */
    @WithDefault("true")
    boolean detection();
}
