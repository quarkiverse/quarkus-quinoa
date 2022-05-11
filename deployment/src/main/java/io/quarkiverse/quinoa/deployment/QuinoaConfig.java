package io.quarkiverse.quinoa.deployment;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class QuinoaConfig {

    /**
     * Indicate if the extension should be enabled
     * Default is true if the Web UI directory exists and dev and prod mode
     * Default is false in test mode (to avoid building the Web UI during backend tests)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    Optional<Boolean> enable;

    /**
     * Path to the Web UI (node) root directory.
     * If not set ${project.root}/src/main/webui/ will be used
     * otherwise the path will be considered relative to the project root
     */
    @ConfigItem
    public Optional<String> uiDir;

    /**
     * Path of the directory which contains the resulting Web UI built files.
     * If not set build/ will be used
     * otherwise the path will be considered relative to the Web UI path
     */
    @ConfigItem
    public Optional<String> buildDir;

    /**
     * Name of the package manager binary.
     * If not set, it will be auto-detected depending on the lockfile falling back to "npm".
     * Only npm, pnpm and yarn are supported for the moment
     */
    @ConfigItem
    public Optional<String> packageManager;

    /**
     * Indicate if the Web UI should also be tested during the build phase (i.e: npm test)
     * To be used in a {@link io.quarkus.test.junit.QuarkusTestProfile} to have Web UI test running during a
     * {@link io.quarkus.test.junit.QuarkusTest}
     * Default is false
     */
    @ConfigItem(name = "run-tests")
    Optional<Boolean> runTests;

    /**
     * Install the packages using a frozen lockfile. Don’t generate a lockfile and fail if an update is needed (useful in CI).
     * If not set it is true if environment CI=true, else it is false
     */
    @ConfigItem
    public Optional<Boolean> frozenLockfile;

    /**
     * Always install packages before building.
     * If not set, it will install packages only if the node_modules directory is absent.
     */
    @ConfigItem
    public Optional<Boolean> alwaysInstall;

    /**
     * Enable SPA (Single Page Application) routing, all unhandled requests will be re-routed to the index.html
     * If not set, it is disabled.
     */
    @ConfigItem
    public Optional<Boolean> enableSPARouting;

    /**
     * Enable using an external server for dev (live coding).
     * The dev server process (i.e npm start) is managed like a dev service by Quarkus.
     * This defines the port of the server to forward requests to.
     */
    @ConfigItem
    public OptionalInt devServerPort;

    /**
     * Timeout in ms for the dev server to be up and running.
     * If not set the default is ~30000ms
     */
    @ConfigItem
    public OptionalInt devServerTimeout;

    /**
     * Enable external dev server live coding logs.
     * This is not enabled by default because most dev servers display compilation errors directly in the browser.
     * False if not set.
     */
    @ConfigItem
    public Optional<Boolean> enableDevServerLogs;

}
