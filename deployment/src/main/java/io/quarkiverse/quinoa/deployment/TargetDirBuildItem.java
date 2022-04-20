package io.quarkiverse.quinoa.deployment;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class TargetDirBuildItem extends SimpleBuildItem {

    private final Path buildDirectory;

    public TargetDirBuildItem(Path buildDirectory) {
        this.buildDirectory = buildDirectory;
    }

    public Path getBuildDirectory() {
        return buildDirectory;
    }
}
