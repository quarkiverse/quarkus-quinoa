package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Collections;
import java.util.Map;

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
