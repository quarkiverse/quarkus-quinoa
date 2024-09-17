package io.quarkiverse.quinoa.deployment.packagemanager;

import static io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType.detectPackageManagerType;
import static io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType.resolveConfiguredPackageManagerType;
import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.QuinoaNetworkConfiguration;
import io.quarkiverse.quinoa.deployment.SslUtil;
import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManager;
import io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.LaunchMode;

public class PackageManagerRunner {
    private static final Logger LOG = Logger.getLogger(PackageManagerRunner.class);
    public static final Predicate<Thread> DEV_PROCESS_THREAD_PREDICATE = thread -> thread.getName()
            .matches("Process (stdout|stderr) streamer");

    private final Path directory;

    private final PackageManager packageManager;
    private final Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem;
    private final LoggingSetupBuildItem loggingSetupBuildItem;

    private PackageManagerRunner(Path directory, PackageManager packageManager,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {
        this.directory = directory;
        this.packageManager = packageManager;
        this.consoleInstalledBuildItem = consoleInstalledBuildItem;
        this.loggingSetupBuildItem = loggingSetupBuildItem;
    }

    public Path getDirectory() {
        return directory;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public void ci() {
        final PackageManager.Command ci = packageManager.ci();
        LOG.infof("Running Quinoa package manager ci command: %s", ci.commandWithArguments);
        if (!exec(ci)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager ci command: %s", ci.commandWithArguments));
        }
    }

    public void install() {
        final PackageManager.Command install = packageManager.install();
        LOG.infof("Running Quinoa package manager install command: %s", install.commandWithArguments);
        if (!exec(install)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager install command: %s", install.commandWithArguments));
        }
    }

    public void build(LaunchMode mode) {
        final PackageManager.Command build = packageManager.build(mode);
        LOG.infof("Running Quinoa package manager build command: %s", build.commandWithArguments);
        if (!exec(build)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager build command: %s", build.commandWithArguments));
        }
    }

    public void test() {
        final PackageManager.Command test = packageManager.test();
        LOG.infof("Running Quinoa package manager test command: %s", test.commandWithArguments);
        if (!exec(test)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager test command: %s", test.commandWithArguments));
        }
    }

    public void publish() {
        final PackageManager.Command publish = packageManager.publish();
        LOG.infof("Running Quinoa package manager publish command: %s", publish.commandWithArguments);
        if (!exec(publish)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager publish command: %s", publish.commandWithArguments));
        }
    }

    public void stopDev(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        LOG.infof("Stopping Quinoa package manager live coding as a dev service.");
        try {
            // Kill children before because React is swallowing the signal
            killDescendants(process.toHandle(), false);
            if (process.isAlive()) {
                process.destroy();
                // Force kill descendants if needed
                killDescendants(process.toHandle(), true);
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.errorf(e, "Error while waiting for Quinoa Dev Server process (#%s) to exit.", process.pid());
        } finally {
            if (process.isAlive()) {
                LOG.warnf("Quinoa was not able to stop the Dev Server process (#%s).", process.pid());
            }
        }

    }

    private static void killDescendants(ProcessHandle process, boolean force) {
        process.children().forEach(child -> {
            killDescendants(child, force);
            if (child.isAlive()) {
                if (force) {
                    child.destroyForcibly();
                } else {
                    child.destroy();
                }
            }
        });
    }

    public DevServer dev(Optional<ConsoleInstalledBuildItem> consoleInstalled, LoggingSetupBuildItem loggingSetup,
            QuinoaNetworkConfiguration network, String checkPath,
            int checkTimeout) {
        final PackageManager.Command dev = packageManager.dev();
        LOG.infof("Running Quinoa package manager live coding as a dev service: %s", dev.commandWithArguments);
        StartupLogCompressor logCompressor = new StartupLogCompressor(
                "Quinoa package manager live coding dev service starting:",
                consoleInstalled,
                loggingSetup,
                DEV_PROCESS_THREAD_PREDICATE);
        Process p = process(dev);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                stopDev(p);
            }
        });
        if (checkPath == null) {
            LOG.infof("Quinoa is configured to continue without check if the live coding server is up");
            return new DevServer(p, network.getHost(), logCompressor);
        }
        String ipAddress = null;
        try {
            int i = 0;
            while ((ipAddress = isDevServerUp(network, checkPath)) == null) {
                if (++i >= checkTimeout / 500) {
                    stopDev(p);
                    throw new RuntimeException(
                            "Quinoa package manager live coding port " + network.getPort()
                                    + " is still not listening after the checkTimeout.");
                }
                Thread.sleep(500);
            }
            // Add a small safety delay to make sure all logs are outputted
            Thread.sleep(500);
        } catch (InterruptedException e) {
            stopDev(p);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return new DevServer(p, ipAddress, logCompressor);
    }

    public static PackageManagerRunner autoDetectPackageManager(Optional<String> configuredBinary,
            PackageManagerCommandConfig packageManagerCommands, Path directory, List<String> paths,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {
        String binary;
        PackageManagerType type = detectPackageManagerType(directory);
        if (configuredBinary.isEmpty()) {
            binary = type.getOSBinary();
        } else {
            binary = configuredBinary.get();
            type = resolveConfiguredPackageManagerType(binary, type);
        }
        return new PackageManagerRunner(directory, PackageManager.resolve(type, binary, packageManagerCommands, paths),
                consoleInstalledBuildItem, loggingSetupBuildItem);
    }

    public static boolean isWindows() {
        return QuarkusConsole.IS_WINDOWS;
    }

    private Process process(PackageManager.Command command) {
        Process process = null;
        final ProcessBuilder builder = new ProcessBuilder()
                .directory(directory.toFile())
                .command(runner(command));
        if (!command.envs.isEmpty()) {
            builder.environment().putAll(command.envs);
        }
        try {
            process = ProcessUtil.launchProcess(builder, true);
        } catch (IOException e) {
            throw new RuntimeException("Input/Output error while running process.", e);
        }
        return process;
    }

    private boolean exec(PackageManager.Command command) {
        Process process = null;
        HandleOutput handleOutput = null;
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder();
            if (!command.envs.isEmpty()) {
                processBuilder.environment().putAll(command.envs);
            }
            process = processBuilder
                    .directory(directory.toFile())
                    .command(runner(command))
                    .redirectErrorStream(true)
                    .start();
            handleOutput = new HandleOutput(process.getInputStream(), consoleInstalledBuildItem, loggingSetupBuildItem);
            handleOutput.run();
            process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("Input/Output error while executing command.", e);
        } catch (InterruptedException e) {
            return false;
        } finally {
            if (handleOutput != null) {
                handleOutput.close();
            }
        }
        return process != null && process.exitValue() == 0;
    }

    private String[] runner(PackageManager.Command command) {
        if (isWindows()) {
            return new String[] { "cmd.exe", "/c", command.commandWithArguments };
        } else {
            return new String[] { "sh", "-c", command.commandWithArguments };
        }
    }

    private static class HandleOutput implements Runnable, Closeable {

        private final InputStream is;
        private final Logger.Level logLevel;
        private final StartupLogCompressor logCompressor;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        HandleOutput(InputStream is,
                Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
                LoggingSetupBuildItem loggingSetupBuildItem) {
            this(is, Logger.Level.INFO, consoleInstalledBuildItem, loggingSetupBuildItem);
        }

        HandleOutput(InputStream is, Logger.Level logLevel,
                Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
                LoggingSetupBuildItem loggingSetupBuildItem) {
            this.is = is;
            this.logLevel = LOG.isEnabled(logLevel) ? logLevel : null;
            logCompressor = new StartupLogCompressor("quinoa", consoleInstalledBuildItem, loggingSetupBuildItem);
        }

        @Override
        public void run() {
            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (logLevel != null) {
                        LOG.log(logLevel, line);
                    }
                }
            } catch (IOException e) {
                if (logLevel != null) {
                    LOG.log(logLevel, "Failed to handle output", e);
                }
                closed.set(true);
                logCompressor.closeAndDumpCaptured();
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                logCompressor.close();
            }
        }
    }

    public static String isDevServerUp(QuinoaNetworkConfiguration network, String path) {
        if (path == null) {
            return network.getHost();
        }
        final String normalizedPath = path.indexOf("/") == 0 ? path : "/" + path;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(network.getHost());
            for (InetAddress address : addresses) {
                try {
                    final String hostAddress = address.getHostAddress();
                    final String ipAddress = address instanceof Inet6Address ? "[" + hostAddress + "]" : hostAddress;
                    URL url = new URL(String.format("%s://%s:%d%s", network.isTls() ? "https" : "http", ipAddress,
                            network.getPort(), normalizedPath));
                    HttpURLConnection connection;
                    if (network.isTls()) {
                        HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
                        if (network.isTlsAllowInsecure()) {
                            httpsConnection.setSSLSocketFactory(SslUtil.createNonValidatingSslContext().getSocketFactory());
                            httpsConnection.setHostnameVerifier(new HostnameVerifier() {
                                @Override
                                public boolean verify(String hostname, SSLSession session) {
                                    return true;
                                }
                            });
                        }
                        connection = httpsConnection;
                    } else {
                        connection = (HttpURLConnection) url.openConnection();
                    }

                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    connection.connect();
                    int code = connection.getResponseCode();
                    // in both cases the server is started, for 404 it might be started on another path
                    return (code == 200 || code == 404) ? ipAddress : null;
                } catch (ConnectException | SocketTimeoutException e) {
                    // Try the next address
                } catch (IOException e) {
                    throw new RuntimeException("Error while checking if package manager dev server is up", e);
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static class DevServer {
        private final Process process;
        private final String hostIPAddress;

        private final StartupLogCompressor logCompressor;

        public DevServer(Process process, String hostIPAddress, StartupLogCompressor logCompressor) {
            this.process = process;
            this.hostIPAddress = hostIPAddress;
            this.logCompressor = logCompressor;
        }

        public Process process() {
            return process;
        }

        public String hostIPAddress() {
            return hostIPAddress;
        }

        public StartupLogCompressor logCompressor() {
            return logCompressor;
        }
    }
}
