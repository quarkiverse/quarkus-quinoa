package io.quarkiverse.quinoa.deployment.items;

import java.nio.file.Path;
import java.util.List;

import io.quarkiverse.quinoa.deployment.config.TauriConfig;
import io.quarkus.builder.item.SimpleBuildItem;

public final class TauriBuildItem extends SimpleBuildItem {

    private final Path tauriDir;
    private final Path projectDir;
    private final Path uiDir;
    private final List<TauriConfig.ExportTarget> exportTargets;
    private final TauriConfig tauriConfig;

    public TauriBuildItem(Path tauriDir, Path projectDir, Path uiDir,
            List<TauriConfig.ExportTarget> exportTargets, TauriConfig tauriConfig) {
        this.tauriDir = tauriDir;
        this.projectDir = projectDir;
        this.uiDir = uiDir;
        this.exportTargets = exportTargets;
        this.tauriConfig = tauriConfig;
    }

    public Path tauriDir() {
        return tauriDir;
    }

    public Path projectDir() {
        return projectDir;
    }

    public Path uiDir() {
        return uiDir;
    }

    public List<TauriConfig.ExportTarget> exportTargets() {
        return exportTargets;
    }

    public TauriConfig tauriConfig() {
        return tauriConfig;
    }
}
