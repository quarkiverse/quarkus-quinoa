package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.LaunchMode;

class EffectiveCommands implements PackageManagerCommands {
    private final PackageManagerCommands defaultCommands;
    private final PackageManagerCommandConfig commandsConfig;

    EffectiveCommands(PackageManagerCommands defaultCommands, PackageManagerCommandConfig commandsConfig) {
        this.defaultCommands = defaultCommands;
        this.commandsConfig = commandsConfig;
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

    private Map<String, String> environment(Command c, Map<String, String> override) {
        Map<String, String> environment = new HashMap<>(c.envs);
        environment.putAll(override);
        return environment;
    }
}
