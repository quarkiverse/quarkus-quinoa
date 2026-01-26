package io.quarkiverse.quinoa.deployment.config;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface PackageManagerInstallConfig {

    String NPM_PROVIDED = "provided";
    String DEFAULT_INSTALL_DIR = ".quinoa/";

    /**
     * Enable Package Manager Installation.
     * This will override "package-manager" config.
     * Set "quarkus.quinoa.package-manager-command.prepend-binary=true"
     * when using with custom commands
     */
    @WithParentName
    @WithDefault("false")
    boolean enabled();


    /**
     * The directory (relative to the project root) where Node.js and other binaries should be installed.
     * Node.js will be installed in a {@code node/} subdirectory of this path and also use a {@code cache/} subdirectory.
     * This directory must be dedicated to Quinoa, as it may be wiped and re-created.
     */
    @WithDefault(DEFAULT_INSTALL_DIR)
    String installDir();

    /**
     * The NodeJS Version to install locally to the project.
     * Required when package-manager-install is enabled.
     */
    Optional<String> nodeVersion();

    /**
     * The NPM version to install and use.
     * By default, the version is provided by NodeJS.
     */
    @WithDefault(NPM_PROVIDED)
    @ConfigDocDefault("'provided' means it will use the NPM embedded in NodeJS")
    String npmVersion();

    /**
     * Where to download NPM from.
     */
    @WithDefault("https://registry.npmjs.org/npm/-/")
    String npmDownloadRoot();

    /**
     * Where to download NodeJS from.
     */
    @WithDefault("https://nodejs.org/dist/")
    String nodeDownloadRoot();

    /**
     * Install and use Yarn as package manager with this version.
     * This is ignored if the npm-version is defined.
     */
    Optional<String> yarnVersion();

    /**
     * Where to download YARN from.
     */
    @WithDefault("https://github.com/yarnpkg/yarn/releases/download/")
    String yarnDownloadRoot();

    /**
     * Install and use PNPM as package manager with this version.
     * This is ignored if the npm-version or the yarn-version is defined.
     */
    Optional<String> pnpmVersion();

    /**
     * Where to download PNPM from.
     */
    @WithDefault("https://registry.npmjs.org/pnpm/-/")
    String pnpmDownloadRoot();

    /**
     * Configuration for installing the package manager authenticated
     */
    @WithName("basic-auth")
    PackageManagerInstallAuthConfig packageManagerInstallAuth();

    /**
     * Install and use Bun as package manager with this version.
     */
    Optional<String> bunVersion();

    static boolean isEqual(PackageManagerInstallConfig p1, PackageManagerInstallConfig p2) {
        if (!Objects.equals(p1.enabled(), p2.enabled())) {
            return false;
        }
        if (!Objects.equals(p1.installDir(), p2.installDir())) {
            return false;
        }
        if (!Objects.equals(p1.enabled(), p2.enabled())) {
            return false;
        }
        if (!Objects.equals(p1.nodeVersion(), p2.nodeVersion())) {
            return false;
        }
        if (!Objects.equals(p1.npmVersion(), p2.npmVersion())) {
            return false;
        }
        if (!Objects.equals(p1.npmDownloadRoot(), p2.npmDownloadRoot())) {
            return false;
        }
        if (!Objects.equals(p1.nodeDownloadRoot(), p2.nodeDownloadRoot())) {
            return false;
        }
        if (!Objects.equals(p1.yarnVersion(), p2.yarnVersion())) {
            return false;
        }
        if (!Objects.equals(p1.pnpmVersion(), p2.pnpmVersion())) {
            return false;
        }
        if (!Objects.equals(p1.pnpmDownloadRoot(), p2.pnpmDownloadRoot())) {
            return false;
        }
        if (!PackageManagerInstallAuthConfig.isEqual(p1.packageManagerInstallAuth(), p2.packageManagerInstallAuth())) {
            return false;
        }
        if (!Objects.equals(p1.bunVersion(), p2.bunVersion())) {
            return false;
        }
        return true;
    }
}
