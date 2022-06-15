package io.quarkiverse.quinoa.deployment;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.quinoa.QuinoaHandlerConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class QuinoaConfig {

    private static final String DEFAULT_WEB_UI_DIR = "src/main/webui";
    private static final int DEFAULT_DEV_SERVER_TIMEOUT = 30000;
    private static final String DEFAULT_INDEX_PAGE = "index.html";

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
     * Path of the directory which contains the Web UI built files (generated during the build).
     * After the build, Quinoa will take the files from this directory,
     * move them to target/quinoa-build (or build/quinoa-build with Gradle) and serve them at runtime.
     * The path is relative to the Web UI path
     * If not set "build/" will be used
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
     * Name of the index page.
     * If not set, "index.html" will be used.
     */
    @ConfigItem
    public Optional<String> indexPage;

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
     * Enable SPA (Single Page Application) routing, all relevant requests will be re-routed to the index.html
     * Currently, for technical reasons, the Quinoa SPA routing configuration won't work with RESTEasy Classic.
     * If not set, it is disabled.
     */
    @ConfigItem
    public Optional<Boolean> enableSPARouting;

    /**
     * List of path prefixes to be ignored by Quinoa.
     * If not set, "quarkus.rest.path", "quarkus.resteasy.path" and "quarkus.http.non-application-root-path" will be ignored.
     */
    @ConfigItem
    public Optional<List<String>> ignoredPathPrefixes;

    /**
     * Enable external server for dev (live coding).
     * The dev server process (i.e npm start) is managed like a dev service by Quarkus.
     * This defines the port of the server to forward requests to.
     * If the external server responds with a 404, it is ignored by Quinoa and processed like any other backend request.
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

    public List<String> getNormalizedIgnoredPathPrefixes() {
        return ignoredPathPrefixes.orElseGet(() -> {
            Config config = ConfigProvider.getConfig();
            List<String> defaultIgnored = new ArrayList<>();
            readExternalConfigPath(config, "quarkus.resteasy.path").ifPresent(defaultIgnored::add);
            readExternalConfigPath(config, "quarkus.rest.path").ifPresent(defaultIgnored::add);
            readExternalConfigPath(config, "quarkus.http.non-application-root-path").ifPresent(defaultIgnored::add);
            return defaultIgnored;
        }).stream().map(s -> s.startsWith("/") ? s : "/" + s).collect(toList());
    }

    public String getIndexPage() {
        return indexPage.orElse(DEFAULT_INDEX_PAGE);
    }

    public QuinoaHandlerConfig toHandlerConfig() {
        return new QuinoaHandlerConfig(getNormalizedIgnoredPathPrefixes(), getIndexPage());
    }

    private Optional<String> readExternalConfigPath(Config config, String key) {
        return config.getOptionalValue(key, String.class)
                .filter(s -> !Objects.equals(s, "/"))
                .map(s -> s.endsWith("/") ? s : s + "/");
    }

    public String getUIDir() {
        return uiDir.orElse(DEFAULT_WEB_UI_DIR);
    }

    public String getBuildDir() {
        return buildDir.orElse("build");
    }

    public int getDevServerTimeout() {
        return devServerTimeout.orElse(DEFAULT_DEV_SERVER_TIMEOUT);
    }

    public boolean shouldRunTests() {
        return runTests.orElse(false);
    }

    public boolean shouldEnableDevServerLogs() {
        return enableDevServerLogs.orElse(false);
    }

    public boolean isEnabled() {
        return enable.orElse(true);
    }

    public boolean isSPARoutingEnabled() {
        return enableSPARouting.orElse(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuinoaConfig that = (QuinoaConfig) o;
        return enable.equals(that.enable) && uiDir.equals(that.uiDir) && buildDir.equals(that.buildDir) &&
                packageManager.equals(that.packageManager) && runTests.equals(that.runTests) &&
                frozenLockfile.equals(that.frozenLockfile) && alwaysInstall.equals(that.alwaysInstall) &&
                enableSPARouting.equals(that.enableSPARouting) && ignoredPathPrefixes.equals(that.ignoredPathPrefixes) &&
                devServerPort.equals(that.devServerPort) && devServerTimeout.equals(that.devServerTimeout) &&
                enableDevServerLogs.equals(that.enableDevServerLogs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enable, uiDir, buildDir, packageManager, runTests, frozenLockfile, alwaysInstall,
                enableSPARouting, ignoredPathPrefixes, devServerPort, devServerTimeout, enableDevServerLogs);
    }
}
