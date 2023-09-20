package io.quarkiverse.quinoa.deployment.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
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
     * The directory where NodeJS should be installed (relative to the project root),
     * It will be installed in a 'node/' subdirectory of this.
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

}
