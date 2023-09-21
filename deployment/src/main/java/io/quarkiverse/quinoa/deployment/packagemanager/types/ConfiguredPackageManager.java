package io.quarkiverse.quinoa.deployment.packagemanager.types;

import static io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig.DEFAULT_BUILD_COMMAND;
import static io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig.DEFAULT_INSTALL_COMMAND;
import static io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig.DEFAULT_TEST_COMMAND;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner;
import io.quarkus.runtime.LaunchMode;

class ConfiguredPackageManager implements PackageManager {
    private static final String PATH_ENV_VAR = "PATH";

    private final PackageManagerType type;
    private final String binary;
    private final PackageManagerCommandConfig commandsConfig;
    private final List<String> paths;

    ConfiguredPackageManager(PackageManagerType type, String binary, PackageManagerCommandConfig commandsConfig,
            List<String> paths) {
        this.type = type;
        this.binary = binary;
        this.commandsConfig = commandsConfig;
        this.paths = paths;
    }

    @Override
    public Command ci() {
        return new Command(
                environment(Map.of(), commandsConfig.ciEnv()),
                prepareCommandWithArguments(commandsConfig.ci().orElse(type.ciCommand())));
    }

    @Override
    public Command install() {
        return new Command(
                environment(Map.of(), commandsConfig.installEnv()),
                prepareCommandWithArguments(commandsConfig.install().orElse(DEFAULT_INSTALL_COMMAND)));
    }

    @Override
    public String binary() {
        return binary;
    }

    @Override
    public Command build(LaunchMode mode) {
        // MODE=dev/test/prod to be able to build differently depending on the mode
        // NODE_ENV=development/production
        final Map<String, String> env = Map.of(
                "MODE", mode.getDefaultProfile(),
                "NODE_ENV", mode.equals(LaunchMode.DEVELOPMENT) ? "development" : "production");
        return new Command(
                environment(env, commandsConfig.buildEnv()),
                prepareCommandWithArguments(commandsConfig.build().orElse(DEFAULT_BUILD_COMMAND)));
    }

    @Override
    public Command test() {
        final Map<String, String> testEnv = commandsConfig.testEnv();
        if (testEnv.isEmpty()) {
            testEnv.put("CI", "true");
        }
        return new Command(
                environment(Map.of(), testEnv),
                prepareCommandWithArguments(commandsConfig.test().orElse(DEFAULT_TEST_COMMAND)));
    }

    @Override
    public Command dev() {
        return new Command(
                environment(Map.of(), commandsConfig.devEnv()),
                prepareCommandWithArguments(commandsConfig.dev().orElseThrow()));
    }

    private String prepareCommandWithArguments(String command) {
        return String.format("%s %s", binary(), command);
    }

    private Map<String, String> environment(final Map<String, String> configuredEnvs,
            final Map<String, String> additionalEnvironment) {
        final Map<String, String> environment = new HashMap<>(System.getenv());

        if (configuredEnvs != null) {
            environment.putAll(configuredEnvs);
        }

        if (additionalEnvironment != null) {
            environment.putAll(additionalEnvironment);
        }

        if (PackageManagerRunner.isWindows()) {
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
