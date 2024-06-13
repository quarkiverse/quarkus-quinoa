package io.quarkiverse.quinoa.deployment.items;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BuiltResourcesBuildItem extends SimpleBuildItem {

    private final Optional<Path> directory;
    private final Set<BuiltResource> resources;

    public BuiltResourcesBuildItem(Path directory, Set<BuiltResource> resources) {
        this.directory = Optional.ofNullable(directory);
        this.resources = resources;
    }

    public BuiltResourcesBuildItem(Set<BuiltResource> entries) {
        this.directory = Optional.empty();
        this.resources = entries;
    }

    public Optional<Path> getDirectory() {
        return directory;
    }

    public Set<BuiltResource> resources() {
        return resources;
    }

    public record BuiltResource(String name, byte[] content) {
    }
}
