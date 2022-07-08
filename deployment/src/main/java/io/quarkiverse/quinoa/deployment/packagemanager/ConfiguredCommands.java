package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.runtime.LaunchMode;

class ConfiguredCommands implements Commands {
    private final Commands detectedCommands;
    private final PackageManagerCommandsConfig commandsConfig;

    ConfiguredCommands(Commands detectedCommands, PackageManagerCommandsConfig commandsConfig) {
        this.detectedCommands = detectedCommands;
        this.commandsConfig = commandsConfig;
    }

    @Override
    public Command install(boolean frozenLockfile) {
        Command c = detectedCommands.install(frozenLockfile);
        return new Command(
                environment(c, commandsConfig.installEnv),
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
                environment(c, commandsConfig.buildEnv),
                mapToArray(commandsConfig.build).orElse(c.args));
    }

    @Override
    public Command test() {
        Command c = detectedCommands.test();
        return new Command(
                environment(c, commandsConfig.testEnv),
                mapToArray(commandsConfig.test).orElse(c.args));
    }

    @Override
    public Command dev() {
        Command c = detectedCommands.dev();
        return new Command(
                environment(c, commandsConfig.devEnv),
                mapToArray(commandsConfig.dev).orElse(c.args));
    }

    private Map<String, String> environment(Command c, Map<String, String> override) {
        Map<String, String> environment = new HashMap<>(c.envs);
        environment.putAll(override);
        return environment;
    }

    private Optional<String[]> mapToArray(Optional<String> command) {
        return command.map(new Function<String, String[]>() {
            @Override
            public String[] apply(String s) {
                return new String[] { s };
            }
        });
    }
}
