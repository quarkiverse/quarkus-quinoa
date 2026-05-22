package io.quarkiverse.quinoa.deployment.framework.override;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import jakarta.json.JsonObject;

import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.TauriConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.TauriConfigDelegate;

public class TauriFramework extends GenericFramework {

    private static final String TAURI_DEV_SCRIPT = "tauri";
    static final String TAURI_DIR = "src-tauri";

    public TauriFramework() {
        super("dist", "dev", 1420);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized, Path uiDir) {
        QuinoaConfig baseConfig = super.override(delegate, packageJson, detectedDevScript, isCustomized, uiDir);

        return new QuinoaConfigDelegate(baseConfig) {
            @Override
            public TauriConfig tauri() {
                return new TauriConfigDelegate(super.tauri()) {
                    @Override
                    public Optional<String> dir() {
                        return Optional.of(super.dir().orElse(autoDetectTauriDir(uiDir)));
                    }
                };
            }

            @Override
            public boolean justBuild() {
                return true;
            }
        };
    }

    static String autoDetectTauriDir(Path uiDir) {
        Path projectRoot = findProjectRoot(uiDir);
        if (projectRoot != null && Files.isDirectory(projectRoot.resolve(TAURI_DIR))) {
            return TAURI_DIR;
        }
        if (Files.isDirectory(uiDir.resolve(TAURI_DIR))) {
            return TAURI_DIR;
        }
        return TAURI_DIR;
    }

    private static Path findProjectRoot(Path start) {
        Path current = start;
        while (current != null && current.getParent() != null) {
            if (Files.exists(current.resolve("pom.xml"))
                    || Files.exists(current.resolve("build.gradle"))
                    || Files.exists(current.resolve("build.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}
