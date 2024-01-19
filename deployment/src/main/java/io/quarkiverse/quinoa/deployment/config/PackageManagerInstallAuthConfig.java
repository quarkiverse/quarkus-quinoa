package io.quarkiverse.quinoa.deployment.config;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface PackageManagerInstallAuthConfig {

    /**
     * The basic authentication username to use for node download.
     */
    Optional<String> username();

    /**
     * The basic authentication password to use for node download.
     */
    Optional<String> password();

    static boolean isEqual(PackageManagerInstallAuthConfig p1, PackageManagerInstallAuthConfig p2) {
        if (!Objects.equals(p1.username(), p2.username())) {
            return false;
        }
        if (!Objects.equals(p1.password(), p2.password())) {
            return false;
        }
        return true;
    }

}
