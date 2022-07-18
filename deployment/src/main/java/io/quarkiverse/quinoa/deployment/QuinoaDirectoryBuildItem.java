package io.quarkiverse.quinoa.deployment;

import java.nio.file.Path;

import io.quarkiverse.quinoa.deployment.packagemanager.PackageManager;
import io.quarkus.builder.item.SimpleBuildItem;

public final class QuinoaDirectoryBuildItem extends SimpleBuildItem {

    private final PackageManager packageManager;

    public QuinoaDirectoryBuildItem(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public Path getDirectory() {
        return getPackageManager().getDirectory();
    }
}
