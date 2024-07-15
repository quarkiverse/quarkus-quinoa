package io.quarkiverse.quinoa.deployment.framework.override;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.PackageManagerCommandConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;
import io.quarkus.logging.Log;

public class AngularFramework extends GenericFramework {

    public static final String ANGULAR_DEVKIT_BUILD_ANGULAR_APPLICATION = "@angular-devkit/build-angular:application";
    public static final String ANGULAR_JSON_FILE = "angular.json";

    public AngularFramework() {
        super("dist", "start", 4200);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig originalConfig, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized) {
        final String devScript = detectedDevScript.orElse(getDefaultDevScriptName());
        return new QuinoaConfigDelegate(super.override(originalConfig, packageJson, detectedDevScript, isCustomized)) {
            @Override
            public Optional<String> buildDir() {
                // Angular builds a custom directory "dist/[appname]" or "dist/[appname]/browser" if it is build with application builder
                final String applicationName = packageJson.map(p -> p.getString("name")).orElse("quinoa");
                final JsonObject angularJson = readAngularJson(originalConfig);
                final String builder = getBuilderUse(angularJson, applicationName);
                String fullBuildDir = String.format("%s/%s", getDefaultBuildDir(), applicationName);
                if (ANGULAR_DEVKIT_BUILD_ANGULAR_APPLICATION.equals(builder)) {
                    fullBuildDir = String.format("%s/browser", fullBuildDir);
                }
                return Optional.of(originalConfig.buildDir().orElse(fullBuildDir));
            }

            private static JsonObject readAngularJson(QuinoaConfig configuration) {
                Log.debug("=== Configuration ===" + configuration);
                try (JsonReader reader = Json
                        .createReader(Files.newInputStream(Path.of(configuration.uiDir() + "/" + ANGULAR_JSON_FILE)))) {
                    return reader.readObject();
                } catch (IOException | JsonException e) {
                    throw new RuntimeException("Quinoa failed to read the angular.json file. %s", e);
                }
            }

            private static String getBuilderUse(JsonObject angularJson, String applicationName) {
                return angularJson.getJsonObject("projects").getJsonObject(applicationName)
                        .getJsonObject("architect").getJsonObject("build").getString("builder");
            }

            @Override
            public PackageManagerCommandConfig packageManagerCommand() {
                return new PackageManagerCommandConfigDelegate(super.packageManagerCommand()) {
                    @Override
                    public Optional<String> dev() {
                        final String extraArgs = isCustomized ? "" : " -- --disable-host-check --hmr";
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
}
