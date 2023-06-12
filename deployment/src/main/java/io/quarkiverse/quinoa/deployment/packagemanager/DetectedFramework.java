package io.quarkiverse.quinoa.deployment.packagemanager;

import java.util.Objects;

import jakarta.json.JsonObject;

public class DetectedFramework {

    private FrameworkType frameworkType;
    private JsonObject packageJson;

    public DetectedFramework() {
        super();
    }

    public DetectedFramework(FrameworkType frameworkType, JsonObject packageJson) {
        super();
        this.frameworkType = frameworkType;
        this.packageJson = packageJson;
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

    @Override
    public String toString() {
        return "DetectedFramework{" +
                "frameworkType=" + frameworkType +
                ", packageJson=" + packageJson +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DetectedFramework that = (DetectedFramework) o;
        return frameworkType == that.frameworkType && packageJson.equals(that.packageJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frameworkType, packageJson);
    }
}
