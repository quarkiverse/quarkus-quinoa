package io.quarkiverse.quinoa.deployment;

import java.nio.file.Path;
import java.util.OptionalInt;

import io.quarkiverse.quinoa.deployment.packagemanager.PackageManager;
import io.quarkus.builder.item.SimpleBuildItem;

public final class QuinoaDirectoryBuildItem extends SimpleBuildItem {

    private final PackageManager packageManager;

    /**
     * Port of the server to forward requests to.
     * The dev server process (i.e npm start) is managed like a dev service by Quarkus.
     * If the external server responds with a 404, it is ignored by Quinoa and processed like any other backend request.
     */
    private OptionalInt devServerPort;

    /**
     * This the Web UI internal build system (webpack, ...) output directory.
     * After the build, Quinoa will take the files from this directory,
     * move them to 'target/quinoa-build' (or build/quinoa-build with Gradle) and serve them at runtime.
     * The path is relative to the Web UI path.
     * If not set "build/" will be used
     */
    private final String buildDirectory;

    public QuinoaDirectoryBuildItem(PackageManager packageManager, OptionalInt devServerPort, String buildDirectory) {
        this.packageManager = packageManager;
        this.devServerPort = devServerPort;
        this.buildDirectory = buildDirectory;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public Path getDirectory() {
        return getPackageManager().getDirectory();
    }

    public OptionalInt getDevServerPort() {
        return devServerPort;
    }

    public String getBuildDirectory() {
        return buildDirectory;
    }

    public boolean isDevServerMode(DevServerConfig devServerConfig) {
        return devServerConfig.enabled && devServerPort.isPresent();
    }
}