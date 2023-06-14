package io.quarkiverse.quinoa.deployment.packagemanager;

import static java.lang.String.format;

import java.io.BufferedReader;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.LaunchMode;

public class PackageManager {
    private static final Logger LOG = Logger.getLogger(PackageManager.class);

    private final Path directory;

    private final PackageManagerCommands packageManagerCommands;

    private PackageManager(Path directory, PackageManagerCommands packageManagerCommands) {
        this.directory = directory;
        this.packageManagerCommands = packageManagerCommands;
    }

    public Path getDirectory() {
        return directory;
    }

    public PackageManagerCommands getPackageManagerCommands() {
        return packageManagerCommands;
    }

    public void install(boolean frozenLockfile) {
        final Command install = packageManagerCommands.install(frozenLockfile);
        LOG.infof("Running Quinoa package manager install command: %s", install.commandWithArguments);
        if (!exec(install)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager install command: %s", install.commandWithArguments));
        }
    }

    public void build(LaunchMode mode) {
        final Command build = packageManagerCommands.build(mode);
        LOG.infof("Running Quinoa package manager build command: %s", build.commandWithArguments);
        if (!exec(build)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager build command: %s", build.commandWithArguments));
        }
    }

    public void test() {
        final Command test = packageManagerCommands.test();
        LOG.infof("Running Quinoa package manager test command: %s", test.commandWithArguments);
        if (!exec(test)) {
            throw new RuntimeException(
                    format("Error in Quinoa while running package manager test command: %s", test.commandWithArguments));
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

    public DevServer dev(String devServerCommand, String devServerHost, int devServerPort, String checkPath, int checkTimeout) {
        final Command dev = packageManagerCommands.dev(devServerCommand);
        LOG.infof("Running Quinoa package manager live coding as a dev service: %s", dev.commandWithArguments);
        Process p = process(dev);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                stopDev(p);
            }
        });
        if (checkPath == null) {
            LOG.infof("Quinoa is configured to continue without check if the live coding server is up");
            return new DevServer(p, devServerHost);
        }
        String ipAddress = null;
        try {
            int i = 0;
            while ((ipAddress = isDevServerUp(devServerHost, devServerPort, checkPath)) == null) {
                if (++i >= checkTimeout / 500) {
                    stopDev(p);
                    throw new RuntimeException(
                            "Quinoa package manager live coding port " + devServerPort
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
        return new DevServer(p, ipAddress);
    }

    public static PackageManager autoDetectPackageManager(Optional<String> binary,
            PackageManagerCommandConfig packageManagerCommands, Path directory, List<String> paths) {
        String resolved = null;
        if (binary.isEmpty()) {
            if (Files.isRegularFile(directory.resolve(PackageManagerType.YARN.getLockFile()))) {
                resolved = PackageManagerType.YARN.getCommand();
            } else if (Files.isRegularFile(directory.resolve(PackageManagerType.PNPM.getLockFile()))) {
                resolved = PackageManagerType.PNPM.getCommand();
            } else {
                resolved = PackageManagerType.NPM.getCommand();
            }
            if (isWindows()) {
                resolved = resolved + ".cmd";
            }
        } else {
            resolved = binary.get();
        }
        return new PackageManager(directory, resolveCommands(resolved, packageManagerCommands, paths));
    }

    public static boolean isWindows() {
        return QuarkusConsole.IS_WINDOWS;
    }

    static PackageManagerCommands resolveCommands(String binary, PackageManagerCommandConfig packageManagerCommands,
            List<String> paths) {
        if (binary.contains(PackageManagerType.PNPM.getCommand())) {
            return new EffectiveCommands(new PNPMPackageManagerCommands(binary), packageManagerCommands, paths);
        }
        if (binary.contains(PackageManagerType.NPM.getCommand())) {
            return new EffectiveCommands(new NPMPackageManagerCommands(binary), packageManagerCommands, paths);
        }
        if (binary.contains(PackageManagerType.YARN.getCommand())) {
            return new EffectiveCommands(new YarnPackageManagerCommands(binary), packageManagerCommands, paths);
        }
        throw new UnsupportedOperationException("Unsupported package manager binary: " + binary);
    }

    private Process process(Command command) {
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

    private boolean exec(Command command) {
        Process process = null;
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
            new HandleOutput(process.getInputStream()).run();
            process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("Input/Output error while executing command.", e);
        } catch (InterruptedException e) {
            return false;
        }
        return process != null && process.exitValue() == 0;
    }

    private String[] runner(Command command) {
        if (isWindows()) {
            return new String[] { "cmd.exe", "/c", command.commandWithArguments };
        } else {
            return new String[] { "sh", "-c", command.commandWithArguments };
        }
    }

    private static class HandleOutput implements Runnable {

        private final InputStream is;
        private final Logger.Level logLevel;

        HandleOutput(InputStream is) {
            this(is, Logger.Level.INFO);
        }

        HandleOutput(InputStream is, Logger.Level logLevel) {
            this.is = is;
            this.logLevel = LOG.isEnabled(logLevel) ? logLevel : null;
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
            }
        }
    }

    public static String isDevServerUp(String host, int port, String path) {
        final String normalizedPath = path.indexOf("/") == 0 ? path : "/" + path;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                try {
                    final String hostAddress = address.getHostAddress();
                    final String ipAddress = address instanceof Inet6Address ? "[" + hostAddress + "]" : hostAddress;
                    URL url = new URL(String.format("http://%s:%d%s", ipAddress, port, normalizedPath));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    connection.connect();
                    int code = connection.getResponseCode();
                    return code == 200 ? ipAddress : null;
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
        private final String hostAddress;

        public DevServer(Process process, String hostAddress) {
            this.process = process;
            this.hostAddress = hostAddress;
        }

        public Process process() {
            return process;
        }

        public String hostAddress() {
            return hostAddress;
        }
    }
}
