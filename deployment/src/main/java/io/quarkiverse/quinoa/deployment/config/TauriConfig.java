package io.quarkiverse.quinoa.deployment.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface TauriConfig {

    String DEFAULT_TAURI_DIR = "src-tauri";
    String DEFAULT_SIDECAR_NAME = "quarkus-backend";

    enum ExportTarget {
        WEB,
        DESKTOP,
        IOS,
        ANDROID
    }

    /**
     * Enable Tauri native app integration.
     * When enabled, Quinoa will build the Web UI and optionally compile a GraalVM native image
     * for packaging as a Tauri sidecar, supporting desktop, iOS, and Android targets.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Path to the Tauri project directory (relative to the project root).
     * This directory should contain the tauri.conf.json file.
     */
    @ConfigDocDefault("auto-detected from src-tauri/tauri.conf.json presence")
    Optional<String> dir();

    /**
     * Target platforms for the Tauri build.
     * Valid values: web, desktop, ios, android.
     * When "web" is included, static web files are built by Quinoa normally.
     * When "desktop", "ios", or "android" are included, the Tauri CLI will be invoked to build native apps.
     */
    @ConfigDocDefault("web,desktop")
    Optional<List<String>> export();

    /**
     * When true, Quinoa will attempt to locate and copy the GraalVM native image
     * as a Tauri sidecar binary before building the Tauri application.
     */
    @WithDefault("true")
    boolean buildNativeImage();

    /**
     * Name of the sidecar binary as registered in Tauri's externalBin configuration.
     * The native image will be copied to src-tauri/binaries/{sidecarName}-{target-triple}.
     */
    @WithDefault(DEFAULT_SIDECAR_NAME)
    String sidecarName();

    /**
     * Path to the GraalVM native image binary.
     * If not set, Quinoa will auto-detect the runner binary in the build output directory.
     */
    @ConfigDocDefault("auto-detected (target/*-runner)")
    Optional<String> nativeImageBinary();

    /**
     * Main class of the Quarkus application used for native image building.
     * If not set, Quarkus will use its default detection.
     */
    @ConfigDocDefault("auto-detected from quarkus.package.main-class")
    Optional<String> mainClass();

    /**
     * When true, Quinoa will manage the Tauri build process automatically.
     * When false, the Tauri CLI must be invoked manually.
     */
    @WithDefault("true")
    boolean managed();

    /**
     * Additional arguments to pass to the Tauri CLI build command.
     */
    @ConfigDocDefault("empty (no additional arguments)")
    Optional<List<String>> buildArgs();

    /**
     * Path to an additional Tauri configuration file to merge during build.
     * This can be used to provide platform-specific overrides.
     */
    @ConfigDocDefault("empty (no additional config)")
    Optional<String> buildConfig();

    /**
     * Enable verbose output from the Tauri CLI.
     */
    @WithDefault("false")
    boolean verbose();

    /**
     * Port for the Tauri dev server in development mode.
     * Set to 0 to let Tauri auto-assign a port.
     */
    @WithDefault("0")
    int devServerPort();

    static boolean isEqual(TauriConfig t1, TauriConfig t2) {
        if (!Objects.equals(t1.enabled(), t2.enabled())) {
            return false;
        }
        if (!Objects.equals(t1.dir(), t2.dir())) {
            return false;
        }
        if (!Objects.equals(t1.export(), t2.export())) {
            return false;
        }
        if (!Objects.equals(t1.buildNativeImage(), t2.buildNativeImage())) {
            return false;
        }
        if (!Objects.equals(t1.sidecarName(), t2.sidecarName())) {
            return false;
        }
        if (!Objects.equals(t1.nativeImageBinary(), t2.nativeImageBinary())) {
            return false;
        }
        if (!Objects.equals(t1.mainClass(), t2.mainClass())) {
            return false;
        }
        if (!Objects.equals(t1.managed(), t2.managed())) {
            return false;
        }
        if (!Objects.equals(t1.buildArgs(), t2.buildArgs())) {
            return false;
        }
        if (!Objects.equals(t1.buildConfig(), t2.buildConfig())) {
            return false;
        }
        if (!Objects.equals(t1.verbose(), t2.verbose())) {
            return false;
        }
        if (!Objects.equals(t1.devServerPort(), t2.devServerPort())) {
            return false;
        }
        return true;
    }
}
