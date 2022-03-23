package io.quarkiverse.quinoa.deployment;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SrcUIDirBuildItem extends SimpleBuildItem {

    private final Path directory;

    public SrcUIDirBuildItem(Path directory) {
        this.directory = directory;
    }

    public Path getDirectory() {
        return directory;
    }
}
