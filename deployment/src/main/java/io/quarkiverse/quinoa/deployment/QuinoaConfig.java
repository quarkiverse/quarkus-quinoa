package io.quarkiverse.quinoa.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class QuinoaConfig {

    /**
     * Path to the frontend root directory.
     * If not set ${project.root}/src/main/ui/ will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root
     */
    @ConfigItem
    public Optional<String> uiPath;

    /**
     * Path of the directory containing the ui built files.
     * If not set build/ will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the ui path
     */
    @ConfigItem
    public Optional<String> buildDirectoryPath;

    /**
     * Name of the ui package manager binary.
     */
    @ConfigItem(defaultValue = "npm")
    public Optional<String> packageManager;
}
