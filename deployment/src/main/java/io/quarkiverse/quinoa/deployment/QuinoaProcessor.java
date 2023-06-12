package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.QuinoaRecorder.META_INF_WEB_UI;
import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_SPA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManager.autoDetectPackageManager;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.install;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.QuinoaHandlerConfig;
import io.quarkiverse.quinoa.QuinoaRecorder;
import io.quarkiverse.quinoa.deployment.packagemanager.DetectedFramework;
import io.quarkiverse.quinoa.deployment.packagemanager.FrameworkType;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManager;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerType;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
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
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;

public class QuinoaProcessor {

    private static final Logger LOG = Logger.getLogger(QuinoaProcessor.class);

    private static final String FEATURE = "quinoa";
    private static final String TARGET_DIR_NAME = "quinoa-build";
    private static final Set<String> IGNORE_WATCH = Set.of("node_modules", "build", "target", "dist", "out");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public QuinoaDirectoryBuildItem prepareQuinoaDirectory(
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReload,
            QuinoaConfig quinoaConfig,
            OutputTargetBuildItem outputTarget) throws IOException {
        if (!quinoaConfig.isEnabled()) {
            LOG.info("Quinoa is disabled.");
            return null;
        }
        if (launchMode.isTest() && !quinoaConfig.enable.isPresent()) {
            // Default to disabled in tests
            LOG.warn("Quinoa is disabled by default in tests.");
            return null;
        }
        final String configuredDir = quinoaConfig.uiDir;
        final ProjectDirs projectDirs = resolveProjectDirs(quinoaConfig, outputTarget);
        if (projectDirs == null) {
            return null;
        }
        final Path packageJsonFile = projectDirs.uiDir.resolve("package.json");
        if (!Files.isRegularFile(packageJsonFile)) {
            throw new ConfigurationException("No package.json found in Web UI directory: '" + configuredDir + "'");
        }
        Optional<String> packageManagerBinary = quinoaConfig.packageManager;
        List<String> paths = new ArrayList<>();
        if (quinoaConfig.packageManagerInstall.enabled) {
            final PackageManagerInstall.Installation result = install(quinoaConfig.packageManagerInstall,
                    projectDirs);
            packageManagerBinary = Optional.of(result.getPackageManagerBinary());
            paths.add(result.getNodeDirPath());
        }
        PackageManager packageManager = autoDetectPackageManager(packageManagerBinary,
                quinoaConfig.packageManagerCommand, projectDirs.getUIDir(), paths);
        final boolean alreadyInstalled = Files.isDirectory(packageManager.getDirectory().resolve("node_modules"));
        final boolean packageFileModified = liveReload.isLiveReload()
                && liveReload.getChangedResources().stream().anyMatch(r -> r.equals(packageJsonFile.toString()));
        if (quinoaConfig.forceInstall || !alreadyInstalled || packageFileModified) {
            final boolean frozenLockfile = quinoaConfig.frozenLockfile.orElseGet(QuinoaProcessor::isCI);
            packageManager.install(frozenLockfile);
        }

        // attempt to autoconfigure settings based on the framework being used
        final DetectedFramework detectedFramework = detectFramework(launchMode, quinoaConfig, packageJsonFile);

        return initDefaultConfig(packageManager, launchMode, quinoaConfig, detectedFramework);
    }

    @BuildStep
    public TargetDirBuildItem processBuild(
            QuinoaConfig quinoaConfig,
            Optional<QuinoaDirectoryBuildItem> quinoaDir,
            OutputTargetBuildItem outputTarget,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReload) throws IOException, BuildException {
        if (!quinoaDir.isPresent()) {
            return null;
        }

        final QuinoaDirectoryBuildItem quinoaDirectoryBuildItem = quinoaDir.get();
        final PackageManager packageManager = quinoaDirectoryBuildItem.getPackageManager();
        final QuinoaLiveContext contextObject = liveReload.getContextObject(QuinoaLiveContext.class);
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT
                && quinoaDirectoryBuildItem.isDevServerMode(quinoaConfig.devServer)) {
            return null;
        }
        if (liveReload.isLiveReload()
                && liveReload.getChangedResources().stream()
                        .noneMatch(r -> r.startsWith(packageManager.getDirectory().toString()))
                && contextObject != null) {
            return new TargetDirBuildItem(contextObject.getLocation());
        }
        if (quinoaConfig.runTests) {
            packageManager.test();
        }
        packageManager.build(launchMode.getLaunchMode());
        final String configuredBuildDir = quinoaDirectoryBuildItem.getBuildDirectory();
        final Path buildDir = packageManager.getDirectory().resolve(configuredBuildDir);
        if (!Files.isDirectory(buildDir)) {
            throw new ConfigurationException("Quinoa build directory not found: '" + buildDir.toAbsolutePath() + "'",
                    Set.of("quarkus.quinoa.build-dir"));
        }
        final Path targetBuildDir = outputTarget.getOutputDirectory().resolve(TARGET_DIR_NAME);
        FileUtil.deleteDirectory(targetBuildDir);
        Files.move(buildDir, targetBuildDir);
        liveReload.setContextObject(QuinoaLiveContext.class, new QuinoaLiveContext(targetBuildDir));
        return new TargetDirBuildItem(targetBuildDir);
    }

    @BuildStep(onlyIf = IsNormal.class)
    public BuiltResourcesBuildItem prepareResourcesForNormalMode(
            Optional<TargetDirBuildItem> targetDir,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources) throws IOException {
        if (!targetDir.isPresent()) {
            return null;
        }
        return new BuiltResourcesBuildItem(
                prepareBuiltResources(generatedResources, nativeImageResources, targetDir.get().getBuildDirectory()));
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    public BuiltResourcesBuildItem prepareResourcesForOtherMode(
            Optional<TargetDirBuildItem> targetDir,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) throws IOException {
        if (!targetDir.isPresent()) {
            return null;
        }
        final HashSet<BuiltResourcesBuildItem.BuiltResource> entries = prepareBuiltResources(generatedResources,
                null, targetDir.get().getBuildDirectory());
        return new BuiltResourcesBuildItem(targetDir.get().getBuildDirectory(), entries);
    }

    @BuildStep
    void watchChanges(
            QuinoaConfig quinoaConfig,
            Optional<QuinoaDirectoryBuildItem> quinoaDir,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths) throws IOException {
        if (!quinoaDir.isPresent() || quinoaDir.get().isDevServerMode(quinoaConfig.devServer)) {
            return;
        }
        scan(quinoaDir.get().getPackageManager().getDirectory(), watchedPaths);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(
            QuinoaConfig quinoaConfig,
            HttpBuildTimeConfig httpBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            Optional<BuiltResourcesBuildItem> uiResources,
            QuinoaRecorder recorder,
            CoreVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<ResumeOn404BuildItem> resumeOn404) throws IOException {
        if (quinoaConfig.justBuild) {
            LOG.info("Quinoa is in build only mode");
            return;
        }
        if (uiResources.isPresent() && !uiResources.get().getNames().isEmpty()) {
            String directory = null;
            if (uiResources.get().getDirectory().isPresent()) {
                directory = uiResources.get().getDirectory().get().toAbsolutePath().toString();
            }
            final QuinoaHandlerConfig handlerConfig = quinoaConfig.toHandlerConfig(
                    !launchMode.getLaunchMode().isDevOrTest(),
                    httpBuildTimeConfig);
            resumeOn404.produce(new ResumeOn404BuildItem());
            routes.produce(RouteBuildItem.builder().orderedRoute("/*", QUINOA_ROUTE_ORDER)
                    .handler(recorder.quinoaHandler(handlerConfig, directory,
                            uiResources.get().getNames()))
                    .build());
            if (quinoaConfig.enableSPARouting) {
                routes.produce(RouteBuildItem.builder().orderedRoute("/*", QUINOA_SPA_ROUTE_ORDER)
                        .handler(recorder.quinoaSPARoutingHandler(handlerConfig))
                        .build());
            }
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    List<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles(QuinoaConfig quinoaConfig,
            OutputTargetBuildItem outputTarget) {
        final List<HotDeploymentWatchedFileBuildItem> watchedFiles = new ArrayList<>(PackageManagerType.values().length);
        final ProjectDirs projectDirs = resolveProjectDirs(quinoaConfig, outputTarget);
        if (projectDirs == null) {
            // UI dir is misconfigured so just skip watching files
            return watchedFiles;
        }
        for (PackageManagerType pm : PackageManagerType.values()) {
            final String watchFile = projectDirs.uiDir.resolve(pm.getLockFile()).toString();
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem(watchFile));
        }
        return watchedFiles;
    }

    private DetectedFramework detectFramework(LaunchModeBuildItem launchMode, QuinoaConfig config, Path packageJsonFile) {
        // only read package.json if the defaults are in use
        if (launchMode.getLaunchMode() == LaunchMode.NORMAL || (!config.devServer.port.isEmpty() &&
                !QuinoaConfig.DEFAULT_BUILD_DIR.equalsIgnoreCase(config.buildDir))) {
            return new DetectedFramework();
        }
        JsonObject packageJson = null;
        JsonString startScript = null;
        try (JsonReader reader = Json.createReader(Files.newInputStream(packageJsonFile))) {
            packageJson = reader.readObject();
            JsonObject scripts = packageJson.getJsonObject("scripts");
            if (scripts != null) {
                startScript = scripts.getJsonString("start");
                if (startScript == null) {
                    startScript = scripts.getJsonString("dev");
                }
            }
        } catch (IOException e) {
            LOG.warnf("Quinoa failed to auto-detect the framework from package.json file. %s", e.getMessage());
        }

        if (startScript == null) {
            LOG.info("Quinoa could not auto-detect the framework from package.json file.");
            return new DetectedFramework();
        }

        // check if we found a script to detect which framework
        final FrameworkType frameworkType = FrameworkType.evaluate(startScript.getString());
        if (frameworkType == null) {
            LOG.info("Quinoa could not auto-detect the framework from package.json file.");
            return new DetectedFramework();
        }

        LOG.infof("%s framework automatically detected from package.json file.", frameworkType);
        return new DetectedFramework(frameworkType, packageJson);
    }

    private QuinoaDirectoryBuildItem initDefaultConfig(PackageManager packageManager, LaunchModeBuildItem launchMode,
            QuinoaConfig config, DetectedFramework detectedFramework) {
        String buildDirectory = config.buildDir;
        OptionalInt port = config.devServer.port;

        if (detectedFramework == null || detectedFramework.getFrameworkType() == null) {
            // nothing to do as no framework was detected
            return new QuinoaDirectoryBuildItem(packageManager, port, buildDirectory);
        }

        // only override properties that have not been set
        FrameworkType framework = detectedFramework.getFrameworkType();
        if (launchMode.getLaunchMode() != LaunchMode.NORMAL && port.isEmpty()) {
            LOG.infof("%s framework setting dev server port: %d", framework, framework.getDevServerPort());
            port = OptionalInt.of(framework.getDevServerPort());
        }

        if (QuinoaConfig.DEFAULT_BUILD_DIR.equalsIgnoreCase(buildDirectory)) {
            buildDirectory = detectedFramework.getBuildDirectory();
            LOG.infof("%s framework setting build directory: '%s'", framework, buildDirectory);
        }
        return new QuinoaDirectoryBuildItem(packageManager, port, buildDirectory);
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
                } else if (Files.isDirectory(filePath) && !IGNORE_WATCH.contains(filePath.getFileName().toString())) {
                    LOG.debugf("Quinoa is scanning directory: %s", filePath);
                    scan(filePath, watchedPaths);
                }
            }
        }
    }

    private static ProjectDirs resolveProjectDirs(QuinoaConfig config,
            OutputTargetBuildItem outputTarget) {
        Path projectRoot = findProjectRoot(outputTarget.getOutputDirectory());
        Path configuredUIDirPath = Path.of(config.uiDir.trim());
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
                    config.uiDir,
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