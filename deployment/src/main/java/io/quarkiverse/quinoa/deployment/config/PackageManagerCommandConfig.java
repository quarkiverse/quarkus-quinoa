package io.quarkiverse.quinoa.deployment.config;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface PackageManagerCommandConfig {

    String DEFAULT_DEV_SCRIPT_NAME = "start";
    String DEFAULT_DEV_COMMAND = "run " + DEFAULT_DEV_SCRIPT_NAME;
    String DEFAULT_INSTALL_COMMAND = "install";
    String DEFAULT_BUILD_COMMAND = "run build";
    String DEFAULT_TEST_COMMAND = "run test";

    /**
     * Custom command for installing all packages.
     * e.g. «ci --cache $CACHE_DIR/.npm --prefer-offline»
     */
    @ConfigDocDefault(DEFAULT_INSTALL_COMMAND)
    Optional<String> install();

    /**
     * Environment variables for install command execution.
     */
    Map<String, String> installEnv();

    /**
     * Custom command for installing all the packages without generating a lockfile (frozen lockfile)
     * and failing if an update is needed (useful in CI).
     */
    @ConfigDocDefault("Detected based on package manager")
    Optional<String> ci();

    /**
     * Environment variables for ci command execution.
     */
    Map<String, String> ciEnv();

    /**
     * Custom command for building the application.
     */
    @WithDefault(DEFAULT_BUILD_COMMAND)
    Optional<String> build();

    /**
     * Environment variables for build command execution.
     */
    Map<String, String> buildEnv();

    /**
     * Custom command for running tests for the application.
     */
    @ConfigDocDefault(DEFAULT_TEST_COMMAND)
    Optional<String> test();

    /**
     * Environment variables for test command execution.
     */
    @ConfigDocDefault("CI=true")
    Map<String, String> testEnv();

    /**
     * Custom command for starting the application in development mode.
     */
    @ConfigDocDefault("framework detection with fallback to '" + DEFAULT_DEV_SCRIPT_NAME + "'")
    Optional<String> dev();

    /**
     * Environment variables for development command execution.
     */
    Map<String, String> devEnv();

}
