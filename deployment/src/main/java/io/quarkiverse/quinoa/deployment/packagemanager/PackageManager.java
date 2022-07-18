package io.quarkiverse.quinoa.deployment.packagemanager;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ProcessUtil;
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
        if (process == null) {
            return;
        }
        LOG.infof("Stopping Quinoa package manager live coding as a dev service.");
        // Kill children before because react is swallowing the signal
        process.descendants().forEach(new Consumer<ProcessHandle>() {
            @Override
            public void accept(ProcessHandle processHandle) {
                processHandle.destroy();
            }
        });
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                ;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    public Process dev(int checkPort, String checkPath, int checkTimeout) {
        final Command dev = packageManagerCommands.dev();
        LOG.infof("Running Quinoa package manager live coding as a dev service: %s", dev.commandWithArguments);
        Process p = process(dev);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                stopDev(p);
            }
        });
        if (checkPath == null) {
            LOG.infof("Quinoa is configured to continue without check if the live coding server is up");
            return p;
        }
        try {
            int i = 0;
            while (!isDevServerUp(checkPath, checkPort)) {
                if (++i >= checkTimeout / 500) {
                    stopDev(p);
                    throw new RuntimeException(
                            "Quinoa package manager live coding port " + checkPort
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
        return p;
    }

    public static PackageManager autoDetectPackageManager(Optional<String> binary,
            PackageManagerCommandsConfig packageManagerCommands, Path directory) {
        String resolved = null;
        if (binary.isEmpty()) {
            if (Files.isRegularFile(directory.resolve("yarn.lock"))) {
                resolved = YarnPackageManagerCommands.yarn;
            } else if (Files.isRegularFile(directory.resolve("pnpm-lock.yaml"))) {
                resolved = PNPMPackageManagerCommands.pnpm;
            } else {
                resolved = NPMPackageManagerCommands.npm;
            }
            final String os = System.getProperty("os.name");
            if (os != null && os.startsWith("Windows")) {
                resolved = resolved + ".cmd";
            }
        } else {
            resolved = binary.get();
        }
        return new PackageManager(directory, resolveCommands(resolved, packageManagerCommands));
    }

    static PackageManagerCommands resolveCommands(String binary, PackageManagerCommandsConfig packageManagerCommands) {
        if (binary.contains(PNPMPackageManagerCommands.pnpm)) {
            return new EffectiveCommands(new PNPMPackageManagerCommands(binary), packageManagerCommands);
        }
        if (binary.contains(NPMPackageManagerCommands.npm)) {
            return new EffectiveCommands(new NPMPackageManagerCommands(binary), packageManagerCommands);
        }
        if (binary.contains(YarnPackageManagerCommands.yarn)) {
            return new EffectiveCommands(new YarnPackageManagerCommands(binary), packageManagerCommands);
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
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Windows")) {
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

    private static boolean isDevServerUp(String path, int port) {
        try {
            final String normalizedPath = path.indexOf("/") == 0 ? path : "/" + path;
            URL url = new URL("http://localhost:" + port + normalizedPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(200);
            connection.connect();
            int code = connection.getResponseCode();
            if (code == 200) {
                return true;
            }
            return false;
        } catch (ConnectException | SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Error while checking if package manager dev server is up", e);
        }
    }
}
