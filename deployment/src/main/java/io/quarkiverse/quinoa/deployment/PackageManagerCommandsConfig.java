package io.quarkiverse.quinoa.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.smallrye.config.ConfigMapping;

@ConfigGroup
public class PackageManagerCommandsConfig {
    /**
     * Custom command for installing all dependencies.
     * e.g. «npm ci --cache $CACHE_DIR/.npm --prefer-offline»
     */
    @ConfigItem
    Optional<String> install;

    /**
     * Environment variables for install command execution.
     */
    @ConfigMapping
    Map<String, String> installEnv;

    /**
     * Custom command for building the application.
     */
    @ConfigItem
    Optional<String> build;

    /**
     * Environment variables for build command execution.
     */
    @ConfigMapping
    Map<String, String> buildEnv;

    /**
     * Custom command for running tests for the application.
     */
    @ConfigItem
    Optional<String> test;

    /**
     * Environment variables for test command execution.
     */
    @ConfigMapping
    Map<String, String> testEnv;

    /**
     * Custom command for starting the application in development mode.
     */
    @ConfigItem
    Optional<String> dev;

    /**
     * Environment variables for development command execution.
     */
    @ConfigMapping
    Map<String, String> devEnv;
}
