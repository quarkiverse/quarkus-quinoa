package io.quarkiverse.quinoa.deployment.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.quinoa.QuinoaHandlerConfig;
import io.quarkus.deployment.util.UriNormalizationUtil;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.quinoa")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface QuinoaConfig {

    String DEFAULT_BUILD_DIR = "build/";
    String DEFAULT_WEB_UI_ROOT_PATH = "/";
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
     * Root path for hosting the Web UI.
     * This path is normalized and always resolved relative to 'quarkus.http.root-path'.
     */
    @WithDefault(DEFAULT_WEB_UI_ROOT_PATH)
    String uiRootPath();

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
     * The paths are normalized and always resolved relative to 'quarkus.quinoa.ui-root-path'.
     */
    @ConfigDocDefault("ignore values configured by 'quarkus.resteasy-reactive.path', 'quarkus.resteasy.path' and 'quarkus.http.non-application-root-path'")
    Optional<List<String>> ignoredPathPrefixes();

    /**
     * Configuration for the external dev server (live coding server)
     */
    DevServerConfig devServer();

    static List<String> getNormalizedIgnoredPathPrefixes(QuinoaConfig config,
            NonApplicationRootPathBuildItem nonApplicationRootPath) {
        return config.ignoredPathPrefixes()
                .map(list -> list.stream()
                        .map(s -> normalizePath(s, false))
                        .collect(Collectors.toList()))
                .orElseGet(() -> {
                    Config allConfig = ConfigProvider.getConfig();
                    List<String> defaultIgnore = new ArrayList<>();
                    String uiRootPath = getNormalizedUiRootPath(config);
                    // note that quarkus.resteasy.path and quarkus.resteasy-reactive.path are always relative to the http root path
                    readExternalConfigPath(uiRootPath, allConfig, "quarkus.resteasy.path").ifPresent(defaultIgnore::add);
                    readExternalConfigPath(uiRootPath, allConfig, "quarkus.resteasy-reactive.path")
                            .ifPresent(defaultIgnore::add);
                    // the non-application root path is not always relative to the http root path
                    convertNonApplicationRootPath(uiRootPath, nonApplicationRootPath).ifPresent(defaultIgnore::add);
                    return defaultIgnore;
                });
    }

    /**
     * <p>
     * Normalizes the {@link QuinoaConfig#uiRootPath()} and the returned path always starts with {@code "/"} and ends with
     * {@code "/"}.
     * <p>
     * Note that this will not resolve the path relative to 'quarkus.http.root-path'.
     */
    static String getNormalizedUiRootPath(QuinoaConfig config) {
        return normalizePath(config.uiRootPath(), true);
    }

    static QuinoaHandlerConfig toHandlerConfig(QuinoaConfig config, boolean devMode,
            final HttpBuildTimeConfig httpBuildTimeConfig, NonApplicationRootPathBuildItem nonApplicationRootPath) {
        final Set<String> compressMediaTypes = httpBuildTimeConfig.compressMediaTypes.map(Set::copyOf).orElse(Set.of());
        final String indexPage = resolveIndexPage(config, devMode);
        return new QuinoaHandlerConfig(getNormalizedIgnoredPathPrefixes(config, nonApplicationRootPath), indexPage, devMode,
                httpBuildTimeConfig.enableCompression, compressMediaTypes, config.devServer().directForwarding());
    }

    /**
     * Normalizes the path and the returned path starts with a slash and if {@code trailingSlash} is set to {@code true} then it
     * will also end in a slash.
     */
    private static String normalizePath(String path, boolean trailingSlash) {
        String normalizedPath = UriNormalizationUtil.toURI(path, trailingSlash).getPath();
        return normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
    }

    /**
     * Note that {@code rootPath} and {@code leafPath} are required to start and end in a slash.
     * The returned path also fulfills this requirement.
     */
    private static Optional<String> relativizePath(String rootPath, String leafPath) {
        return Optional.ofNullable(UriNormalizationUtil.relativize(rootPath, leafPath))
                // note that relativize always removes the leading slash
                .map(s -> "/" + s);
    }

    private static String resolveIndexPage(QuinoaConfig config, boolean devMode) {
        if (!devMode) {
            // Make sure we never return the devServer.indexPage() in non-dev mode
            return config.indexPage();
        }
        return isDevServerMode(config) ? config.devServer().indexPage().orElse(config.indexPage()) : config.indexPage();
    }

    private static Optional<String> readExternalConfigPath(String uiRootPath, Config config, String key) {
        return config.getOptionalValue(key, String.class)
                .map(s -> normalizePath(s, true))
                // only add this path if it is relative to the ui-root-path
                .flatMap(s -> relativizePath(uiRootPath, s))
                .filter(s -> !Objects.equals(s, "/"))
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s);
    }

    private static Optional<String> convertNonApplicationRootPath(String uiRootPath,
            NonApplicationRootPathBuildItem nonApplicationRootPath) {
        // only add the non-application root path if it is relative to the http root path
        // note that both paths start and end in a slash already
        return relativizePath(nonApplicationRootPath.getNormalizedHttpRootPath(),
                nonApplicationRootPath.getNonApplicationRootPath())
                // and also only add this path if it is relative to the ui-root-path
                .flatMap(s -> relativizePath(uiRootPath, s))
                .filter(s -> !Objects.equals(s, "/"))
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s);
    }

    static boolean isDevServerMode(QuinoaConfig config) {
        return config.devServer().enabled() && config.devServer().port().isPresent();
    }

    static boolean isEnabled(QuinoaConfig config) {
        return config.enabled().orElse(true);
    }

    static boolean isEqual(QuinoaConfig q1, QuinoaConfig q2) {
        if (!Objects.equals(q1.enabled(), q2.enabled())) {
            return false;
        }
        if (!Objects.equals(q1.justBuild(), q2.justBuild())) {
            return false;
        }
        if (!Objects.equals(q1.uiRootPath(), q2.uiRootPath())) {
            return false;
        }
        if (!Objects.equals(q1.uiDir(), q2.uiDir())) {
            return false;
        }
        if (!Objects.equals(q1.buildDir(), q2.buildDir())) {
            return false;
        }
        if (!Objects.equals(q1.packageManager(), q2.packageManager())) {
            return false;
        }
        if (!PackageManagerInstallConfig.isEqual(q1.packageManagerInstall(), q2.packageManagerInstall())) {
            return false;
        }
        if (!PackageManagerCommandConfig.isEqual(q1.packageManagerCommand(), q2.packageManagerCommand())) {
            return false;
        }
        if (!Objects.equals(q1.indexPage(), q2.indexPage())) {
            return false;
        }
        if (!Objects.equals(q1.runTests(), q2.runTests())) {
            return false;
        }
        if (!Objects.equals(q1.ci(), q2.ci())) {
            return false;
        }
        if (!Objects.equals(q1.forceInstall(), q2.forceInstall())) {
            return false;
        }
        if (!FrameworkConfig.isEqual(q1.framework(), q2.framework())) {
            return false;
        }
        if (!Objects.equals(q1.enableSPARouting(), q2.enableSPARouting())) {
            return false;
        }
        if (!Objects.equals(q1.ignoredPathPrefixes(), q2.ignoredPathPrefixes())) {
            return false;
        }
        if (!DevServerConfig.isEqual(q1.devServer(), q2.devServer())) {
            return false;
        }
        return true;
    }

}
