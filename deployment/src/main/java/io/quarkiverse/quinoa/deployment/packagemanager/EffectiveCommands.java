package io.quarkiverse.quinoa.deployment.packagemanager;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.LaunchMode;

class EffectiveCommands implements PackageManagerCommands {
    private static final String PATH_ENV_VAR = "PATH";
    private final PackageManagerCommands defaultCommands;
    private final PackageManagerCommandConfig commandsConfig;
    private final List<String> paths;

    EffectiveCommands(PackageManagerCommands defaultCommands, PackageManagerCommandConfig commandsConfig, List<String> paths) {
        this.defaultCommands = defaultCommands;
        this.commandsConfig = commandsConfig;
        this.paths = paths;
    }

    @Override
    public Command install(boolean frozenLockfile) {
        Command c = defaultCommands.install(frozenLockfile);
        return new Command(
                environment(c, commandsConfig.installEnv),
                getCustomCommandWithArguments(commandsConfig.install)
                        .orElse(c.commandWithArguments));
    }

    @Override
    public String binary() {
        return defaultCommands.binary();
    }

    @Override
    public Command build(LaunchMode mode) {
        Command c = defaultCommands.build(mode);
        return new Command(
                environment(c, commandsConfig.buildEnv),
                getCustomCommandWithArguments(commandsConfig.build)
                        .orElse(c.commandWithArguments));
    }

    @Override
    public Command test() {
        Command c = defaultCommands.test();
        return new Command(
                environment(c, commandsConfig.testEnv),
                getCustomCommandWithArguments(commandsConfig.test)
                        .orElse(c.commandWithArguments));
    }

    @Override
    public Command dev() {
        Command c = defaultCommands.dev();
        return new Command(
                environment(c, commandsConfig.devEnv),
                getCustomCommandWithArguments(commandsConfig.dev)
                        .orElse(c.commandWithArguments));
    }

    private Optional<String> getCustomCommandWithArguments(Optional<String> command) {
        return command
                .map(a -> commandsConfig.prependBinary ? defaultCommands.binary() + " " + a : a);
    }

    private Map<String, String> environment(final Command c, final Map<String, String> additionalEnvironment) {
        final Map<String, String> environment = new HashMap<>(System.getenv());

        environment.putAll(c.envs);

        if (additionalEnvironment != null) {
            environment.putAll(additionalEnvironment);
        }

        if (PackageManager.isWindows()) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                final String pathName = entry.getKey();
                if (PATH_ENV_VAR.equalsIgnoreCase(pathName)) {
                    final String pathValue = entry.getValue();
                    environment.put(pathName, extendPathVariable(pathValue, paths));
                }
            }
        } else {
            final String pathValue = environment.get(PATH_ENV_VAR);
            environment.put(PATH_ENV_VAR, extendPathVariable(pathValue, paths));
        }

        return environment;
    }

    private String extendPathVariable(final String existingValue, final List<String> paths) {
        final StringBuilder pathBuilder = new StringBuilder();
        for (final String path : paths) {
            pathBuilder.append(path).append(File.pathSeparator);
        }
        if (existingValue != null) {
            pathBuilder.append(existingValue).append(File.pathSeparator);
        }
        return pathBuilder.toString();
    }
}
