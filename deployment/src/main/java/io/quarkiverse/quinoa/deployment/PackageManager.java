package io.quarkiverse.quinoa.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.jboss.logging.Logger;

public class PackageManager {
    private static final Logger LOG = Logger.getLogger(PackageManager.class);
    private static final Commands NPM = new NPMCommands();
    private static final Commands YARN = new YarnCommands();

    private final String packageManagerBinary;
    private final Path directory;
    private final Commands commands;

    public PackageManager(String packageManagerBinary, Path directory) {
        this.packageManagerBinary = packageManagerBinary;
        this.directory = directory;
        this.commands = detect(packageManagerBinary);
    }

    public boolean install(boolean frozenLockfile) {
        final Command install = commands.install(frozenLockfile);
        LOG.infof("Running Quinoa install command: %s %s", packageManagerBinary, String.join(" ", install.args));
        return exec(install);
    }

    public boolean build() {
        final Command build = commands.build();
        LOG.infof("Running Quinoa build command: %s %s", packageManagerBinary, String.join(" ", build.args));
        return exec(build);
    }

    public boolean test() {
        final Command test = commands.test();
        LOG.infof("Running Quinoa test command: %s %s", packageManagerBinary, String.join(" ", test.args));
        return exec(test);
    }

    static Commands detect(String binary) {
        if (binary.contains("npm")) {
            return NPM;
        }
        if (binary.contains("yarn")) {
            return YARN;
        }
        throw new UnsupportedOperationException("Unsupported package manager binary: " + binary);
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
    }

    interface Commands {
        Command install(boolean frozenLockfile);

        Command build();

        Command test();
    }

    private static class NPMCommands implements Commands {

        @Override
        public Command install(boolean frozenLockfile) {
            if (frozenLockfile) {
                return new Command("install", "--frozen-lockfile");
            }
            return new Command("install");
        }

        @Override
        public Command build() {
            return new Command("run", "build");
        }

        @Override
        public Command test() {
            return new Command(Collections.singletonMap("CI", "true"), "test");
        }
    }

    private static class YarnCommands implements Commands {

        @Override
        public Command install(boolean frozenLockfile) {
            if (frozenLockfile) {
                return new Command("ci");
            }
            return new Command("install");
        }

        @Override
        public Command build() {
            return new Command("build");
        }

        @Override
        public Command test() {
            return new Command(Collections.singletonMap("CI", "true"), "test");
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
}
