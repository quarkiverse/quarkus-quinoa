package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Configuration defaults for multiple JS frameworks that can be used to allow for easier adoption with less user configuration.
 */
public enum FrameworkType {

    REACT("build", "start", 3000, Set.of("react-scripts", "react-app-rewired", "craco")),
    VITE("dist", "dev", 5173, Set.of("vite")),
    NEXT("out", "dev", 3000, Set.of("next")),
    ANGULAR("dist/%s", "start", 4200, Set.of("ng")),
    WEB_COMPONENTS("dist", "start", 8003, Set.of("web-dev-server"));

    /**
     * This the Web UI internal build system (webpack, …​) output directory. After the build, Quinoa will take the files from
     * this directory, move them to 'target/quinoa-build' (or build/quinoa-build with Gradle) and serve them at runtime.
     */
    private final String buildDirectory;

    /**
     * The script to run in package.json in dev mode typically "start" or "dev".
     */
    private final String devScript;

    /**
     * Default UI live-coding dev server port (proxy mode).
     */
    private final int devServerPort;

    /**
     * Match package.json scripts to detect this framework in use.
     */
    private final Set<String> packageScripts;

    FrameworkType(String buildDirectory, String devScript, int devServerPort, Set<String> packageScripts) {
        this.buildDirectory = buildDirectory;
        this.devScript = devScript;
        this.devServerPort = devServerPort;
        this.packageScripts = packageScripts;
    }

    /**
     * Try and detect the framework based on the script starting with a command like "vite" or "ng"
     *
     * @param script the script to check
     * @return either NULL if no match or the matching framework if found
     */
    public static FrameworkType evaluate(String script) {
        final String lowerScript = script.toLowerCase(Locale.ROOT);
        for (FrameworkType value : values()) {
            Set<String> commands = value.getPackageScripts();
            for (String command : commands) {
                if (lowerScript.startsWith(command)) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Gets the unique list of possible start dev scripts like "dev" and "start" etc.
     *
     * @return the Set of unique start scripts
     */
    public static Set<String> getDevScripts() {
        final Set<String> scripts = new TreeSet<>();
        for (FrameworkType value : values()) {
            scripts.add(value.getDevScript());
        }
        return scripts;
    }

    public String getBuildDirectory() {
        return buildDirectory;
    }

    public String getDevScript() {
        return devScript;
    }

    public Set<String> getPackageScripts() {
        return packageScripts;
    }

    public int getDevServerPort() {
        return devServerPort;
    }
}