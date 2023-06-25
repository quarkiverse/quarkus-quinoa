package io.quarkiverse.quinoa.deployment.packagemanager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.JsonObject;

import org.jboss.logging.Logger;

public class DetectedFramework {

    private static final Logger LOG = Logger.getLogger(DetectedFramework.class);

    private FrameworkType frameworkType;
    private JsonObject packageJson;
    private String devServerCommand;

    private int devServerPort;

    public DetectedFramework() {
        super();
    }

    public DetectedFramework(FrameworkType frameworkType, JsonObject packageJson, String devServerCommand, int devServerPort) {
        super();
        this.frameworkType = frameworkType;
        this.packageJson = packageJson;
        this.devServerCommand = devServerCommand;
        this.devServerPort = devServerPort;
    }

    public FrameworkType getFrameworkType() {
        return frameworkType;
    }

    public void setFrameworkType(FrameworkType frameworkType) {
        this.frameworkType = frameworkType;
    }

    public JsonObject getPackageJson() {
        return packageJson;
    }

    public void setPackageJson(JsonObject packageJson) {
        this.packageJson = packageJson;
    }

    public String getDevServerCommand() {
        return devServerCommand;
    }

    public void setDevServerCommand(String devServerCommand) {
        this.devServerCommand = devServerCommand;
    }

    public int getDevServerPort() {
        return devServerPort;
    }

    public void setDevServerPort(int devServerPort) {
        this.devServerPort = devServerPort;
    }

    /**
     * Gets the current framework build directory with special handling for Angular.
     *
     * @return the build directory for this framework type
     */
    public String getBuildDirectory() {
        FrameworkType framework = getFrameworkType();
        if (framework == null) {
            return null;
        }

        String buildDirectory = framework.getBuildDirectory();

        // Angular builds a custom directory "dist/[appname]"
        if (framework == FrameworkType.ANGULAR) {
            String applicationName = Objects.toString(packageJson.getString("name"), "quinoa");
            buildDirectory = String.format(buildDirectory, applicationName);
        }
        return buildDirectory;
    }

    /**
     * Some frameworks like Vite might configure the dev server port in another file like vite.config.js.
     *
     * @param uiDirectory the user infterface directory to look for the config file
     */
    public void checkPortOverride(Path uiDirectory) {
        FrameworkType framework = this.getFrameworkType();
        if (framework == null) {
            return;
        }

        final String frameworkConfigFile = framework.getFrameworkConfigFile();
        final Pattern regex = framework.getFrameworkPortRegex();
        if (frameworkConfigFile == null || regex == null) {
            return;
        }

        // check vite.config.js for { server: { port: 3000 }}
        final Path cfgFile = uiDirectory.resolve(frameworkConfigFile);
        if (Files.isRegularFile(cfgFile)) {
            try {
                final String cfgContent = Files.readString(cfgFile, StandardCharsets.UTF_8);

                final Matcher matcher = regex.matcher(cfgContent);
                if (matcher.find()) {
                    final int port = Integer.parseInt(matcher.group(1));
                    LOG.infof("%s framework found port override in '%s': %d", this.getFrameworkType(),
                            frameworkConfigFile, port);
                    this.setDevServerPort(port);
                }
            } catch (IOException e) {
                // nothing more to check here if parsing failed
            }
        }
    }

    @Override
    public String toString() {
        return "DetectedFramework{" +
                "frameworkType=" + frameworkType +
                ", packageJson=" + packageJson +
                ", devServerCommand='" + devServerCommand + '\'' +
                ", devServerPort=" + devServerPort +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DetectedFramework that = (DetectedFramework) o;
        return devServerPort == that.devServerPort && frameworkType == that.frameworkType
                && packageJson.equals(that.packageJson) && devServerCommand.equals(that.devServerCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frameworkType, packageJson, devServerCommand, devServerPort);
    }
}
