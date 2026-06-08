package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_SPA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.getNormalizedIgnoredPathPrefixes;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.isDevServerMode;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.isEnabled;
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
import io.quarkiverse.quinoa.deployment.items.PublishedPackageBuildItem;
import io.quarkiverse.quinoa.deployment.items.TargetDirBuildItem;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner;
import io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType;
import io.quarkiverse.quinoa.deployment.sbom.CdxgenRunner;
import io.quarkiverse.quinoa.deployment.sbom.CycloneDxBomParser;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.sbom.SbomContributionBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.sbom.SbomContribution;
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

        final QuinoaConfig resolvedConfig = overrideConfig(launchMode, userConfig, packageJson, projectDirs.uiDir);
        if (resolvedConfig.enableSPARouting() && resolvedConfig.enableSSRMode()) {
            throw new ConfigurationException(
                    "Quinoa cannot have both 'enable-spa-routing' and 'enable-ssr-mode' enabled. " +
                            "Use 'enable-ssr-mode' for SSR frameworks like Next.js, or 'enable-spa-routing' for traditional SPAs.");
        }

        return new ConfiguredQuinoaBuildItem(projectDirs.projectRootDir, projectDirs.uiDir, packageJson, resolvedConfig);
    }

    @BuildStep
    public InstalledPackageManagerBuildItem install(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            LiveReloadBuildItem liveReload,
            OutputTargetBuildItem outputTarget,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) throws IOException {
        if (configuredQuinoa != null) {
            final QuinoaConfig resolvedConfig = configuredQuinoa.resolvedConfig();
            Optional<String> packageManagerBinary = resolvedConfig.packageManager();
            List<String> paths = new ArrayList<>();
            if (resolvedConfig.packageManagerInstall().enabled()) {
                final PackageManagerInstall.Installation result = PackageManagerInstall.install(
                        resolvedConfig.packageManagerInstall(),
                        configuredQuinoa.projectDir(),
                        configuredQuinoa.uiDir(),
                        consoleInstalledBuildItem,
                        loggingSetupBuildItem);
                packageManagerBinary = Optional.of(result.packageManagerBinary());
                paths.add(result.nodeDirPath());
            }

            final PackageManagerRunner packageManagerRunner = autoDetectPackageManager(packageManagerBinary,
                    resolvedConfig.packageManagerShell(),
                    resolvedConfig.packageManagerCommand(), configuredQuinoa.uiDir(), paths, consoleInstalledBuildItem,
                    loggingSetupBuildItem);
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
        if (configuredQuinoa.resolvedConfig().enableSSRMode() && !configuredQuinoa.resolvedConfig().justBuild()) {
            LOG.warn("Quinoa SSR mode is enabled in a non-dev-server build. "
                    + "The build output will NOT be served as static files by Quarkus. "
                    + "SSR frameworks like Next.js require a Node.js server (e.g. 'next start') to serve pages at runtime. "
                    + "Consider using 'quarkus.quinoa.just-build=true' and deploying the Node.js server separately.");
        }
        if (liveReload.isLiveReload()
                && liveReload.getChangedResources().stream()
                        .noneMatch(r -> r.startsWith(packageManagerRunner.getDirectory().toString()))
                && contextObject != null) {
            return new TargetDirBuildItem(contextObject.location());
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

        // doesn't make sense to copy from ui build dir to quinoa target dir
        // in case of `just-build` option enabled
        if (configuredQuinoa.resolvedConfig().justBuild()) {
            return new TargetDirBuildItem(buildDir);
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
    public PublishedPackageBuildItem publishBuiltPackage(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            InstalledPackageManagerBuildItem installedPackageManager,
            Optional<TargetDirBuildItem> targetDir) {
        if (configuredQuinoa == null || !configuredQuinoa.resolvedConfig().publish()) {
            return new PublishedPackageBuildItem(true);
        }

        if (targetDir.isEmpty()) {
            LOG.warn("Target dir is empty - unable to run publish command.");
            return new PublishedPackageBuildItem(true);
        }

        final PackageManagerRunner packageManagerRunner = installedPackageManager.getPackageManager();
        packageManagerRunner.publish();
        return new PublishedPackageBuildItem(false);
    }

    @BuildStep
    @Consume(PublishedPackageBuildItem.class) // just to order build steps
    public BuiltResourcesBuildItem prepareBuiltResources(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            Optional<TargetDirBuildItem> targetDir) throws IOException {
        if (targetDir.isEmpty()) {
            return null;
        }
        if (configuredQuinoa != null && configuredQuinoa.resolvedConfig().justBuild()) {
            // no need to configure vertx static resources when `just-build` activated
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
            Optional<BuiltResourcesBuildItem> uiResources) {
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
            if (Objects.requireNonNull(configuredQuinoa).resolvedConfig().enableSPARouting()) {
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
            LOG.infof("Quinoa target directory: '%s' containing %d resources", targetDir, files.size());
            for (Path file : files) {
                final String name = "/" + targetDir.relativize(file).toString().replace('\\', '/');
                LOG.debugf("Quinoa generated resource: '%s'", name);
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

    private record QuinoaLiveContext(Path location) {
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

    /**
     * Produces an SBOM contribution build item by running cdxgen to generate
     * a CycloneDX SBOM and parsing the result into component descriptors.
     * <p>
     * Development dependencies are included or excluded based on the SBOM configuration
     * and the current launch mode. By default, dev dependencies are included in
     * development mode and excluded in production builds.
     *
     * @param configured the configured Quinoa build item, if present
     * @param installed the installed package manager build item, if present
     * @param launchMode the current launch mode
     * @param config the Quinoa configuration
     * @param sbomProducer the producer for SBOM contribution build items
     */
    @BuildStep
    void produceSbomContribution(
            Optional<ConfiguredQuinoaBuildItem> configured,
            Optional<InstalledPackageManagerBuildItem> installed,
            LaunchModeBuildItem launchMode,
            OutputTargetBuildItem outputTarget,
            QuinoaConfig config,
            BuildProducer<SbomContributionBuildItem> sbomProducer) {
        if (configured.isEmpty() || installed.isEmpty()) {
            return;
        }
        if (!config.sbom().enabled()) {
            return;
        }

        final PackageManagerRunner packageManagerRunner = installed.get().getPackageManager();
        final Path uiDir = configured.get().uiDir();
        final Path packageJson = configured.get().packageJson();
        final List<String> paths = packageManagerRunner.getPaths();

        final Path sbomCacheDir = outputTarget.getOutputDirectory().resolve(TARGET_DIR_NAME).resolve("cdxgen");
        final Path cachedSbom = sbomCacheDir.resolve("sbom-cdxgen.json");
        final Path cachedPackageJson = sbomCacheDir.resolve("sbom-package.json");
        final Path lockFile = uiDir.resolve(packageManagerRunner.getType().getLockFile());
        final Path cachedLockFile = sbomCacheDir.resolve("sbom-lockfile");
        final Path cachedConfigFingerprint = sbomCacheDir.resolve("sbom-config");
        final String configFingerprint = sbomConfigFingerprint(config);

        final boolean cacheHit = isSbomCacheValid(packageJson, cachedPackageJson, lockFile, cachedLockFile,
                configFingerprint, cachedConfigFingerprint, cachedSbom);
        if (cacheHit) {
            LOG.info("Reusing cached SBOM (inputs unchanged)");
        } else {
            int timeoutSeconds = config.sbom().timeout();
            if (timeoutSeconds < 1) {
                LOG.warnf("Invalid SBOM timeout %d, using default 300s", timeoutSeconds);
                timeoutSeconds = 300;
            }

            try {
                Files.createDirectories(sbomCacheDir);
                CdxgenRunner.generate(uiDir, paths, packageManagerRunner.getType(),
                        config.sbom().cdxgenVersion(), timeoutSeconds, cachedSbom);
            } catch (Exception e) {
                LOG.warn("Failed to generate SBOM with cdxgen", e);
                return;
            }
        }

        final SbomContribution contribution;
        try {
            contribution = CycloneDxBomParser.parse(cachedSbom, packageJson);
        } catch (Exception e) {
            deleteQuietly(cachedSbom);
            deleteQuietly(cachedPackageJson);
            deleteQuietly(cachedLockFile);
            deleteQuietly(cachedConfigFingerprint);
            throw new RuntimeException("Failed to parse cdxgen SBOM output", e);
        }

        if (!cacheHit) {
            try {
                Files.copy(packageJson, cachedPackageJson, StandardCopyOption.REPLACE_EXISTING);
                if (Files.isRegularFile(lockFile)) {
                    Files.copy(lockFile, cachedLockFile, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    deleteQuietly(cachedLockFile);
                }
                Files.writeString(cachedConfigFingerprint, configFingerprint);
            } catch (IOException e) {
                LOG.debug("Failed to update SBOM cache", e);
            }
        }

        if (contribution.components().isEmpty()) {
            return;
        }

        final SbomContribution result;
        if (shouldIncludeDevDependencies(config, launchMode)) {
            result = contribution;
        } else {
            result = CycloneDxBomParser.excludeDevDependencies(contribution);
        }
        if (!result.components().isEmpty()) {
            sbomProducer.produce(new SbomContributionBuildItem(result));
        }
    }

    /**
     * Checks whether the cached SBOM is still valid by comparing the current
     * package.json, lock file, and SBOM configuration fingerprint against
     * their cached counterparts.
     *
     * @param currentPackageJson path to the current package.json
     * @param cachedPackageJson path to the cached copy of package.json
     * @param currentLockFile path to the current lock file
     * @param cachedLockFile path to the cached copy of the lock file
     * @param configFingerprint the current SBOM configuration fingerprint string
     * @param cachedConfigFingerprint path to the cached configuration fingerprint file
     * @param cachedSbom path to the cached SBOM output file
     * @return {@code true} if all cached inputs match and the cached SBOM exists
     */
    private static boolean isSbomCacheValid(Path currentPackageJson, Path cachedPackageJson,
            Path currentLockFile, Path cachedLockFile,
            String configFingerprint, Path cachedConfigFingerprint,
            Path cachedSbom) {
        try {
            if (!Files.isRegularFile(cachedSbom)) {
                return false;
            }
            if (!filesEqual(currentPackageJson, cachedPackageJson)) {
                return false;
            }
            if (!filesEqual(currentLockFile, cachedLockFile)
                    && (Files.isRegularFile(currentLockFile) || Files.isRegularFile(cachedLockFile))) {
                return false;
            }
            return Files.isRegularFile(cachedConfigFingerprint)
                    && configFingerprint.equals(Files.readString(cachedConfigFingerprint));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Compares two files for equality by checking existence, size, and byte content.
     * Returns {@code false} if either path does not point to a regular file.
     *
     * @param a path to the first file
     * @param b path to the second file
     * @return {@code true} if both files exist, have the same size, and identical content
     * @throws IOException if an I/O error occurs reading the files
     */
    private static boolean filesEqual(Path a, Path b) throws IOException {
        if (!Files.isRegularFile(a) || !Files.isRegularFile(b)) {
            return false;
        }
        if (Files.size(a) != Files.size(b)) {
            return false;
        }
        return Arrays.equals(Files.readAllBytes(a), Files.readAllBytes(b));
    }

    /**
     * Builds a fingerprint string from the SBOM-relevant configuration properties.
     * Used to detect configuration changes that should invalidate the cached SBOM.
     *
     * @param config the Quinoa configuration
     * @return a string representing the current SBOM configuration
     */
    private static String sbomConfigFingerprint(QuinoaConfig config) {
        return config.sbom().cdxgenVersion().orElse("latest")
                + "\n" + config.sbom().timeout();
    }

    /**
     * Determines whether development dependencies should be included in the SBOM.
     * <p>
     * If the configuration explicitly sets {@code includeDevDependencies}, that value is used.
     * Otherwise, dev dependencies are included only in development mode.
     *
     * @param config the Quinoa configuration
     * @param launchMode the current launch mode
     * @return {@code true} if dev dependencies should be included
     */
    private static boolean shouldIncludeDevDependencies(QuinoaConfig config, LaunchModeBuildItem launchMode) {
        return config.sbom().includeDevDependencies()
                .orElse(launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT);
    }

    /**
     * Deletes a file if it exists, silently ignoring any I/O errors.
     *
     * @param path the path to delete
     */
    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

}
