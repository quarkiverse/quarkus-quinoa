package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Collections;
import java.util.Map;

import io.quarkus.runtime.LaunchMode;

interface PackageManagerCommands {
    Command install(boolean frozenLockfile);

    String binary();

    default Command build(LaunchMode mode) {
        // MODE=dev/test/prod to be able to build differently depending on the mode
        // NODE_ENV=development/production
        final Map<String, String> env = Map.of(
                "MODE", mode.getDefaultProfile(),
                "NODE_ENV", mode.equals(LaunchMode.DEVELOPMENT) ? "development" : "production");
        return new Command(env, binary() + " run build");
    }

    default Command test() {
        // CI=true to avoid watch mode on Angular
        return new Command(Collections.singletonMap("CI", "true"), binary() + " test");
    }

    default Command dev() {
        // BROWSER=NONE so the browser is not automatically opened with React
        return new Command(Collections.singletonMap("BROWSER", "none"), binary() + " start");
    }
}
