package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.QuinoaRecorder.META_INF_WEB_UI;
import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_SPA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.deployment.PackageManager.autoDetectPackageManager;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.QuinoaRecorder;
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
        final String configuredDir = quinoaConfig.getUIDir();
        final AbstractMap.SimpleEntry<Path, Path> uiDirEntry = computeUIDir(configuredDir, outputTarget);
        if (uiDirEntry == null) {
            LOG.warnf(
                    "Quinoa directory not found 'quarkus.quinoa.ui-dir=%s'. It is recommended to remove the quarkus-quinoa extension if not used.",
                    configuredDir);
            return null;
        }
        final Path packageFile = uiDirEntry.getKey().resolve("package.json");
        if (!Files.isRegularFile(packageFile)) {
            throw new ConfigurationException(
                    "No package.json found in Web UI directory: '" + configuredDir + "'");
        }
        PackageManager packageManager = autoDetectPackageManager(quinoaConfig.packageManager, uiDirEntry.getKey());
        final boolean alreadyInstalled = Files.isDirectory(packageManager.getDirectory().resolve("node_modules"));
        final boolean packageFileModified = liveReload.isLiveReload()
                && liveReload.getChangedResources().stream().anyMatch(r -> r.equals(packageFile.toString()));
        if (quinoaConfig.alwaysInstall.orElse(!alreadyInstalled || packageFileModified)) {
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
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT && quinoaConfig.devServerPort.isPresent()) {
            return null;
        }
        if (liveReload.isLiveReload()
                && liveReload.getChangedResources().stream()
                        .noneMatch(r -> r.startsWith(packageManager.getDirectory().toString()))
                && contextObject != null) {
            return new TargetDirBuildItem(contextObject.getLocation());
        }
        if (quinoaConfig.shouldRunTests()) {
            packageManager.test();
        }
        packageManager.build(launchMode.getLaunchMode());
        final String configuredBuildDir = quinoaConfig.getBuildDir();
        final Path buildDir = packageManager.getDirectory().resolve(configuredBuildDir);
        if (!Files.isDirectory(buildDir)) {
            throw new ConfigurationException("Quinoa build directory not found: '" + configuredBuildDir + "'",
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
        if (!quinoaDir.isPresent() || quinoaConfig.devServerPort.isPresent()) {
            return;
        }
        scan(quinoaDir.get().getPackageManager().getDirectory(), watchedPaths);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(
            QuinoaConfig quinoaConfig,
            Optional<BuiltResourcesBuildItem> uiResources,
            QuinoaRecorder recorder,
            CoreVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<ResumeOn404BuildItem> resumeOn404) throws IOException {
        if (uiResources.isPresent() && !uiResources.get().getNames().isEmpty()) {
            String directory = null;
            if (uiResources.get().getDirectory().isPresent()) {
                directory = uiResources.get().getDirectory().get().toAbsolutePath().toString();
            }
            final List<String> ignoredPathPrefixes = quinoaConfig.getNormalizedIgnoredPathPrefixes();
            final boolean enableSPARouting = quinoaConfig.isSPARoutingEnabled();
            resumeOn404.produce(new ResumeOn404BuildItem());
            routes.produce(RouteBuildItem.builder().orderedRoute("/*", QUINOA_ROUTE_ORDER)
                    .handler(recorder.quinoaHandler(directory,
                            uiResources.get().getNames(), ignoredPathPrefixes))
                    .build());
            if (enableSPARouting) {
                routes.produce(RouteBuildItem.builder().orderedRoute("/*", QUINOA_SPA_ROUTE_ORDER)
                        .handler(recorder.quinoaSPARoutingHandler(ignoredPathPrefixes))
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
            final String name = "/" + targetDir.relativize(file);
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

    private AbstractMap.SimpleEntry<Path, Path> computeUIDir(String configuredDir,
            OutputTargetBuildItem outputTarget) {
        Map.Entry<Path, Path> mainSourcesRoot = findMainSourcesRoot(outputTarget.getOutputDirectory());
        if (mainSourcesRoot == null) {
            return null;
        }
        final Path uiRoot = mainSourcesRoot.getValue().resolve(configuredDir);
        final File file = uiRoot.toFile();
        if (!file.exists() || !file.isDirectory()) {
            return null;
        }
        return new AbstractMap.SimpleEntry<>(uiRoot, mainSourcesRoot.getValue());
    }

    /**
     * Return a Map.Entry (which is used as a Tuple) containing the main sources root as the key
     * and the project root as the value
     */
    static AbstractMap.SimpleEntry<Path, Path> findMainSourcesRoot(Path outputDirectory) {
        Path currentPath = outputDirectory;
        do {
            Path toCheck = currentPath.resolve(Paths.get("src", "main"));
            if (toCheck.toFile().exists()) {
                return new AbstractMap.SimpleEntry<>(toCheck, currentPath);
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

}
