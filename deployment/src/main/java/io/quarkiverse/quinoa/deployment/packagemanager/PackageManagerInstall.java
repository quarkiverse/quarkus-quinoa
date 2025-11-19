package io.quarkiverse.quinoa.deployment.packagemanager;

import static io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType.isYarnBerry;
import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.PackageManagerInstallFactory;

import io.quarkiverse.quinoa.deployment.config.PackageManagerInstallConfig;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public final class PackageManagerInstall {

    private static final Logger LOG = Logger.getLogger(PackageManagerInstall.class);
    private static final String INSTALL_SUB_PATH = "node";
    public static final String NODE_BINARY = PackageManagerRunner.isWindows() ? "node.exe" : "node";
    public static final String BUN_BINARY = PackageManagerRunner.isWindows() ? "bun.exe" : "bun";
    public static final String NPM_PATH = INSTALL_SUB_PATH + "/node_modules/npm/bin/npm-cli.js";
    public static final String PNPM_PATH = INSTALL_SUB_PATH + "/node_modules/pnpm/bin/pnpm.cjs";
    public static final String YARN_PATH = INSTALL_SUB_PATH + "/yarn/dist/bin/yarn.js";
    public static final String BUN_PATH = "bun/" + BUN_BINARY;

    private PackageManagerInstall() {

    }

    public static Installation install(PackageManagerInstallConfig config,
            final Path projectDir, final Path uiDir,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {
        Path installDir = resolveInstallDir(config, projectDir).normalize();

        // Check if using Bun (which doesn't require Node.js)
        boolean isBunOnly = isBunOnly(config);

        if (!isBunOnly && config.nodeVersion().isEmpty()) {
            throw new ConfigurationException(
                    "node-version is required to install package manager (not required when using only bun-version)",
                    Set.of("quarkus.quinoa.package-manager-install.node-version"));
        }
        if (!isBunOnly && Integer.parseInt(config.nodeVersion().get().split("[.]")[0]) < 4) {
            throw new ConfigurationException("Quinoa is not compatible with Node prior to v4.0.0",
                    Set.of("quarkus.quinoa.package-manager-install.node-version"));
        }
        int i = 0;
        Exception thrown = null;
        Vertx vertx = null;
        try {
            vertx = createVertxInstance();
            PackageManagerInstallFactory factory = new PackageManagerInstallFactory(vertx, uiDir, installDir,
                    config.packageManagerInstallAuth().username().orElse(null),
                    config.packageManagerInstallAuth().password().orElse(null));
            while (i < 5) {
                try {
                    if (i > 0) {
                        LOG.warnf("An error occurred '%s' during the previous Node.js install, retrying (%s/5)",
                                thrown.getCause().getMessage(), i + 1);
                        FileUtil.deleteDirectory(installDir);
                    }
                    return attemptInstall(config, uiDir, installDir, factory, consoleInstalledBuildItem, loggingSetupBuildItem);
                } catch (InstallationException e) {
                    thrown = e;
                    i++;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        } finally {
            Objects.requireNonNull(vertx).close();
        }

        throw new RuntimeException("Error while installing NodeJS", thrown);
    }

    private static Vertx createVertxInstance() {
        String originalValue = System.getProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
        Vertx vertx;
        try {
            System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true");
            vertx = Vertx.vertx(new VertxOptions());
        } finally {
            // Restore the original value
            if (originalValue == null) {
                System.clearProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
            } else {
                System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, originalValue);
            }
        }
        return vertx;
    }

    private static Installation attemptInstall(PackageManagerInstallConfig config, Path uiDir, Path installDir,
            PackageManagerInstallFactory factory,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) throws InstallationException {
        // Check if using Bun only
        boolean isBunOnly = isBunOnly(config);

        // Only install Node.js if not using Bun only
        if (!isBunOnly) {
            StartupLogCompressor nodeInstallerLogCompressor = null;
            try {
                nodeInstallerLogCompressor = new StartupLogCompressor("node installer", consoleInstalledBuildItem,
                        loggingSetupBuildItem);
                factory.getNodeInstaller()
                        .setNodeVersion("v" + config.nodeVersion().orElse("???"))
                        .setNodeDownloadRoot(config.nodeDownloadRoot())
                        .setNpmVersion(config.npmVersion())
                        .install();
                nodeInstallerLogCompressor.close();
            } catch (InstallationException e) {
                nodeInstallerLogCompressor.closeAndDumpCaptured();
                if (e.getCause() instanceof DirectoryNotEmptyException && e.getCause().getMessage().contains("tmp")) {
                    LOG.warnf("Quinoa was not able to delete the Node install temporary directory: %s",
                            e.getCause().getMessage());
                } else {
                    throw e;
                }
            }
        }

        // Use npm if npmVersion is different from provided or if no other version is set (then it will use the version provided by Node.js)
        String executionPath = NPM_PATH;
        final String npmVersion = config.npmVersion();
        boolean isNpmProvided = PackageManagerInstallConfig.NPM_PROVIDED.equalsIgnoreCase(npmVersion);
        if (!isNpmProvided) {
            StartupLogCompressor npmInstallerLogCompressor = null;
            try {
                npmInstallerLogCompressor = new StartupLogCompressor("npm installer", consoleInstalledBuildItem,
                loggingSetupBuildItem);
                factory.getNPMInstaller()
                        .setNodeVersion("v" + config.nodeVersion().orElse("???"))
                        .setNpmVersion(npmVersion)
                        .setNpmDownloadRoot(config.npmDownloadRoot())
                        .install();
                npmInstallerLogCompressor.close();
            } catch (InstallationException e) {
                npmInstallerLogCompressor.closeAndDumpCaptured();
                throw e;
            }
        }

        // Use yarn if yarnVersion is set (and npm is provided)
        final Optional<String> yarnVersion = config.yarnVersion();
        if (yarnVersion.isPresent() && isNpmProvided) {
            executionPath = YARN_PATH;
            factory.getYarnInstaller()
                    .setIsYarnBerry(isYarnBerry(uiDir))
                    .setYarnVersion("v" + config.yarnVersion().orElse("???"))
                    .setYarnDownloadRoot(config.yarnDownloadRoot())
                    .setIsYarnBerry(true)
                    .install();
        }

        // Use pnpm if pnpmVersion is set (and npm is provided and yarnVersion is not set)
        final Optional<String> pnpmVersion = config.pnpmVersion();
        if (pnpmVersion.isPresent() && isNpmProvided && yarnVersion.isEmpty()) {
            executionPath = PNPM_PATH;
            factory.getPnpmInstaller()
                    .setNodeVersion("v" + config.nodeVersion().orElse("???"))
                    .setPnpmVersion(pnpmVersion.get())
                    .setPnpmDownloadRoot(config.pnpmDownloadRoot())
                    .install();
        }

        // Use bun if bunVersion is set (and npm is provided and nodeVersion, yarnVersion and pnpmVersion are not set)
        if (isBunOnly) {
            executionPath = BUN_PATH;
            factory.getBunInstaller()
                    .setBunVersion("v" + config.bunVersion().orElse("???"))
                    .install();
        }

        return resolveInstalledExecutorBinary(installDir, executionPath, isBunOnly);
    }

    private static Path resolveInstallDir(PackageManagerInstallConfig config, Path projectDir) {
        final Path installPath = Path.of(config.installDir().trim());
        if (installPath.isAbsolute()) {
            return installPath;
        }
        if (projectDir == null) {
            throw new ConfigurationException(
                    "Use an absolute package-manager-install.install-dir when the project root directory is not standard",
                    Set.of("quarkus.quinoa.package-manager-install.install-dir"));
        }
        return projectDir.resolve(installPath);
    }

    private static Installation resolveInstalledExecutorBinary(Path installDirectory, String executionPath, boolean isBunOnly) {
        final Path nodeDirPath = installDirectory.resolve(!isBunOnly ? INSTALL_SUB_PATH : "").toAbsolutePath();
        final Path executorPath = installDirectory.resolve(executionPath).toAbsolutePath();
        final String platformNodeDirPath = normalizePath(nodeDirPath.toString());
        final String platformExecutorPath = normalizePath(executorPath.toString());

        // If using Bun, the binary IS the executor (no need to prepend node)
        final String packageManagerBinary;
        if (isBunOnly) {
            packageManagerBinary = quotePathWithSpaces(platformExecutorPath);
        } else {
            packageManagerBinary = NODE_BINARY + " " + quotePathWithSpaces(platformExecutorPath);
        }

        return new Installation(platformNodeDirPath, packageManagerBinary);
    }

    public static String normalizePath(String path) {
        return PackageManagerRunner.isWindows() ? path.replaceAll("/", "\\\\") : path;
    }

    public static String quotePathWithSpaces(String path) {
        return path.contains(" ") ? "\"".concat(path).concat("\"") : path;
    }

    private static boolean isBunOnly(PackageManagerInstallConfig config) {
        final Optional<String> nodeVersion = config.nodeVersion();
        final Optional<String> bunVersion = config.bunVersion();
        final Optional<String> yarnVersion = config.yarnVersion();
        final Optional<String> pnpmVersion = config.pnpmVersion();
        final String npmVersion = config.npmVersion();
        boolean isNpmProvided = PackageManagerInstallConfig.NPM_PROVIDED.equalsIgnoreCase(npmVersion);
        return bunVersion.isPresent() && isNpmProvided && yarnVersion.isEmpty() && pnpmVersion.isEmpty() && nodeVersion.isEmpty();
    }

    public record Installation(String nodeDirPath, String packageManagerBinary) {
    }

}
