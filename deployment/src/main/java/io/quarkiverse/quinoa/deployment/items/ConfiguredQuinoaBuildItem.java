package io.quarkiverse.quinoa.deployment.items;

import java.nio.file.Path;

import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner;
import io.quarkus.builder.item.SimpleBuildItem;

public final class ConfiguredQuinoaBuildItem extends SimpleBuildItem {

    private final PackageManagerRunner packageManagerRunner;

    private final QuinoaConfig resolvedConfig;

    public ConfiguredQuinoaBuildItem(PackageManagerRunner packageManagerRunner, QuinoaConfig resolvedConfig) {
        this.packageManagerRunner = packageManagerRunner;
        this.resolvedConfig = resolvedConfig;
    }

    public PackageManagerRunner getPackageManager() {
        return packageManagerRunner;
    }

    public Path getDirectory() {
        return getPackageManager().getDirectory();
    }

    public QuinoaConfig resolvedConfig() {
        return resolvedConfig;
    }

}
