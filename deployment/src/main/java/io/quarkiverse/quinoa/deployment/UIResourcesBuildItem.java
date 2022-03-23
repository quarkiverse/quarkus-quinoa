package io.quarkiverse.quinoa.deployment;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class UIResourcesBuildItem extends SimpleBuildItem {

    private final Optional<Path> directory;
    private final Set<UIEntry> entries;

    public UIResourcesBuildItem(Path directory, Set<UIEntry> entries) {
        this.directory = Optional.ofNullable(directory);
        this.entries = entries;
    }

    public UIResourcesBuildItem(Set<UIEntry> entries) {
        this.directory = Optional.empty();
        this.entries = entries;
    }

    public Optional<Path> getDirectory() {
        return directory;
    }

    public Set<String> getNames() {
        Set<String> names = new HashSet<>(entries.size());
        for (UIEntry entry : entries) {
            names.add(entry.getName());
        }
        return names;
    }

    public static class UIEntry {
        private final String name;

        public UIEntry(String name) {
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
            UIEntry entry = (UIEntry) o;
            return name.equals(entry.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
