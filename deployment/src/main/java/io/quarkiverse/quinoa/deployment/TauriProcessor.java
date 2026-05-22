package io.quarkiverse.quinoa.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.TauriConfig;
import io.quarkiverse.quinoa.deployment.config.TauriConfig.ExportTarget;
import io.quarkiverse.quinoa.deployment.items.ConfiguredQuinoaBuildItem;
import io.quarkiverse.quinoa.deployment.items.InstalledPackageManagerBuildItem;
import io.quarkiverse.quinoa.deployment.items.TargetDirBuildItem;
import io.quarkiverse.quinoa.deployment.items.TauriBuildItem;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.smallrye.common.os.OS;

public class TauriProcessor {

    private static final Logger LOG = Logger.getLogger(TauriProcessor.class);

    @BuildStep
    public TauriBuildItem prepareTauriBuild(
            ConfiguredQuinoaBuildItem configuredQuinoa,
            QuinoaConfig config,
            OutputTargetBuildItem outputTarget,
            LaunchModeBuildItem launchMode) {
        if (configuredQuinoa == null) {
            return null;
        }

        TauriConfig tauriConfig = configuredQuinoa.resolvedConfig().tauri();
        if (!tauriConfig.enabled()) {
            return null;
        }

        Path projectDir = configuredQuinoa.projectDir();
        Path uiDir = configuredQuinoa.uiDir();
        String tauriDirName = tauriConfig.dir().orElse(TauriConfig.DEFAULT_TAURI_DIR);

        Path tauriDir = projectDir.resolve(tauriDirName);
        if (!Files.isDirectory(tauriDir)) {
            tauriDir = uiDir.resolve(tauriDirName);
        }

        if (!Files.isDirectory(tauriDir)) {
            LOG.warnf(
                    "Tauri integration is enabled but the Tauri directory '%s' was not found. "
                            + "Make sure to initialize Tauri in your project (e.g., `npm create tauri-app@latest` or `cargo tauri init`).",
                    tauriDirName);
            return null;
        }

        if (!Files.isRegularFile(tauriDir.resolve("tauri.conf.json"))) {
            LOG.warnf(
                    "Tauri directory '%s' found but no tauri.conf.json found. "
                            + "The Tauri project might not be properly initialized.",
                    tauriDir.toAbsolutePath());
            return null;
        }

        List<ExportTarget> exportTargets = resolveExportTargets(tauriConfig);
        LOG.infof("Quinoa Tauri integration enabled. Export targets: %s", exportTargets);

        return new TauriBuildItem(tauriDir, projectDir, uiDir, exportTargets, tauriConfig);
    }

    @BuildStep(onlyIf = IsNormal.class)
    @Consume(TargetDirBuildItem.class)
    @Produce(ArtifactResultBuildItem.class)
    public void processTauriBuild(
            TauriBuildItem tauriBuild,
            ConfiguredQuinoaBuildItem configuredQuinoa,
            InstalledPackageManagerBuildItem installedPackageManager,
            OutputTargetBuildItem outputTarget) throws IOException {
        if (tauriBuild == null) {
            return;
        }

        TauriConfig tauriConfig = tauriBuild.tauriConfig();
        List<ExportTarget> exportTargets = tauriBuild.exportTargets();

        if (tauriConfig.buildNativeImage()) {
            copyNativeImageSidecar(tauriBuild, outputTarget);
        }

        for (ExportTarget target : exportTargets) {
            switch (target) {
                case DESKTOP -> buildTauriDesktop(tauriBuild, installedPackageManager, outputTarget);
                case IOS -> buildTauriIOS(tauriBuild, installedPackageManager, outputTarget);
                case ANDROID -> buildTauriAndroid(tauriBuild, installedPackageManager, outputTarget);
                case WEB -> LOG.info("Tauri WEB export target: static web files are already built by Quinoa.");
            }
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Produce(ArtifactResultBuildItem.class)
    void watchTauriChanges(
            Optional<TauriBuildItem> tauriBuild) {
        if (tauriBuild.isPresent()) {
            LOG.info("Quinoa Tauri dev mode: use 'cargo tauri dev' from the Tauri directory for live development.");
        }
    }

    List<ExportTarget> resolveExportTargets(TauriConfig tauriConfig) {
        Optional<List<String>> configured = tauriConfig.export();
        if (configured.isPresent() && !configured.get().isEmpty()) {
            List<ExportTarget> targets = new ArrayList<>();
            for (String target : configured.get()) {
                try {
                    targets.add(ExportTarget.valueOf(target.toUpperCase().trim()));
                } catch (IllegalArgumentException e) {
                    LOG.warnf("Unknown Tauri export target '%s'. Valid values: web, desktop, ios, android", target);
                }
            }
            if (!targets.isEmpty()) {
                return targets;
            }
        }

        List<ExportTarget> defaults = new ArrayList<>();
        defaults.add(ExportTarget.WEB);
        defaults.add(ExportTarget.DESKTOP);
        return defaults;
    }

    void copyNativeImageSidecar(TauriBuildItem tauriBuild, OutputTargetBuildItem outputTarget) throws IOException {
        TauriConfig tauriConfig = tauriBuild.tauriConfig();
        String sidecarName = tauriConfig.sidecarName();
        Path binariesDir = tauriBuild.tauriDir().resolve("binaries");

        Path nativeBinary = resolveNativeImageBinary(tauriConfig, outputTarget);
        if (nativeBinary == null || !Files.isRegularFile(nativeBinary)) {
            LOG.warnf(
                    "GraalVM native image not found. Build the native image first with: mvn package -Pnative "
                            + "Then the native image will be bundled as a Tauri sidecar named '%s'.",
                    sidecarName);
            return;
        }

        Files.createDirectories(binariesDir);

        String targetTriple = getHostTargetTriple();
        if (targetTriple == null) {
            LOG.warn("Could not determine Rust target triple. Sidecar binary will not be copied.");
            return;
        }

        String sidecarFileName = sidecarName + "-" + targetTriple;
        if (OS.WINDOWS.isCurrent()) {
            sidecarFileName += ".exe";
        }

        Path sidecarPath = binariesDir.resolve(sidecarFileName);
        Files.copy(nativeBinary, sidecarPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        if (!OS.WINDOWS.isCurrent()) {
            try {
                nativeBinary.toFile().setExecutable(true);
                sidecarPath.toFile().setExecutable(true);
            } catch (SecurityException e) {
                LOG.debug("Could not set executable permission on sidecar binary", e);
            }
        }

        LOG.infof("Quinoa Tauri: copied native image to sidecar '%s'", sidecarPath);

        updateTauriConfSidecar(tauriBuild.tauriDir(), sidecarName);
    }

    Path resolveNativeImageBinary(TauriConfig tauriConfig, OutputTargetBuildItem outputTarget) {
        Optional<String> configuredBinary = tauriConfig.nativeImageBinary();
        if (configuredBinary.isPresent()) {
            Path binaryPath = Path.of(configuredBinary.get());
            if (binaryPath.isAbsolute()) {
                return binaryPath;
            }
            return outputTarget.getOutputDirectory().resolve(binaryPath);
        }

        Path targetDir = outputTarget.getOutputDirectory();
        try (Stream<Path> files = Files.list(targetDir)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                String name = file.getFileName().toString();
                if (name.endsWith("-runner") || (OS.WINDOWS.isCurrent() && name.endsWith("-runner.exe"))) {
                    return file;
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to list target directory for native image", e);
        }

        return null;
    }

    void buildTauriDesktop(TauriBuildItem tauriBuild,
            InstalledPackageManagerBuildItem installedPackageManager,
            OutputTargetBuildItem outputTarget) {
        LOG.info("Quinoa Tauri: building desktop application...");
        List<String> args = buildTauriCommand(tauriBuild, "build");
        execTauri(tauriBuild.uiDir(), args, installedPackageManager);
        LOG.info("Quinoa Tauri: desktop build complete.");
    }

    void buildTauriIOS(TauriBuildItem tauriBuild,
            InstalledPackageManagerBuildItem installedPackageManager,
            OutputTargetBuildItem outputTarget) {
        LOG.info("Quinoa Tauri: building iOS application...");
        if (!OS.MAC.isCurrent()) {
            LOG.warn("Tauri iOS builds require macOS with Xcode installed. Skipping iOS build.");
            return;
        }
        List<String> args = buildTauriCommand(tauriBuild, "ios", "build");
        execTauri(tauriBuild.uiDir(), args, installedPackageManager);
        LOG.info("Quinoa Tauri: iOS build complete.");
    }

    void buildTauriAndroid(TauriBuildItem tauriBuild,
            InstalledPackageManagerBuildItem installedPackageManager,
            OutputTargetBuildItem outputTarget) {
        LOG.info("Quinoa Tauri: building Android application...");
        List<String> args = buildTauriCommand(tauriBuild, "android", "build");
        execTauri(tauriBuild.uiDir(), args, installedPackageManager);
        LOG.info("Quinoa Tauri: Android build complete.");
    }

    List<String> buildTauriCommand(TauriBuildItem tauriBuild, String... subcommands) {
        List<String> command = new ArrayList<>();
        command.add("tauri");
        for (String sub : subcommands) {
            command.add(sub);
        }

        TauriConfig tauriConfig = tauriBuild.tauriConfig();

        if (tauriConfig.verbose()) {
            command.add("--verbose");
        }

        if (tauriConfig.buildConfig().isPresent()) {
            command.add("--config");
            command.add(tauriConfig.buildConfig().get());
        }

        if (tauriConfig.buildArgs().isPresent()) {
            command.addAll(tauriConfig.buildArgs().get());
        }

        return command;
    }

    void execTauri(Path workingDir, List<String> tauriArgs,
            InstalledPackageManagerBuildItem installedPackageManager) {
        List<String> fullCommand = resolveTauriCommand(installedPackageManager, tauriArgs);

        LOG.infof("Running Tauri command: %s", String.join(" ", fullCommand));

        try {
            ProcessBuilder pb = new ProcessBuilder()
                    .directory(workingDir.toFile())
                    .command(fullCommand)
                    .redirectErrorStream(true);

            Process process = pb.start();
            try (InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(
                        String.format("Tauri command failed with exit code %d: %s", exitCode, String.join(" ", fullCommand)));
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while running Tauri command: " + String.join(" ", fullCommand), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while running Tauri command", e);
        }
    }

    List<String> resolveTauriCommand(InstalledPackageManagerBuildItem installedPackageManager, List<String> tauriArgs) {
        List<String> fullCommand = new ArrayList<>();
        if (installedPackageManager != null) {
            PackageManagerRunner pmRunner = installedPackageManager.getPackageManager();
            String npmBinary = pmRunner.getPackageManager().binary();
            fullCommand.add(npmBinary);
            fullCommand.add("run");
            fullCommand.add("tauri");
            fullCommand.add("--");
        } else {
            fullCommand.add("cargo");
            fullCommand.add("tauri");
        }
        fullCommand.addAll(tauriArgs);
        return fullCommand;
    }

    String getHostTargetTriple() {
        try {
            ProcessBuilder pb = new ProcessBuilder("rustc", "--print", "host-tuple");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String triple = reader.readLine();
                process.waitFor();
                if (triple != null && !triple.isBlank()) {
                    return triple.trim();
                }
            }
        } catch (IOException e) {
            LOG.debug("rustc not found, cannot determine host target triple", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (OS.LINUX.isCurrent()) {
            return "x86_64-unknown-linux-gnu";
        } else if (OS.MAC.isCurrent()) {
            return "aarch64-apple-darwin";
        } else if (OS.WINDOWS.isCurrent()) {
            return "x86_64-pc-windows-msvc";
        }
        return null;
    }

    void updateTauriConfSidecar(Path tauriDir, String sidecarName) {
        Path confPath = tauriDir.resolve("tauri.conf.json");
        if (!Files.isRegularFile(confPath)) {
            LOG.debug("tauri.conf.json not found, skipping sidecar configuration update");
            return;
        }

        try {
            String content = Files.readString(confPath);
            String sidecarEntry = "\"binaries/" + sidecarName + "\"";
            if (content.contains(sidecarEntry)) {
                LOG.debug("Sidecar already configured in tauri.conf.json");
                return;
            }

            if (content.contains("\"externalBin\"")) {
                String existingPattern = "\"externalBin\":\\s*\\[";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(existingPattern);
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    int insertPos = matcher.end();
                    content = content.substring(0, insertPos) + "\n    " + sidecarEntry + "," + content.substring(insertPos);
                    Files.writeString(confPath, content);
                    LOG.infof("Quinoa Tauri: added sidecar entry to existing externalBin in tauri.conf.json");
                }
            } else if (content.contains("\"bundle\"")) {
                String sidecarJson = ",\n      \"externalBin\": [\n        " + sidecarEntry + "\n      ]";
                int bundleIdx = content.indexOf("\"bundle\"");
                int braceIdx = content.indexOf('{', bundleIdx);
                if (braceIdx > 0) {
                    content = content.substring(0, braceIdx + 1) + sidecarJson + content.substring(braceIdx + 1);
                    Files.writeString(confPath, content);
                    LOG.infof("Quinoa Tauri: added externalBin config with sidecar to tauri.conf.json");
                }
            }
        } catch (IOException e) {
            LOG.warnf("Failed to update tauri.conf.json with sidecar configuration: %s", e.getMessage());
        }
    }
}
