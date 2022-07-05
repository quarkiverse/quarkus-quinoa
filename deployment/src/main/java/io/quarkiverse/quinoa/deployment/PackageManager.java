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
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.runtime.LaunchMode;

public class PackageManager {
    private static final Logger LOG = Logger.getLogger(PackageManager.class);
    private static final Commands NPM = new NPMCommands();
    private static final Commands YARN = new YarnCommands();
    private static final Commands PNPM = new PNPMCommands();

    private final Path directory;
    private final Commands commands;

    private PackageManager(Path directory, Commands commands) {
        this.directory = directory;
        this.commands = commands;
    }

    public Path getDirectory() {
        return directory;
    }

    public void install(boolean frozenLockfile) {
        final Command install = commands.install(frozenLockfile);
        final String printable = install.printable();
        LOG.infof("Running Quinoa package manager install command: %s", printable);
        if (!exec(install)) {
            throw new RuntimeException(format("Error in Quinoa while running package manager install command: %s", printable));
        }
    }

    public void build(LaunchMode mode) {
        final Command build = commands.build(mode);
        final String printable = build.printable();
        LOG.infof("Running Quinoa package manager build command: %s", printable);
        if (!exec(build)) {
            throw new RuntimeException(format("Error in Quinoa while running package manager build command: %s", printable));
        }
    }

    public void test() {
        final Command test = commands.test();
        final String printable = test.printable();
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

    public Process dev(int checkPort, String checkPath, int checkTimeout) {
        final Command dev = commands.dev();
        LOG.infof("Running Quinoa package manager live coding as a dev service: %s", dev.printable());
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
                resolved = YarnCommands.yarn;
            } else if (Files.isRegularFile(directory.resolve("pnpm-lock.yaml"))) {
                resolved = PNPMCommands.pnpm;
            } else {
                resolved = NPMCommands.npm;
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

    static Commands resolveCommands(String binary, PackageManagerCommandsConfig packageManagerCommands) {
        if (binary.contains(PNPMCommands.pnpm)) {
            return new ConfiguredCommands(PNPM, packageManagerCommands);
        }
        if (binary.contains(NPMCommands.npm)) {
            return new ConfiguredCommands(NPM, packageManagerCommands);
        }
        if (binary.contains(YarnCommands.yarn)) {
            return new ConfiguredCommands(YARN, packageManagerCommands);
        }
        throw new UnsupportedOperationException("Unsupported package manager binary: " + binary);
    }

    private Process process(Command command) {
        Process process = null;
        final ProcessBuilder builder = new ProcessBuilder()
                .directory(directory.toFile())
                .command(command.args);
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
                    .command(command.args)
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

        public String printable() {
            return String.join(" ", args) + ", with environment: " + envs;
        }
    }

    interface Commands {
        Command install(boolean frozenLockfile);

        String binary();

        default Command build(LaunchMode mode) {
            // MODE=dev/test/normal to be able to build differently depending on the mode
            return new Command(Collections.singletonMap("MODE", mode.getDefaultProfile()), binary(), "run", "build");
        }

        default Command test() {
            // CI=true to avoid watch mode on Angular
            return new Command(Collections.singletonMap("CI", "true"), binary(), "test");
        }

        default Command dev() {
            // BROWSER=NONE so the browser is not automatically opened with React
            return new Command(Collections.singletonMap("BROWSER", "none"), binary(), "start");
        }
    }

    private static class NPMCommands implements Commands {
        static final String npm = "npm";

        @Override
        public String binary() {
            return npm;
        }

        @Override
        public Command install(boolean frozenLockfile) {
            if (frozenLockfile) {
                return new Command("ci");
            }
            return new Command("install");
        }

    }

    private static class PNPMCommands implements Commands {

        static String pnpm = "pnpm";

        @Override
        public String binary() {
            return pnpm;
        }

        @Override
        public Command install(boolean frozenLockfile) {
            if (frozenLockfile) {
                return new Command("install", "--frozen-lockfile");
            }
            return new Command("install");
        }
    }

    private static class YarnCommands implements Commands {
        static final String yarn = "yarn";

        @Override
        public String binary() {
            return yarn;
        }

        @Override
        public Command install(boolean frozenLockfile) {
            if (frozenLockfile) {
                return new Command("install", "--frozen-lockfile");
            }
            return new Command("install");
        }
    }

    private static class ConfiguredCommands implements Commands {
        private final Commands detectedCommands;
        private final PackageManagerCommandsConfig commandsConfig;

        private ConfiguredCommands(Commands detectedCommands, PackageManagerCommandsConfig commandsConfig) {
            this.detectedCommands = detectedCommands;
            this.commandsConfig = commandsConfig;
        }

        @Override
        public Command install(boolean frozenLockfile) {
            Command c = detectedCommands.install(frozenLockfile);
            return new Command(
                    commandsConfig.installEnv.isEmpty() ? c.envs : commandsConfig.installEnv,
                    mapToArray(commandsConfig.install).orElse(c.args));
        }

        @Override
        public String binary() {
            return detectedCommands.binary();
        }

        @Override
        public Command build(LaunchMode mode) {
            Command c = detectedCommands.build(mode);
            return new Command(
                    commandsConfig.buildEnv.isEmpty() ? c.envs : commandsConfig.buildEnv,
                    mapToArray(commandsConfig.build).orElse(c.args));
        }

        @Override
        public Command test() {
            Command c = detectedCommands.test();
            return new Command(
                    commandsConfig.testEnv.isEmpty() ? c.envs : commandsConfig.testEnv,
                    mapToArray(commandsConfig.test).orElse(c.args));
        }

        @Override
        public Command dev() {
            Command c = detectedCommands.dev();
            return new Command(
                    commandsConfig.devEnv.isEmpty() ? c.envs : commandsConfig.devEnv,
                    mapToArray(commandsConfig.dev).orElse(c.args));
        }

        private Optional<String[]> mapToArray(Optional<String> command) {
            return command.map(new Function<String, String[]>() {
                @Override
                public String[] apply(String s) {
                    return s.split(" ");
                }
            });
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
