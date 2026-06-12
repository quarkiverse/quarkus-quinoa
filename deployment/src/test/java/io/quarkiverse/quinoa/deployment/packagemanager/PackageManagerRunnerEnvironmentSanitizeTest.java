package io.quarkiverse.quinoa.deployment.packagemanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PackageManagerRunnerEnvironmentSanitizeTest {

    @SuppressWarnings("unchecked")
    private static Set<String> getProblematicEnvVars() {
        try {
            var field = PackageManagerRunner.class.getDeclaredField("PROBLEMATIC_ENV_VARS");
            field.setAccessible(true);
            return (Set<String>) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void sanitizeEnv(Map<String, String> env) {
        for (String var : getProblematicEnvVars()) {
            env.remove(var);
        }
    }

    @Test
    void sanitizeEnvironment_removesPWD() {
        var env = new HashMap<String, String>();
        env.put("PWD", "/some/wrong/dir");
        env.put("PATH", "/usr/bin:/bin");

        sanitizeEnv(env);

        assertThat(env).containsKey("PATH");
        assertThat(env).doesNotContainKey("PWD");
    }

    @Test
    void sanitizeEnvironment_removesOLDPWD() {
        var env = new HashMap<String, String>();
        env.put("OLDPWD", "/previous/dir");

        sanitizeEnv(env);

        assertThat(env).doesNotContainKey("OLDPWD");
    }
}
