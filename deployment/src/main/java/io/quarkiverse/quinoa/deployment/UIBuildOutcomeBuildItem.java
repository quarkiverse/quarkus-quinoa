package io.quarkiverse.quinoa.deployment;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class UIBuildOutcomeBuildItem extends SimpleBuildItem {

    private final Path uiBuildDirectory;

    public UIBuildOutcomeBuildItem(Path uiBuildDirectory) {
        this.uiBuildDirectory = uiBuildDirectory;
    }

    public Path getUiBuildDirectory() {
        return uiBuildDirectory;
    }
}
