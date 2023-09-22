package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.QuinoaRecorder.META_INF_WEB_UI;
import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_SPA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.isDevServerMode;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.isEnabled;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.toHandlerConfig;
import static io.quarkiverse.quinoa.deployment.framework.FrameworkType.overrideConfig;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner.autoDetectPackageManager;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
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

import io.quarkiverse.quinoa.QuinoaHandlerConfig;
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
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.resteasy.reactive.server.spi.ResumeOn404BuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;

public class QuinoaProcessor {

    private static final Logger LOG = Logger.getLogger(QuinoaProcessor.class);
    private static final Set<String> IGNORE_WATCH = Set.of("node_modules", "target");
    private static final Set<String> IGNORE_WATCH_BUILD_DIRS = Arrays.stream(FrameworkType.values()).sequential()
            .map(frameworkType -> frameworkType.factory().getFrameworkBuildDir())
            .collect(Collectors.toSet());
    private static final Pattern IGNORE_WATCH_REGEX = Pattern.compile("^[.].+$"); // ignore "." directories

    private static final String FEATURE = "quinoa";
    private static final String TARGET_DIR_NAME = "quinoa-build";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public ConfiguredQuinoaBuildItem prepareQuinoaDirectory(
            LaunchModeBuildItem launchMode,
            QuinoaConfig userConfig,
            OutputTargetBuildItem outputTarget) {
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
        final Path packageJson = projectDirs.uiDir.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            throw new ConfigurationException("No package.json found in Web UI directory: '" + configuredDir + "'");
        }

        final QuinoaConfig resolvedConfig = overrideConfig(launchMode, userConfig, packageJson);

        return new ConfiguredQuinoaBuildItem(projectDirs.projectRootDir, projectDirs.uiDir, packageJson, resolvedConfig);
    }

    @BuildStep
    public InstalledPackageManagerBuildItem install(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            LiveReloadBuildItem liveReload) {
        if (configuredQuinoa != null) {
            final QuinoaConfig resolvedConfig = configuredQuinoa.resolvedConfig();
            Optional<String> packageManagerBinary = resolvedConfig.packageManager();
            List<String> paths = new ArrayList<>();
            if (resolvedConfig.packageManagerInstall().enabled()) {
                final PackageManagerInstall.Installation result = PackageManagerInstall.install(
                        resolvedConfig.packageManagerInstall(),
                        configuredQuinoa.projectDir());
                packageManagerBinary = Optional.of(result.getPackageManagerBinary());
                paths.add(result.getNodeDirPath());
            }

            final PackageManagerRunner packageManagerRunner = autoDetectPackageManager(packageManagerBinary,
                    resolvedConfig.packageManagerCommand(), configuredQuinoa.uiDir(), paths);
            final boolean alreadyInstalled = Files.isDirectory(packageManagerRunner.getDirectory().resolve("node_modules"));
            final boolean packageFileModified = liveReload.isLiveReload()
                    && liveReload.getChangedResources().stream()
                            .anyMatch(r -> r.equals(configuredQuinoa.packageJson().toString()));
            if (resolvedConfig.forceInstall() || !alreadyInstalled || packageFileModified) {
                final boolean ci = resolvedConfig.ci().orElseGet(QuinoaProcessor::isCI);
                if (ci) {
                    packageManagerRunner.ci();
                } else {
                    packageManagerRunner.install();
                }

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
        final Path targetBuildDir = outputTarget.getOutputDirectory().resolve(TARGET_DIR_NAME);
        FileUtil.deleteDirectory(targetBuildDir);
        LOG.infof("Copy build directory: %s to target directory: %s", buildDir, targetBuildDir);
        copyDirectory(buildDir, targetBuildDir);
        liveReload.setContextObject(QuinoaLiveContext.class, new QuinoaLiveContext(targetBuildDir));
        return new TargetDirBuildItem(targetBuildDir);
    }

    @BuildStep(onlyIf = IsNormal.class)
    public BuiltResourcesBuildItem prepareResourcesForNormalMode(
            Optional<TargetDirBuildItem> targetDir,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources) throws IOException {
        if (targetDir.isEmpty()) {
            return null;
        }
        return new BuiltResourcesBuildItem(
                prepareBuiltResources(generatedResources, nativeImageResources, targetDir.get().getBuildDirectory()));
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    public BuiltResourcesBuildItem prepareResourcesForOtherMode(
            Optional<TargetDirBuildItem> targetDir,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) throws IOException {
        if (targetDir.isEmpty()) {
            return null;
        }
        final HashSet<BuiltResourcesBuildItem.BuiltResource> entries = prepareBuiltResources(generatedResources,
                null, targetDir.get().getBuildDirectory());
        return new BuiltResourcesBuildItem(targetDir.get().getBuildDirectory(), entries);
    }

    @BuildStep
    void watchChanges(
            Optional<ConfiguredQuinoaBuildItem> quinoaDir,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths) throws IOException {
        if (quinoaDir.isEmpty() || isDevServerMode(quinoaDir.get().resolvedConfig())) {
            return;
        }
        scan(quinoaDir.get().uiDir(), watchedPaths);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            HttpBuildTimeConfig httpBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            Optional<BuiltResourcesBuildItem> uiResources,
            QuinoaRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<ResumeOn404BuildItem> resumeOn404) throws IOException {
        if (configuredQuinoa != null && configuredQuinoa.resolvedConfig().justBuild()) {
            LOG.info("Quinoa is in build only mode");
            return;
        }
        if (uiResources.isPresent() && !uiResources.get().getNames().isEmpty()) {
            String directory = null;
            if (uiResources.get().getDirectory().isPresent()) {
                directory = uiResources.get().getDirectory().get().toAbsolutePath().toString();
            }
            final QuinoaHandlerConfig handlerConfig = toHandlerConfig(configuredQuinoa.resolvedConfig(),
                    !launchMode.getLaunchMode().isDevOrTest(),
                    httpBuildTimeConfig);
            resumeOn404.produce(new ResumeOn404BuildItem());
            routes.produce(RouteBuildItem.builder().orderedRoute("/*", QUINOA_ROUTE_ORDER)
                    .handler(recorder.quinoaHandler(handlerConfig, directory,
                            uiResources.get().getNames()))
                    .build());
            if (configuredQuinoa.resolvedConfig().enableSPARouting()) {
                routes.produce(RouteBuildItem.builder().orderedRoute("/*", QUINOA_SPA_ROUTE_ORDER)
                        .handler(recorder.quinoaSPARoutingHandler(handlerConfig))
                        .build());
            }
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    List<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles(Optional<ConfiguredQuinoaBuildItem> configuredQuinoa,
            OutputTargetBuildItem outputTarget) {
        final List<HotDeploymentWatchedFileBuildItem> watchedFiles = new ArrayList<>(PackageManagerType.values().length);
        if (configuredQuinoa.isEmpty()) {
            return watchedFiles;
        }

        for (PackageManagerType pm : PackageManagerType.values()) {
            final String watchFile = configuredQuinoa.get().uiDir().resolve(pm.getLockFile()).toString();
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem(watchFile));
        }
        return watchedFiles;
    }

    private HashSet<BuiltResourcesBuildItem.BuiltResource> prepareBuiltResources(
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            Path targetDir) throws IOException {
        final List<Path> files = Files.walk(targetDir).filter(Files::isRegularFile)
                .collect(Collectors.toList());
        final HashSet<BuiltResourcesBuildItem.BuiltResource> entries = new HashSet<>(files.size());
        LOG.infof("Quinoa target directory: '%s'", targetDir);
        for (Path file : files) {
            final String name = "/" + targetDir.relativize(file).toString().replace('\\', '/');
            LOG.infof("Quinoa generated resource: '%s'", name);
            generatedResources.produce(new GeneratedResourceBuildItem(META_INF_WEB_UI + name, Files.readAllBytes(file), true));
            if (nativeImageResources != null) {
                nativeImageResources
                        .produce(new NativeImageResourceBuildItem(META_INF_WEB_UI + name));
            }
            entries.add(new BuiltResourcesBuildItem.BuiltResource(name));
        }
        return entries;
    }

    private void scan(Path directory, BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths)
            throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            Iterator<Path> iter = files.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                if (Files.isRegularFile(filePath)) {
                    LOG.debugf("Quinoa is watching: %s", filePath);
                    watchedPaths.produce(new HotDeploymentWatchedFileBuildItem(filePath.toString()));
                } else if (shouldScanPath(filePath)) {
                    LOG.debugf("Quinoa is scanning directory: %s", filePath);
                    scan(filePath, watchedPaths);
                }
            }
        }
    }

    /**
     * Check whether this path should be scanned for changes by comparing against known directories that should be ignored.
     * Ignored directories include any that start with DOT "." like ".next" or ".svelte", also "node_modules" and any
     * of the framework build directories.
     *
     * @param filePath the file path to check
     * @return true if it is a directory that should be scanned for changes, false if it should be ignored
     */
    private static boolean shouldScanPath(Path filePath) {
        if (!Files.isDirectory(filePath)) {
            // not a directory so do not scan
            return false;
        }

        final Set<String> ignoreSet = new HashSet<>();
        ignoreSet.addAll(IGNORE_WATCH);
        ignoreSet.addAll(IGNORE_WATCH_BUILD_DIRS);
        final String directory = filePath.getFileName().toString();
        if (ignoreSet.contains(directory) || IGNORE_WATCH_REGEX.matcher(directory).matches()) {
            return false;
        }
        return true;
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

    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
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
