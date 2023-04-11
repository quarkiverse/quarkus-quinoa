package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PackageManagerInstallConfig {

    public static final String NPM_PROVIDED = "provided";
    private static final String DEFAULT_INSTALL_DIR = ".quinoa/";

    /**
     * Enable Package Manager Installation.
     * This will override "package-manager" config.
     * Set "quarkus.quinoa.package-manager-command.prepend-binary=true"
     * when using with custom commands
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "false")
    public boolean enabled;

    /**
     * The directory where NodeJS should be installed,
     * it will be installed in a node/ sub-directory.
     * Default is ${project.root}/.quinoa
     */
    @ConfigItem(defaultValue = DEFAULT_INSTALL_DIR)
    public String installDir;

    /**
     * The NodeJS Version to install locally to the project.
     * Required when package-manager-install is enabled.
     */
    @ConfigItem
    public Optional<String> nodeVersion;

    /**
     * Where to download NodeJS from.
     */
    @ConfigItem(defaultValue = "https://nodejs.org/dist/")
    public String nodeDownloadRoot;

    /**
     * The NPM version to install.
     * By default, the version is provided by NodeJS.
     */
    @ConfigItem(defaultValue = NPM_PROVIDED)
    public String npmVersion;

    /**
     * Where to download NPM from.
     */
    @ConfigItem(defaultValue = "https://registry.npmjs.org/npm/-/")
    public String npmDownloadRoot;

    /**
     * The PNPM version to install.
     * If the version is set and NPM and YARN are not set, then this version will attempt to be downloaded.
     */
    @ConfigItem
    public Optional<String> pnpmVersion;

    /**
     * Where to download PNPM from.
     */
    @ConfigItem(defaultValue = "https://registry.npmjs.org/pnpm/-/")
    public String pnpmDownloadRoot;

    /**
     * The YARN version to install.
     * If the version is set and NPM Version is not set, then this version will attempt to be downloaded.
     */
    @ConfigItem
    public Optional<String> yarnVersion;

    /**
     * Where to download YARN from.
     */
    @ConfigItem(defaultValue = "https://github.com/yarnpkg/yarn/releases/download/")
    public String yarnDownloadRoot;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PackageManagerInstallConfig that = (PackageManagerInstallConfig) o;
        return enabled == that.enabled && Objects.equals(nodeVersion, that.nodeVersion)
                && Objects.equals(nodeDownloadRoot, that.nodeDownloadRoot) && Objects.equals(npmVersion, that.npmVersion)
                && Objects.equals(npmDownloadRoot, that.npmDownloadRoot) && Objects.equals(pnpmVersion, that.pnpmVersion)
                && Objects.equals(pnpmDownloadRoot, that.pnpmDownloadRoot) && Objects.equals(yarnVersion, that.yarnVersion)
                && Objects.equals(yarnDownloadRoot, that.yarnDownloadRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, nodeVersion, nodeDownloadRoot, npmVersion, npmDownloadRoot, pnpmVersion, pnpmDownloadRoot,
                yarnVersion, yarnDownloadRoot);
    }
}