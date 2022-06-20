package io.quarkiverse.quinoa.deployment;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.quinoa.QuinoaHandlerConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class QuinoaConfig {

    private static final String DEFAULT_WEB_UI_DIR = "src/main/webui";
    private static final String DEFAULT_INDEX_PAGE = "index.html";

    /**
     * Indicate if the extension should be enabled.
     * Default is true if the Web UI directory exists and dev and prod mode.
     * Default is false in test mode (to avoid building the Web UI during backend tests).
     */
    @ConfigItem(name = ConfigItem.PARENT)
    Optional<Boolean> enable;

    /**
     * Path to the Web UI (node) root directory.
     * If not set ${project.root}/src/main/webui/ will be used.
     * otherwise the path will be considered relative to the project root.
     */
    @ConfigItem(defaultValueDocumentation = "src/main/webui/")
    public Optional<String> uiDir;

    /**
     * Path of the directory which contains the Web UI built files (generated during the build).
     * After the build, Quinoa will take the files from this directory,
     * move them to target/quinoa-build (or build/quinoa-build with Gradle) and serve them at runtime.
     * The path is relative to the Web UI path.
     * If not set "build/" will be used
     */
    @ConfigItem(defaultValueDocumentation = "build/")
    public Optional<String> buildDir;

    /**
     * Name of the package manager binary.
     * If not set, it will be auto-detected depending on the lockfile falling back to "npm".
     * Only npm, pnpm and yarn are supported for the moment.
     */
    @ConfigItem
    public Optional<String> packageManager;

    /**
     * Name of the index page.
     * If not set, "index.html" will be used.
     */
    @ConfigItem(defaultValueDocumentation = "index.html")
    public Optional<String> indexPage;

    /**
     * Indicate if the Web UI should also be tested during the build phase (i.e: npm test).
     * To be used in a {@link io.quarkus.test.junit.QuarkusTestProfile} to have Web UI test running during a
     * {@link io.quarkus.test.junit.QuarkusTest}
     * Default is false.
     */
    @ConfigItem(name = "run-tests", defaultValueDocumentation = "false")
    Optional<Boolean> runTests;

    /**
     * Install the packages using a frozen lockfile. Don’t generate a lockfile and fail if an update is needed (useful in CI).
     * If not set it is true if environment CI=true, else it is false.
     */
    @ConfigItem
    public Optional<Boolean> frozenLockfile;

    /**
     * Force install packages before building.
     * If not set, it will install packages only if the node_modules directory is absent.
     */
    @ConfigItem(defaultValueDocumentation = "false")
    public Optional<Boolean> forceInstall;

    /**
     * Enable SPA (Single Page Application) routing, all relevant requests will be re-routed to the "index.html".
     * Currently, for technical reasons, the Quinoa SPA routing configuration won't work with RESTEasy Classic.
     * If not set, it is disabled.
     */
    @ConfigItem(defaultValueDocumentation = "false")
    public Optional<Boolean> enableSPARouting;

    /**
     * List of path prefixes to be ignored by Quinoa.
     * If not set, "quarkus.rest.path", "quarkus.resteasy.path" and "quarkus.http.non-application-root-path" will be ignored.
     */
    @ConfigItem
    public Optional<List<String>> ignoredPathPrefixes;

    /**
     * Configuration for the external dev server (live coding server)
     */
    @ConfigItem
    public DevServerConfig devServer;

    public List<String> getNormalizedIgnoredPathPrefixes() {
        return ignoredPathPrefixes.orElseGet(() -> {
            Config config = ConfigProvider.getConfig();
            List<String> defaultIgnore = new ArrayList<>();
            readExternalConfigPath(config, "quarkus.resteasy.path").ifPresent(defaultIgnore::add);
            readExternalConfigPath(config, "quarkus.rest.path").ifPresent(defaultIgnore::add);
            readExternalConfigPath(config, "quarkus.http.non-application-root-path").ifPresent(defaultIgnore::add);
            return defaultIgnore;
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
        return buildDir.orElse("build/");
    }

    public boolean shouldRunTests() {
        return runTests.orElse(false);
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
        return Objects.equals(enable, that.enable) && Objects.equals(uiDir, that.uiDir)
                && Objects.equals(buildDir, that.buildDir) && Objects.equals(packageManager, that.packageManager)
                && Objects.equals(indexPage, that.indexPage) && Objects.equals(runTests, that.runTests)
                && Objects.equals(frozenLockfile, that.frozenLockfile) && Objects.equals(forceInstall, that.forceInstall)
                && Objects.equals(enableSPARouting, that.enableSPARouting)
                && Objects.equals(ignoredPathPrefixes, that.ignoredPathPrefixes)
                && Objects.equals(devServer, that.devServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enable, uiDir, buildDir, packageManager, indexPage, runTests, frozenLockfile, forceInstall,
                enableSPARouting, ignoredPathPrefixes, devServer);
    }
}
