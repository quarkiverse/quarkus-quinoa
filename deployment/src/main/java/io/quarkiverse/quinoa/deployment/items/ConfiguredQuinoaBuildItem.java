package io.quarkiverse.quinoa.deployment.items;

import java.nio.file.Path;

import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkus.builder.item.SimpleBuildItem;

public final class ConfiguredQuinoaBuildItem extends SimpleBuildItem {

    private final Path projectDir;
    private final Path uiDir;
    private final Path packageJson;
    private final QuinoaConfig resolvedConfig;

    public ConfiguredQuinoaBuildItem(Path projectDir, Path uiDir, Path packageJson, QuinoaConfig resolvedConfig) {
        this.projectDir = projectDir;
        this.uiDir = uiDir;
        this.packageJson = packageJson;
        this.resolvedConfig = resolvedConfig;
    }

    public Path projectDir() {
        return projectDir;
    }

    public Path uiDir() {
        return uiDir;
    }

    public Path packageJson() {
        return packageJson;
    }

    public QuinoaConfig resolvedConfig() {
        return resolvedConfig;
    }

}
