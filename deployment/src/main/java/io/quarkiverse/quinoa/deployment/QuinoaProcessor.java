package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_SPA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.*;
import static io.quarkiverse.quinoa.deployment.framework.FrameworkType.overrideConfig;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner.autoDetectPackageManager;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.QuinoaRecorder;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.framework.FrameworkType;
import io.quarkiverse.quinoa.deployment.items.BuiltResourcesBuildItem;
import io.quarkiverse.quinoa.deployment.items.ConfiguredQuinoaBuildItem;
import io.quarkiverse.quinoa.deployment.items.InstalledPackageManagerBuildItem;
import io.quarkiverse.quinoa.deployment.items.TargetDirBuildItem;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner;
import io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;

public class QuinoaProcessor {

    private static final Logger LOG = Logger.getLogger(QuinoaProcessor.class);
    private static final Set<String> IGNORE_WATCH = Set.of("node_modules", "target");
    private static final Set<String> IGNORE_WATCH_LOCKFILES = Arrays.stream(PackageManagerType.values())
            .map(PackageManagerType::getLockFile).collect(Collectors.toSet());
    private static final Set<String> IGNORE_WATCH_BUILD_DIRS = Arrays.stream(FrameworkType.values()).sequential()
            .map(frameworkType -> frameworkType.factory().getDefaultBuildDir())
            .collect(Collectors.toSet());
    private static final Pattern IGNORE_WATCH_REGEX = Pattern.compile("^[.].+$"); // ignore "." directories

    private static final String FEATURE = "quinoa";
    private static final String TARGET_DIR_NAME = "quinoa";
    private static final String TARGET_BUILD_DIR_NAME = "build";
    private static final String BUILD_FILE = "package.json";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public ConfiguredQuinoaBuildItem prepareQuinoaDirectory(
            LaunchModeBuildItem launchMode,
            QuinoaConfig userConfig,
            OutputTargetBuildItem outputTarget) throws IOException {
        if (!isEnabled(userConfig)) {
            LOG.info("Quinoa is disabled.");
            return null;
        }
        if (launchMode.isTest() && userConfig.enabled().isEmpty()) {
            // Default to disabled in tests
            LOG.warn("Quinoa is disabled by default in tests.");
            return null;
        }
        final String configuredDir = userConfig.uiDir();
        final ProjectDirs projectDirs = resolveProjectDirs(userConfig, outputTarget);
        if (projectDirs == null) {
            return null;
        }
        final Path packageJson = projectDirs.uiDir.resolve(BUILD_FILE);
        if (!Files.isRegularFile(packageJson)) {
            throw new ConfigurationException("No " + BUILD_FILE + " found in Web UI directory: '" + configuredDir + "'");
        }

        initializeTargetDirectory(outputTarget);

        final QuinoaConfig resolvedConfig = overrideConfig(launchMode, userConfig, packageJson);

        return new ConfiguredQuinoaBuildItem(projectDirs.projectRootDir, projectDirs.uiDir, packageJson, resolvedConfig);
    }

    @BuildStep
    public InstalledPackageManagerBuildItem install(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            LiveReloadBuildItem liveReload,
            OutputTargetBuildItem outputTarget) throws IOException {
        if (configuredQuinoa != null) {
            final QuinoaConfig resolvedConfig = configuredQuinoa.resolvedConfig();
            Optional<String> packageManagerBinary = resolvedConfig.packageManager();
            List<String> paths = new ArrayList<>();
            if (resolvedConfig.packageManagerInstall().enabled()) {
                final PackageManagerInstall.Installation result = PackageManagerInstall.install(
                        resolvedConfig.packageManagerInstall(),
                        configuredQuinoa.projectDir(),
                        configuredQuinoa.uiDir());
                packageManagerBinary = Optional.of(result.getPackageManagerBinary());
                paths.add(result.getNodeDirPath());
            }

            final PackageManagerRunner packageManagerRunner = autoDetectPackageManager(packageManagerBinary,
                    resolvedConfig.packageManagerCommand(), configuredQuinoa.uiDir(), paths);
            final Path targetPackageJson = outputTarget.getOutputDirectory().resolve(TARGET_DIR_NAME).resolve(BUILD_FILE);
            final Path currentPackageJson = configuredQuinoa.packageJson();
            if (resolvedConfig.forceInstall()
                    || shouldInstallPackages(configuredQuinoa, liveReload, targetPackageJson, currentPackageJson)) {
                final boolean ci = resolvedConfig.ci().orElseGet(QuinoaProcessor::isCI);
                if (ci) {
                    packageManagerRunner.ci();
                } else {
                    packageManagerRunner.install();
                }
                // copy the package.json to build, so we can compare for next time
                Files.copy(currentPackageJson, targetPackageJson, StandardCopyOption.REPLACE_EXISTING);
            }
            return new InstalledPackageManagerBuildItem(packageManagerRunner);
        }
        return null;
    }

    @BuildStep
    public TargetDirBuildItem processBuild(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            InstalledPackageManagerBuildItem installedPackageManager,
            OutputTargetBuildItem outputTarget,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReload) throws IOException {
        if (configuredQuinoa == null) {
            return null;
        }

        final PackageManagerRunner packageManagerRunner = installedPackageManager.getPackageManager();
        final QuinoaLiveContext contextObject = liveReload.getContextObject(QuinoaLiveContext.class);
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT
                && isDevServerMode(configuredQuinoa.resolvedConfig())) {
            return null;
        }
        if (liveReload.isLiveReload()
                && liveReload.getChangedResources().stream()
                        .noneMatch(r -> r.startsWith(packageManagerRunner.getDirectory().toString()))
                && contextObject != null) {
            return new TargetDirBuildItem(contextObject.getLocation());
        }
        if (configuredQuinoa.resolvedConfig().runTests()) {
            packageManagerRunner.test();
        }
        packageManagerRunner.build(launchMode.getLaunchMode());
        final String configuredBuildDir = configuredQuinoa.resolvedConfig().buildDir().orElseThrow();
        final Path buildDir = packageManagerRunner.getDirectory().resolve(configuredBuildDir);
        if (!Files.isDirectory(buildDir)) {
            throw new ConfigurationException("Quinoa build directory not found: '" + buildDir.toAbsolutePath() + "'",
                    Set.of("quarkus.quinoa.build-dir"));
        }

        final Path targetBuildDir = initializeTargetDirectory(outputTarget).resolve(TARGET_BUILD_DIR_NAME);
        FileUtil.deleteDirectory(targetBuildDir);
        try {
            Files.move(buildDir, targetBuildDir);
        } catch (IOException e) {
            String message = String.format(
                    "Error moving directory '%s -> %s'. Please make sure no files are open such as in Files Explorer or other tools.",
                    buildDir, targetBuildDir);
            throw new IOException(message, e);
        }
        liveReload.setContextObject(QuinoaLiveContext.class, new QuinoaLiveContext(targetBuildDir));
        return new TargetDirBuildItem(targetBuildDir);
    }

    @BuildStep
    public BuiltResourcesBuildItem prepareBuiltResources(Optional<TargetDirBuildItem> targetDir) throws IOException {
        if (targetDir.isEmpty()) {
            return null;
        }
        return new BuiltResourcesBuildItem(lookupBuiltResources(targetDir.get().getBuildDirectory()));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void watchChanges(
            Optional<ConfiguredQuinoaBuildItem> quinoaDir,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths) throws IOException {
        if (quinoaDir.isEmpty()) {
            return;
        }
        if (isDevServerMode(quinoaDir.get().resolvedConfig())) {
            final HotDeploymentWatchedFileBuildItem watchPackageJson = HotDeploymentWatchedFileBuildItem.builder()
                    .setLocation(quinoaDir.get().packageJson().toString())
                    .setRestartNeeded(true)
                    .build();
            watchedPaths.produce(watchPackageJson);
            return;
        }
        scan(quinoaDir.get().uiDir(), quinoaDir.get().uiDir(), watchedPaths);
    }

    @BuildStep
    public void produceGeneratedStaticResources(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            BuildProducer<GeneratedStaticResourceBuildItem> generatedStaticResourceProducer,
            Optional<BuiltResourcesBuildItem> uiResources) throws IOException {
        if (configuredQuinoa != null && configuredQuinoa.resolvedConfig().justBuild()) {
            LOG.info("Quinoa is in build only mode");
            return;
        }
        if (uiResources.isPresent() && !uiResources.get().resources().isEmpty()) {
            String uiRootPath = QuinoaConfig.getNormalizedUiRootPath(configuredQuinoa.resolvedConfig());
            for (BuiltResourcesBuildItem.BuiltResource resource : uiResources.get().resources()) {
                // note how uiRootPath always starts and ends in a slash
                // and resource.name() always starts in a slash, therfore resource.name().substring(1) never starts in a slash
                generatedStaticResourceProducer
                        .produce(new GeneratedStaticResourceBuildItem(uiRootPath + resource.name().substring(1),
                                resource.content()));
            }
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            HttpRootPathBuildItem httpRootPath,
            NonApplicationRootPathBuildItem nonApplicationRootPath,
            QuinoaRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            Optional<BuiltResourcesBuildItem> uiResources) throws IOException {
        if (configuredQuinoa != null && configuredQuinoa.resolvedConfig().justBuild()) {
            return;
        }
        if (uiResources.isPresent() && !uiResources.get().resources().isEmpty()) {
            String uiRootPath = QuinoaConfig.getNormalizedUiRootPath(configuredQuinoa.resolvedConfig());
            // the resolvedUiRootPath is only used for logging
            String resolvedUiRootPath = httpRootPath.relativePath(uiRootPath);
            recorder.logUiRootPath(resolvedUiRootPath.endsWith("/") ? resolvedUiRootPath : resolvedUiRootPath + "/");
            if (configuredQuinoa.resolvedConfig().enableSPARouting()) {
                routes.produce(RouteBuildItem.builder().orderedRoute(uiRootPath + "*", QUINOA_SPA_ROUTE_ORDER)
                        .handler(recorder
                                .quinoaSPARoutingHandler(getNormalizedIgnoredPathPrefixes(configuredQuinoa.resolvedConfig(),
                                        nonApplicationRootPath)))
                        .build());
            }
        }
    }

    private HashSet<BuiltResourcesBuildItem.BuiltResource> lookupBuiltResources(Path targetDir) throws IOException {
        try (Stream<Path> paths = Files.walk(targetDir, FileVisitOption.FOLLOW_LINKS).filter(Files::isRegularFile)) {
            final var files = paths.toList();
            final HashSet<BuiltResourcesBuildItem.BuiltResource> entries = new HashSet<>(files.size());
            LOG.infof("Quinoa target directory: '%s'", targetDir);
            for (Path file : files) {
                final String name = "/" + targetDir.relativize(file).toString().replace('\\', '/');
                LOG.infof("Quinoa generated resource: '%s'", name);
                entries.add(new BuiltResourcesBuildItem.BuiltResource(name, Files.readAllBytes(file)));
            }
            return entries;
        }

    }

    private void scan(Path uiDir, Path directory, BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths)
            throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            Iterator<Path> iter = files.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                final String relativePath = uiDir.relativize(filePath).toString();
                if (shouldWatch(relativePath)) {
                    if (Files.isRegularFile(filePath)) {
                        LOG.debugf("Quinoa is watching: %s", filePath);
                        watchedPaths.produce(new HotDeploymentWatchedFileBuildItem(filePath.toString()));
                    } else {
                        LOG.debugf("Quinoa is scanning directory: %s", filePath);
                        scan(uiDir, filePath, watchedPaths);
                    }
                } else {
                    LOG.debugf("'%s' is set to be ignored by dev-mode watch", relativePath);
                }
            }
        }
    }

    private static boolean shouldInstallPackages(ConfiguredQuinoaBuildItem configuredQuinoa,
            LiveReloadBuildItem liveReload,
            Path targetPackageJson,
            Path currentPackageJson) throws IOException {

        if (!Files.isDirectory(configuredQuinoa.uiDir().resolve("node_modules"))) {
            LOG.info("Quinoa didn't detect a node_modules directory, let's install packages...");
            return true;
        }

        if (isPackageJsonLiveReloadChanged(configuredQuinoa, liveReload)) {
            return true;
        }

        if (!Files.exists(targetPackageJson)) {
            LOG.info("Fresh Quinoa build, let's install packages...");
            return true;
        }
        // Check for size then content
        if (Files.size(currentPackageJson) != Files.size(targetPackageJson)
                || !Arrays.equals(Files.readAllBytes(currentPackageJson), Files.readAllBytes(targetPackageJson))) {
            LOG.info("Quinoa detected a change in package.json since the previous install, let's install packages again...");
            return true;
        }

        LOG.debug("package.json seems to be the same as previous Quinoa install, skipping packages install");
        return false;
    }

    static boolean isPackageJsonLiveReloadChanged(ConfiguredQuinoaBuildItem configuredQuinoa, LiveReloadBuildItem liveReload) {
        return liveReload.isLiveReload()
                && liveReload.getChangedResources().stream()
                        .anyMatch(r -> r.equals(configuredQuinoa.packageJson().toString()));
    }

    /**
     * Check whether this path should be scanned for changes by comparing against known files that should be ignored.
     * Ignored directories include any that start with DOT "." like ".next" or ".svelte", also "node_modules" and any
     * of the framework build directories.
     *
     * @param relativeFilePath the file path to check
     * @return true if it is a directory that should be scanned for changes, false if it should be ignored
     */
    private static boolean shouldWatch(String relativeFilePath) {
        final Set<String> ignoreSet = new HashSet<>();
        ignoreSet.addAll(IGNORE_WATCH);
        ignoreSet.addAll(IGNORE_WATCH_LOCKFILES);
        ignoreSet.addAll(IGNORE_WATCH_BUILD_DIRS);
        return !ignoreSet.contains(relativeFilePath) && !IGNORE_WATCH_REGEX.matcher(relativeFilePath).matches();
    }

    private static ProjectDirs resolveProjectDirs(QuinoaConfig config,
            OutputTargetBuildItem outputTarget) {
        Path projectRoot = findProjectRoot(outputTarget.getOutputDirectory());
        Path configuredUIDirPath = Path.of(config.uiDir().trim());
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            if (configuredUIDirPath.isAbsolute() && Files.isDirectory(configuredUIDirPath)) {
                return new ProjectDirs(null, configuredUIDirPath.normalize());
            }
            throw new IllegalStateException(
                    "If not absolute, the Web UI directory is resolved relative to the project root, but Quinoa was not able to find the project root.");
        }
        final Path uiRoot = projectRoot.resolve(configuredUIDirPath).normalize();
        if (!Files.isDirectory(uiRoot)) {
            LOG.warnf(
                    "Quinoa directory not found 'quarkus.quinoa.ui-dir=%s' resolved to '%s'. It is recommended to remove the quarkus-quinoa extension if not used.",
                    config.uiDir(),
                    uiRoot.toAbsolutePath());
            return null;
        }
        return new ProjectDirs(projectRoot, uiRoot);
    }

    static Path findProjectRoot(Path outputDirectory) {
        Path currentPath = outputDirectory;
        do {
            if (Files.exists(currentPath.resolve(Paths.get("src", "main")))
                    || Files.exists(currentPath.resolve(Paths.get("config", "application.properties")))
                    || Files.exists(currentPath.resolve(Paths.get("config", "application.yaml")))
                    || Files.exists(currentPath.resolve(Paths.get("config", "application.yml")))) {
                return currentPath.normalize();
            }
            if (currentPath.getParent() != null && Files.exists(currentPath.getParent())) {
                currentPath = currentPath.getParent();
            } else {
                return null;
            }
        } while (true);
    }

    private static boolean isCI() {
        String ci;
        if (System.getProperties().containsKey("CI")) {
            ci = System.getProperty("CI");
        } else {
            ci = System.getenv().getOrDefault("CI", "false");
        }
        return Objects.equals(ci, "true");
    }

    public static Path initializeTargetDirectory(OutputTargetBuildItem outputTarget) throws IOException {
        final Path targetBuildDir = outputTarget.getOutputDirectory().resolve(TARGET_DIR_NAME);
        Files.createDirectories(targetBuildDir);
        return targetBuildDir;
    }

    private static class QuinoaLiveContext {
        private final Path location;

        private QuinoaLiveContext(Path location) {
            this.location = location;
        }

        public Path getLocation() {
            return location;
        }
    }

    public static class ProjectDirs {
        private final Path projectRootDir;
        private final Path uiDir;

        public ProjectDirs(Path projectRootDir, Path uiDir) {
            this.projectRootDir = projectRootDir;
            this.uiDir = uiDir;
        }

        public Path getProjectRootDir() {
            return projectRootDir;
        }

        public Path getUIDir() {
            return uiDir;
        }
    }

}
