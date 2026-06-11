package io.quarkiverse.quinoa.deployment.config;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configuration for SBOM (Software Bill of Materials) generation.
 */
@ConfigGroup
public interface SbomConfig {

    /**
     * Whether SBOM generation is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether to include development dependencies in the SBOM.
     * If not set, dev dependencies are included in dev mode and excluded in production.
     */
    Optional<Boolean> includeDevDependencies();

    /**
     * The cdxgen version to use. If not set, the latest version is used.
     */
    Optional<String> cdxgenVersion();

    /**
     * Timeout in seconds for the cdxgen process.
     */
    @WithDefault("300")
    int timeout();

    static boolean isEqual(SbomConfig s1, SbomConfig s2) {
        if (!Objects.equals(s1.enabled(), s2.enabled())) {
            return false;
        }
        if (!Objects.equals(s1.includeDevDependencies(), s2.includeDevDependencies())) {
            return false;
        }
        if (!Objects.equals(s1.cdxgenVersion(), s2.cdxgenVersion())) {
            return false;
        }
        if (s1.timeout() != s2.timeout()) {
            return false;
        }
        return true;
    }
}
