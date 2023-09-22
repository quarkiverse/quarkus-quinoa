package io.quarkiverse.quinoa.deployment.items;

import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner;
import io.quarkus.builder.item.SimpleBuildItem;

public final class InstalledPackageManagerBuildItem extends SimpleBuildItem {

    private final PackageManagerRunner packageManagerRunner;

    public InstalledPackageManagerBuildItem(PackageManagerRunner packageManagerRunner) {
        this.packageManagerRunner = packageManagerRunner;
    }

    public PackageManagerRunner getPackageManager() {
        return packageManagerRunner;
    }
}
