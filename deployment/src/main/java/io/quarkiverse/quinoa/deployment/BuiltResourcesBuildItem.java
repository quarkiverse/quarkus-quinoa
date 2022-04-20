package io.quarkiverse.quinoa.deployment;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
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

    public Set<String> getNames() {
        Set<String> names = new HashSet<>(resources.size());
        for (BuiltResource entry : resources) {
            names.add(entry.getName());
        }
        return names;
    }

    public static class BuiltResource {
        private final String name;

        public BuiltResource(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            BuiltResource entry = (BuiltResource) o;
            return name.equals(entry.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
