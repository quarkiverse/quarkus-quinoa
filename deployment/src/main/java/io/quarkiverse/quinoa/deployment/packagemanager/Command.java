package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Collections;
import java.util.Map;

class Command {
    public final Map<String, String> envs;
    public final String[] args;

    Command(String... args) {
        this.envs = Collections.emptyMap();
        this.args = args;
    }

    Command(Map<String, String> envs, String... args) {
        this.envs = envs;
        this.args = args;
    }

    public String asSingleCommand() {
        return String.join(" ", args);
    }
}
