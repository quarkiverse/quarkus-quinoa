package io.quarkiverse.quinoa.deployment.packagemanager.types;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkus.runtime.LaunchMode;

public interface PackageManager {

    Command ci();

    Command install();

    String binary();

    /**
     * Returns the additional filesystem paths required by the package manager,
     * such as the Node.js installation directory.
     *
     * @return list of additional paths, may be empty
     */
    List<String> paths();

    /**
     * Returns the type of the package manager (e.g. npm, yarn, pnpm, bun).
     *
     * @return the package manager type
     */
    PackageManagerType type();

    Command build(LaunchMode mode);

    Command test();

    Command publish();

    Command dev();

    public static PackageManager resolve(PackageManagerType type, String binary,
            PackageManagerCommandConfig packageManagerCommands,
            List<String> paths) {
        return configure(type, binary, packageManagerCommands, paths);
    }

    private static PackageManager configure(PackageManagerType type, String binary, PackageManagerCommandConfig commandsConfig,
            List<String> paths) {
        return new ConfiguredPackageManager(type, binary, commandsConfig, paths);
    }

    class Command {
        public final Map<String, String> envs;
        public final String commandWithArguments;

        Command(String commandWithArguments) {
            this.envs = Collections.emptyMap();
            this.commandWithArguments = commandWithArguments;
        }

        Command(Map<String, String> envs, String commandWithArguments) {
            this.envs = envs;
            this.commandWithArguments = commandWithArguments;
        }
    }
}
