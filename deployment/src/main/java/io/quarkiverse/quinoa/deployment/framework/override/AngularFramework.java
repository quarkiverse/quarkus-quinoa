package io.quarkiverse.quinoa.deployment.framework.override;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import jakarta.json.*;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.PackageManagerCommandConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;

public class AngularFramework extends GenericFramework {

    public static final String ANGULAR_DEVKIT_BUILD_ANGULAR_APPLICATION = "@angular-devkit/build-angular:application";
    public static final String ANGULAR_JSON_FILE = "angular.json";

    public AngularFramework() {
        super("dist", "start", 4200);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig originalConfig, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized, Path uiDir) {
        final String devScript = detectedDevScript.orElse(getDefaultDevScriptName());
        return new QuinoaConfigDelegate(super.override(originalConfig, packageJson, detectedDevScript, isCustomized,
                uiDir)) {
            @Override
            public Optional<String> buildDir() {
                return Optional.of(originalConfig.buildDir().orElseGet(() -> {
                    final JsonObject angularJson = readAngularJson(uiDir);
                    final JsonObject projectList = angularJson.getJsonObject("projects");
                    final JsonObject builder = projectList.values().stream()
                            .map(JsonValue::asJsonObject)
                            .filter(project -> "application".equals(project.getString("projectType")))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException(
                                    "Quinoa failed to determine which application must be started in the angular.json file."))
                            .getJsonObject("architect")
                            .getJsonObject("build");
                    return getBuildDir(builder);
                }));
            }

            private static JsonObject readAngularJson(Path uiDir) {
                try (JsonReader reader = Json.createReader(Files.newInputStream(uiDir.resolve(ANGULAR_JSON_FILE)))) {
                    return reader.readObject();
                } catch (IOException | JsonException e) {
                    throw new RuntimeException("Quinoa failed to read the angular.json file.", e);
                }
            }

            @Override
            public PackageManagerCommandConfig packageManagerCommand() {
                return new PackageManagerCommandConfigDelegate(super.packageManagerCommand()) {
                    @Override
                    public Optional<String> dev() {
                        final String extraArgs = isCustomized ? "" : " -- --host=0.0.0.0 --hmr";
                        return Optional.of(originalConfig.packageManagerCommand().dev()
                                .orElse("run " + devScript + extraArgs));
                    }

                    @Override
                    public Optional<String> test() {
                        final String extraArgs = isCustomized ? "" : " -- --no-watch --no-progress --browsers=ChromeHeadlessCI";
                        return Optional.of(originalConfig.packageManagerCommand().test()
                                .orElse(DEFAULT_TEST_COMMAND + extraArgs));
                    }
                };
            }
        };
    }

    static String getBuildDir(JsonObject build) {
        final String builderName = build.getString("builder");
        JsonValue outputPath = build.getJsonObject("options").get("outputPath");
        if (outputPath instanceof JsonString outputPathStr) {
            String fullBuildDir = outputPathStr.getString();
            if (ANGULAR_DEVKIT_BUILD_ANGULAR_APPLICATION.equals(builderName)) {
                fullBuildDir = String.format("%s/browser", fullBuildDir);
            }
            return fullBuildDir;
        }
        if (outputPath instanceof JsonObject outputPathObj) {
            return String.format("%s/%s",
                    outputPathObj.getString("base"),
                    outputPathObj.getString("browser", "browser"));
        }
        throw new RuntimeException("Unexpected type of outputPath in the angular.json file.");
    }

}
