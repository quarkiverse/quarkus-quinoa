package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.QuinoaRecorder.META_INF_UI;
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
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

public class QuinoaProcessor {

    private static final Logger log = Logger.getLogger(QuinoaProcessor.class);

    private static final String FEATURE = "quinoa";
    private static final String TARGET_DIR_NAME = "quinoa-build";
    private static final Set<String> IGNORE_WATCH = Set.of("node_modules", "build", "target", "dist");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public SrcUIDirBuildItem prepareSrcUI(
            LaunchModeBuildItem launchModeBuildItem,
            QuinoaConfig quinoaConfig,
            OutputTargetBuildItem outputTargetBuildItem) throws IOException {
        if (!quinoaConfig.enable.orElse(true)) {
            return null;
        }
        if (launchModeBuildItem.isTest() && !quinoaConfig.enable.isPresent()) {
            // Default to disabled in tests
            return null;
        }
        final AbstractMap.SimpleEntry<Path, Path> srcUIDirEntry = getUIDir(quinoaConfig, outputTargetBuildItem);
        if (srcUIDirEntry == null || !Files.isDirectory(srcUIDirEntry.getKey())) {
            log.warnf(
                    "No Quinoa UI directory found. It is recommended to remove the quarkus-quinoa extension if not used.");
            return null;
        }
        if (!Files.isRegularFile(srcUIDirEntry.getKey().resolve("package.json"))) {
            throw new ConfigurationException(
                    "No package.json found in UI directory: '" + srcUIDirEntry.getKey() + "'");
        }
        return new SrcUIDirBuildItem(srcUIDirEntry.getKey());
    }

    @BuildStep
    public UIBuildOutcomeBuildItem uiBuild(
            QuinoaConfig quinoaConfig,
            Optional<SrcUIDirBuildItem> srcUIDirBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            LiveReloadBuildItem liveReloadBuildItem) throws IOException, BuildException {
        if (!srcUIDirBuildItem.isPresent()) {
            return null;
        }
        final QuinoaLiveContext contextObject = liveReloadBuildItem.getContextObject(QuinoaLiveContext.class);
        if (liveReloadBuildItem.isLiveReload()
                && liveReloadBuildItem.getChangedResources().stream()
                        .noneMatch(r -> r.startsWith(srcUIDirBuildItem.get().getDirectory().toString()))
                && contextObject != null) {
            return new UIBuildOutcomeBuildItem(contextObject.getLocation());
        }
        final String packageManagerBinary = quinoaConfig.packageManager.orElse("npm");
        PackageManager packageManager = new PackageManager(packageManagerBinary, srcUIDirBuildItem.get().getDirectory());
        if (quinoaConfig.alwaysInstallPackages
                .orElse(!Files.isDirectory(srcUIDirBuildItem.get().getDirectory().resolve("node_modules")))) {
            if (!packageManager
                    .install(quinoaConfig.frozenLockfile.orElseGet(() -> Objects.equals(System.getenv("CI"), "true")))) {
                throw new RuntimeException("Error while installing the UI");
            }
        }
        if (quinoaConfig.runUITests.orElse(false)) {
            if (!packageManager.test()) {
                throw new RuntimeException("Error while testing the UI");
            }
        }
        if (!packageManager.build()) {
            throw new RuntimeException("Error while building the UI");
        }
        final Path buildDir = srcUIDirBuildItem.get().getDirectory().resolve(quinoaConfig.buildDir.orElse("build"));
        if (!Files.isDirectory(buildDir)) {
            throw new ConfigurationException("Invalid UI build directory: '" + buildDir + "'");
        }
        final Path targetBuildDir = outputTargetBuildItem.getOutputDirectory().resolve(TARGET_DIR_NAME);
        FileUtil.deleteDirectory(targetBuildDir);
        Files.move(buildDir, targetBuildDir);
        liveReloadBuildItem.setContextObject(QuinoaLiveContext.class, new QuinoaLiveContext(targetBuildDir));
        return new UIBuildOutcomeBuildItem(targetBuildDir);
    }

    @BuildStep(onlyIf = IsNormal.class)
    public UIResourcesBuildItem prepareResourcesForNormalMode(
            Optional<UIBuildOutcomeBuildItem> uiBuildOutcomeBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) throws IOException {
        if (!uiBuildOutcomeBuildItem.isPresent()) {
            return null;
        }
        return new UIResourcesBuildItem(getUIEntries(generatedResources, uiBuildOutcomeBuildItem.get().getUiBuildDirectory()));
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    public UIResourcesBuildItem prepareResourcesForOtherMode(
            Optional<UIBuildOutcomeBuildItem> uiBuildOutcomeBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) throws IOException {
        if (!uiBuildOutcomeBuildItem.isPresent()) {
            return null;
        }
        final HashSet<UIResourcesBuildItem.UIEntry> entries = getUIEntries(generatedResources,
                uiBuildOutcomeBuildItem.get().getUiBuildDirectory());
        return new UIResourcesBuildItem(uiBuildOutcomeBuildItem.get().getUiBuildDirectory(), entries);
    }

    @BuildStep
    void watchChanges(
            QuinoaConfig quinoaConfig,
            Optional<SrcUIDirBuildItem> srcUIDirBuildItem,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths) throws IOException {
        if (!srcUIDirBuildItem.isPresent()) {
            return;
        }
        scan(srcUIDirBuildItem.get().getDirectory(), watchedPaths);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(
            Optional<UIResourcesBuildItem> uiResources,
            QuinoaRecorder recorder,
            CoreVertxBuildItem vertx, BeanContainerBuildItem beanContainer,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes) throws IOException {
        if (uiResources.isPresent()) {
            String directory = null;
            if (uiResources.get().getDirectory().isPresent()) {
                directory = uiResources.get().getDirectory().get().toAbsolutePath().toString();
            }
            defaultRoutes.produce(
                    new DefaultRouteBuildItem(recorder.start(directory,
                            uiResources.get().getNames())));
            // TODO: Handle single page web-app html5 urls by re-routing not found to /
        }
    }

    private HashSet<UIResourcesBuildItem.UIEntry> getUIEntries(
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            Path uiDir) throws IOException {
        final List<Path> files = Files.walk(uiDir).filter(Files::isRegularFile)
                .collect(Collectors.toList());
        final HashSet<UIResourcesBuildItem.UIEntry> entries = new HashSet<>(files.size());
        log.infof("Quinoa Build directory: '%s'", uiDir);
        for (Path file : files) {
            final String name = "/" + uiDir.relativize(file);
            log.infof("Quinoa Generated: '%s'", name);
            generatedResources.produce(new GeneratedResourceBuildItem(META_INF_UI + name, Files.readAllBytes(file), true));
            entries.add(new UIResourcesBuildItem.UIEntry(name));
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
                    log.debugf("Quinoa is watching: %s", filePath);
                    watchedPaths.produce(new HotDeploymentWatchedFileBuildItem(filePath.toString()));
                } else if (Files.isDirectory(filePath) && !IGNORE_WATCH.contains(filePath.getFileName().toString())) {
                    log.debugf("Quinoa is scanning directory: %s", filePath);
                    scan(filePath, watchedPaths);
                }
            }
        }
    }

    private AbstractMap.SimpleEntry<Path, Path> getUIDir(QuinoaConfig quinoaConfig,
            OutputTargetBuildItem outputTargetBuildItem) {
        Map.Entry<Path, Path> mainSourcesRoot = findMainSourcesRoot(outputTargetBuildItem.getOutputDirectory());
        if (mainSourcesRoot == null) {
            return null;
        }
        Path uiRoot = mainSourcesRoot.getKey().resolve(quinoaConfig.uiDir.orElse("ui"));
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
