package io.quarkiverse.quinoa.deployment;

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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.runtime.LaunchMode;

public class PackageManager {
    private static final Logger LOG = Logger.getLogger(PackageManager.class);
    private static final Commands NPM = new NPMCommands();
    private static final Commands YARN = new YarnCommands();

    private final String packageManagerBinary;
    private final Path directory;
    private final Commands commands;

    private PackageManager(String packageManagerBinary, Path directory) {
        this.packageManagerBinary = packageManagerBinary;
        this.directory = directory;
        this.commands = resolveCommands(packageManagerBinary);
    }

    public Path getDirectory() {
        return directory;
    }

    public void install(boolean frozenLockfile) {
        final Command install = commands.install(frozenLockfile);
        final String printable = install.printable(packageManagerBinary);
        LOG.infof("Running Quinoa package manager install command: %s", printable);
        if (!exec(install)) {
            throw new RuntimeException(format("Error in Quinoa while running package manager install command: %s", printable));
        }
    }

    public void build(LaunchMode mode) {
        final Command build = commands.build(mode);
        final String printable = build.printable(packageManagerBinary);
        LOG.infof("Running Quinoa package manager build command: %s", printable);
        if (!exec(build)) {
            throw new RuntimeException(format("Error in Quinoa while running package manager build command: %s", printable));
        }
    }

    public void test() {
        final Command test = commands.test();
        final String printable = test.printable(packageManagerBinary);
        LOG.infof("Running Quinoa package manager test command: %s", printable);
        if (!exec(test)) {
            throw new RuntimeException(format("Error in Quinoa while running package manager test command: %s", printable));
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

    public Process dev(int checkPort) {
        final Command dev = commands.dev();
        LOG.infof("Running Quinoa package manager live coding as a dev service: %s", dev.printable(packageManagerBinary));
        Process p = process(dev);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                stopDev(p);
            }
        });
        try {
            int i = 0;
            while (!isDevServerUp(checkPort)) {
                if (++i >= 30) {
                    stopDev(p);
                    throw new RuntimeException(
                            "Quinoa package manager live coding port " + checkPort
                                    + " is still not listening after the timeout.");
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

    public static PackageManager autoDetectPackageManager(Optional<String> binary, Path directory) {
        String resolved = binary.orElse("npm");
        if (Files.isRegularFile(directory.resolve("yarn.lock"))) {
            resolved = "yarn";
        }
        if (Files.isRegularFile(directory.resolve("pnpm-lock.yaml"))) {
            resolved = "pnpm";
        }
        return new PackageManager(resolved, directory);

    }

    static Commands resolveCommands(String binary) {
        if (binary.contains("npm") || binary.contains("pnpm")) {
            return NPM;
        }
        if (binary.contains("yarn")) {
            return YARN;
        }
        throw new UnsupportedOperationException("Unsupported package manager binary: " + binary);
    }

    private Process process(Command command) {
        Process process = null;
        String[] cmd = new String[command.args.length + 1];
        cmd[0] = packageManagerBinary;
        if (command.args.length > 0) {
            System.arraycopy(command.args, 0, cmd, 1, command.args.length);
        }
        final ProcessBuilder builder = new ProcessBuilder()
                .directory(directory.toFile())
                .command(cmd);
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
            String[] cmd = new String[command.args.length + 1];
            cmd[0] = packageManagerBinary;
            if (command.args.length > 0) {
                System.arraycopy(command.args, 0, cmd, 1, command.args.length);
            }
            final ProcessBuilder processBuilder = new ProcessBuilder();
            if (!command.envs.isEmpty()) {
                processBuilder.environment().putAll(command.envs);
            }
            process = processBuilder
                    .directory(directory.toFile())
                    .command(cmd)
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

    private static class Command {
        public final Map<String, String> envs;
        public final String[] args;

        private Command(String... args) {
            this.envs = Collections.emptyMap();
            this.args = args;
        }

        private Command(Map<String, String> envs, String... args) {
            this.envs = envs;
            this.args = args;
        }

        public String printable(String binary) {
            return binary + " " + String.join(" ", args);
        }
    }

    interface Commands {
        Command install(boolean frozenLockfile);

        default Command build(LaunchMode mode) {
            // MODE=dev/test/normal to be able to build differently depending on the mode
            return new Command(Collections.singletonMap("MODE", mode.getDefaultProfile()), "run", "build");
        }

        default Command test() {
            // CI=true to avoid watch mode on Angular
            return new Command(Collections.singletonMap("CI", "true"), "test");
        }

        default Command dev() {
            // BROWSER=NONE so the browser is not automatically opened with React
            return new Command(Collections.singletonMap("BROWSER", "none"), "start");
        }
    }

    private static class NPMCommands implements Commands {

        @Override
        public Command install(boolean frozenLockfile) {
            if (frozenLockfile) {
                return new Command("ci");
            }
            return new Command("install");
        }

    }

    private static class YarnCommands implements Commands {

        @Override
        public Command install(boolean frozenLockfile) {
            if (frozenLockfile) {
                return new Command("install", "--frozen-lockfile");
            }
            return new Command("install");
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

    private static boolean isDevServerUp(int port) {
        try {
            URL url = new URL("http://localhost:" + port);
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
