package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.HashMap;
import java.util.Map;

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
                commandsConfig.install.orElse(c.commandWithArguments));
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
                commandsConfig.build.orElse(c.commandWithArguments));
    }

    @Override
    public Command test() {
        Command c = detectedCommands.test();
        return new Command(
                environment(c, commandsConfig.testEnv),
                commandsConfig.test.orElse(c.commandWithArguments));
    }

    @Override
    public Command dev() {
        Command c = detectedCommands.dev();
        return new Command(
                environment(c, commandsConfig.devEnv),
                commandsConfig.dev.orElse(c.commandWithArguments));
    }

    private Map<String, String> environment(Command c, Map<String, String> override) {
        Map<String, String> environment = new HashMap<>(c.envs);
        environment.putAll(override);
        return environment;
    }
}
