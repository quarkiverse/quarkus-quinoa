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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.QuinoaHandlerConfig;
import io.quarkiverse.quinoa.QuinoaRecorder;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManager;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
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
    private static final Set<String> IGNORE_WATCH = Set.of("node_modules", "build", "target", "dist");

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
            LOG.warnf(
                    "Quinoa directory not found 'quarkus.quinoa.ui-dir=%s'. It is recommended to remove the quarkus-quinoa extension if not used.",
                    configuredDir);
            return null;
        }
        final Path packageFile = projectDirs.uiDir.resolve("package.json");
        if (!Files.isRegularFile(packageFile)) {
            throw new ConfigurationException(
                    "No package.json found in Web UI directory: '" + configuredDir + "'");
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
                && liveReload.getChangedResources().stream().anyMatch(r -> r.equals(packageFile.toString()));
        if (quinoaConfig.forceInstall || !alreadyInstalled || packageFileModified) {
            final boolean frozenLockfile = quinoaConfig.frozenLockfile.orElseGet(QuinoaProcessor::isCI);
            packageManager.install(frozenLockfile);
        }
        return new QuinoaDirectoryBuildItem(packageManager);
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

        final PackageManager packageManager = quinoaDir.get().getPackageManager();
        final QuinoaLiveContext contextObject = liveReload.getContextObject(QuinoaLiveContext.class);
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT && quinoaConfig.isDevServerMode()) {
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
        final String configuredBuildDir = quinoaConfig.buildDir;
        final Path buildDir = packageManager.getDirectory().resolve(configuredBuildDir);
        if (!Files.isDirectory(buildDir)) {
            throw new ConfigurationException("Quinoa build directory not found: '" + buildDir + "'",
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
        if (!quinoaDir.isPresent() || quinoaConfig.isDevServerMode()) {
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

    private ProjectDirs resolveProjectDirs(QuinoaConfig config,
            OutputTargetBuildItem outputTarget) {
        Path projectRoot = findProjectRoot(outputTarget.getOutputDirectory());
        Path configuredUIDirPath = Path.of(config.uiDir.trim());
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            if (configuredUIDirPath.isAbsolute() && Files.isDirectory(configuredUIDirPath)) {
                return new ProjectDirs(null, configuredUIDirPath);
            }
            return null;
        }
        final Path uiRoot = projectRoot.resolve(configuredUIDirPath);
        if (!Files.isDirectory(uiRoot)) {
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
                return currentPath;
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
