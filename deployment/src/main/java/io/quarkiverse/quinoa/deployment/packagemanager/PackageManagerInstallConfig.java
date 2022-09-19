package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PackageManagerInstallConfig {

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
     * Default is ${project.root}
     */
    @ConfigItem
    public Optional<String> installDir;

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
    @ConfigItem(defaultValue = "provided")
    public String npmVersion;

    /**
     * Where to download NPM from.
     */
    @ConfigItem(defaultValue = "https://registry.npmjs.org/npm/-/")
    public String npmDownloadRoot;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PackageManagerInstallConfig that = (PackageManagerInstallConfig) o;
        return enabled == that.enabled && Objects.equals(nodeVersion, that.nodeVersion)
                && Objects.equals(nodeDownloadRoot, that.nodeDownloadRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, nodeVersion, nodeDownloadRoot);
    }
}
