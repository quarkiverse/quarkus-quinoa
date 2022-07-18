package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Map;
import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PackageManagerCommandsConfig that = (PackageManagerCommandsConfig) o;
        return Objects.equals(install, that.install) && Objects.equals(installEnv, that.installEnv)
                && Objects.equals(build, that.build) && Objects.equals(buildEnv, that.buildEnv)
                && Objects.equals(test, that.test) && Objects.equals(testEnv, that.testEnv) && Objects.equals(dev, that.dev)
                && Objects.equals(devEnv, that.devEnv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(install, installEnv, build, buildEnv, test, testEnv, dev, devEnv);
    }
}
