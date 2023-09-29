package io.quarkiverse.quinoa.deployment.config;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.quinoa.QuinoaHandlerConfig;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.quinoa")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface QuinoaConfig {

    String DEFAULT_BUILD_DIR = "build/";
    String DEFAULT_WEB_UI_DIR = "src/main/webui";
    String DEFAULT_INDEX_PAGE = "index.html";

    /**
     * Indicate if the extension should be enabled.
     */
    @WithParentName
    @ConfigDocDefault("enabled (disabled in test mode)")
    public Optional<Boolean> enabled();

    /**
     * Indicate if Quinoa should just do the build part.
     * If true, Quinoa will NOT serve the Web UI built resources.
     * This is handy when the output of the build is used
     * to be served via something else (nginx, cdn, ...)
     * Quinoa put the built files in 'target/quinoa/build' (or 'build/quinoa/build with Gradle).
     */
    @WithDefault("false")
    boolean justBuild();

    /**
     * Path to the Web UI (NodeJS) root directory (relative to the project root).
     */
    @WithDefault(DEFAULT_WEB_UI_DIR)
    String uiDir();

    /**
     * This the Web UI internal build system (webpack, ...) output directory.
     * After the build, Quinoa will take the files from this directory,
     * move them to 'target/quinoa/build' (or build/quinoa/build with Gradle) and serve them at runtime.
     * The path is relative to the Web UI path.
     */
    @ConfigDocDefault("framework detection with fallback to '" + DEFAULT_BUILD_DIR + "'")
    Optional<String> buildDir();

    /**
     * Name of the package manager binary.
     * Only npm, pnpm and yarn are supported for the moment.
     */
    @ConfigDocDefault("auto-detected based on lockfile falling back to 'npm'")
    Optional<String> packageManager();

    /**
     * Configuration for installing the package manager
     */
    PackageManagerInstallConfig packageManagerInstall();

    /**
     * Configuration for overriding build commands
     */
    PackageManagerCommandConfig packageManagerCommand();

    /**
     * Name of the index page.
     */
    @WithDefault(DEFAULT_INDEX_PAGE)
    String indexPage();

    /**
     * Indicate if the Web UI should also be tested during the build phase (i.e: npm test).
     * To be used in a {@link io.quarkus.test.junit.QuarkusTestProfile} to have Web UI test running during a
     * {@link io.quarkus.test.junit.QuarkusTest}
     */
    @SuppressWarnings("JavadocReference")
    @WithDefault("false")
    boolean runTests();

    /**
     * Install the packages without generating a lockfile (frozen lockfile) and failing if an update is needed (useful in CI).
     */
    @ConfigDocDefault("true if environment CI=true")
    Optional<Boolean> ci();

    /**
     * Force install packages before building.
     * It will install packages only if the node_modules directory is absent or when the package.json is modified in dev-mode.
     */
    @WithDefault("false")
    boolean forceInstall();

    /**
     * Configure framework detection
     */
    FrameworkConfig framework();

    /**
     * Enable SPA (Single Page Application) routing, all relevant requests will be re-routed to the index page.
     * Currently, for technical reasons, the Quinoa SPA routing configuration won't work with RESTEasy Classic.
     */
    @WithDefault("false")
    @WithName("enable-spa-routing")
    boolean enableSPARouting();

    /**
     * List of path prefixes to be ignored by Quinoa.
     */
    @ConfigDocDefault("ignore values configured by 'quarkus.resteasy-reactive.path', 'quarkus.resteasy.path' and 'quarkus.http.non-application-root-path'")
    Optional<List<String>> ignoredPathPrefixes();

    /**
     * Configuration for the external dev server (live coding server)
     */
    DevServerConfig devServer();

    static List<String> getNormalizedIgnoredPathPrefixes(QuinoaConfig config) {
        return config.ignoredPathPrefixes().orElseGet(() -> {
            Config allConfig = ConfigProvider.getConfig();
            List<String> defaultIgnore = new ArrayList<>();
            readExternalConfigPath(allConfig, "quarkus.resteasy.path").ifPresent(defaultIgnore::add);
            readExternalConfigPath(allConfig, "quarkus.resteasy-reactive.path").ifPresent(defaultIgnore::add);
            readExternalConfigPath(allConfig, "quarkus.http.non-application-root-path").ifPresent(defaultIgnore::add);
            return defaultIgnore;
        }).stream().map(s -> s.startsWith("/") ? s : "/" + s).collect(toList());
    }

    static QuinoaHandlerConfig toHandlerConfig(QuinoaConfig config, boolean prodMode,
            final HttpBuildTimeConfig httpBuildTimeConfig) {
        final Set<String> compressMediaTypes = httpBuildTimeConfig.compressMediaTypes.map(Set::copyOf).orElse(Set.of());
        final String indexPage = !isDevServerMode(config) ? config.indexPage()
                : config.devServer().indexPage().orElse(config.indexPage());
        return new QuinoaHandlerConfig(getNormalizedIgnoredPathPrefixes(config), indexPage, prodMode,
                httpBuildTimeConfig.enableCompression, compressMediaTypes);
    }

    private static Optional<String> readExternalConfigPath(Config config, String key) {
        return config.getOptionalValue(key, String.class)
                .filter(s -> !Objects.equals(s, "/"))
                .map(s -> s.endsWith("/") ? s : s + "/");
    }

    static boolean isDevServerMode(QuinoaConfig config) {
        return config.devServer().enabled() && config.devServer().port().isPresent();
    }

    static boolean isEnabled(QuinoaConfig config) {
        return config.enabled().orElse(true);
    }

}
