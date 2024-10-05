package io.quarkiverse.quinoa.deployment.framework;

import static io.quarkiverse.quinoa.deployment.framework.override.GenericFramework.UNKNOWN_FRAMEWORK;
import static io.quarkiverse.quinoa.deployment.framework.override.GenericFramework.generic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue.ValueType;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.framework.override.AngularFramework;
import io.quarkiverse.quinoa.deployment.framework.override.NextFramework;
import io.quarkiverse.quinoa.deployment.framework.override.ReactFramework;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;

/**
 * Configuration defaults for multiple JS frameworks that can be used to allow for easier adoption with less user configuration.
 */
public enum FrameworkType {

    REACT(Set.of("react-scripts start", "react-app-rewired start", "craco start"), new ReactFramework()),
    VUE_LEGACY(Set.of("vue-cli-service serve"), generic("dist", "serve", 3000)),
    VITE(Set.of("vite"), generic("dist", "dev", 5173)),
    FARM(Set.of("farm"), generic("dist", "dev", 9000)),
    SOLID_START_LEGACY(Set.of("solid-start dev"), generic("dist", "dev", 3000)),
    SOLID_START(Set.of("vinxi dev"), generic(".output", "dev", 3000)),
    ASTRO(Set.of("astro dev"), generic("dist", "dev", 3000)),
    NEXT(Set.of("next"), new NextFramework()),
    NUXT(Set.of("nuxt dev"), generic("dist", "dev", 3000)),
    ANGULAR(Set.of("ng serve"), new AngularFramework()),
    EMBER(Set.of("ember-cli serve"), generic("dist", "serve", 4200)),
    GATSBY(Set.of("gatsby develop"), generic("dist", "develop", 8000)),
    MIDWAYJS(Set.of("midway-bin dev"), generic("dist", "dev", 7001));

    private static final Logger LOG = Logger.getLogger(FrameworkType.class);

    private static final Set<String> DEV_SCRIPTS = Arrays.stream(values())
            .map(framework -> framework.factory.getDefaultDevScriptName())
            .collect(Collectors.toCollection(TreeSet::new));

    private static final Map<String, FrameworkType> TYPE_START_DEV_COMMAND_MAPPING = Arrays.stream(values())
            .flatMap(t -> t.cliStartDev.stream().map(c -> new SimpleImmutableEntry<>(c, t)))
            .collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));
    private final Set<String> cliStartDev;
    private final FrameworkConfigOverrideFactory factory;

    FrameworkType(Set<String> cliStartDev, FrameworkConfigOverrideFactory factory) {
        this.cliStartDev = cliStartDev;
        this.factory = factory;
    }

    public FrameworkConfigOverrideFactory factory() {
        return factory;
    }

    public static QuinoaConfig overrideConfig(LaunchModeBuildItem launchMode, QuinoaConfig config, Path packageJsonFile) {
        if (!config.framework().detection()) {
            return UNKNOWN_FRAMEWORK.override(config, Optional.empty(), Optional.empty(), true);
        }

        final JsonObject packageJson = readPackageJson(packageJsonFile);
        final Optional<DetectedFramework> detectedFramework = detectFramework(packageJson);

        if (detectedFramework.isEmpty()) {
            LOG.trace("Quinoa could not auto-detect the frameworkType from package.json file.");
            return UNKNOWN_FRAMEWORK.override(config, Optional.of(packageJson), Optional.empty(), true);
        }

        final FrameworkType frameworkType = detectedFramework.get().type;
        LOG.infof("Quinoa detected '%s' frameworkType from package.json file.", frameworkType);

        return frameworkType.factory.override(config, Optional.of(packageJson), Optional.of(detectedFramework.get().devScript),
                detectedFramework.get().isCustomized);
    }

    private static JsonObject readPackageJson(Path packageJsonFile) {
        try (JsonReader reader = Json.createReader(Files.newInputStream(packageJsonFile))) {
            return reader.readObject();
        } catch (IOException | JsonException e) {
            throw new RuntimeException("Quinoa failed to read the package.json file. %s", e);
        }
    }

    static Optional<DetectedFramework> detectFramework(JsonObject packageJson) {
        final JsonObject scripts = packageJson.getJsonObject("scripts");
        if (scripts == null || scripts.isEmpty()) {
            return Optional.empty();
        }
        final Map<String, String> projectDevScripts = scripts.entrySet().stream()
                .filter(e -> DEV_SCRIPTS.contains(e.getKey()) && e.getValue() != null
                        && ValueType.STRING.equals(e.getValue().getValueType()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ((JsonString) e.getValue()).getString().trim().toLowerCase(Locale.US)));

        for (Map.Entry<String, String> e : projectDevScripts.entrySet()) {
            if (TYPE_START_DEV_COMMAND_MAPPING.containsKey(e.getValue())) {
                final FrameworkType framework = TYPE_START_DEV_COMMAND_MAPPING.get(e.getValue());
                final String frameworkDefaultDevScript = framework.factory().getDefaultDevScriptName();
                LOG.debugf("Detected framework with dev command perfect match: %s", framework.toString());
                final String projectDevScript = e.getKey();
                final boolean isDefaultDevCommand = projectDevScript.equals(frameworkDefaultDevScript);
                if (!isDefaultDevCommand) {
                    LOG.warnf("%s framework typically defines a '%s' script in package.json file but found '%s' instead.",
                            framework, frameworkDefaultDevScript, projectDevScript);
                }
                return Optional.of(new DetectedFramework(framework, projectDevScript, !isDefaultDevCommand));
            }
        }
        // Fallback to partial match
        for (Map.Entry<String, String> projectDevScriptEntry : projectDevScripts.entrySet()) {
            for (Map.Entry<String, FrameworkType> frameworksByCommand : TYPE_START_DEV_COMMAND_MAPPING.entrySet()) {
                final FrameworkType framework = frameworksByCommand.getValue();
                final String frameworkDefaultDevScript = framework.factory().getDefaultDevScriptName();
                final String frameworkCommand = frameworksByCommand.getKey();
                final String projectDevScript = projectDevScriptEntry.getKey();
                final String projectDevCommand = projectDevScriptEntry.getValue();
                final boolean detected = projectDevCommand.contains(frameworkCommand);
                if (detected) {
                    final boolean isDefaultDevCommand = projectDevScript.equals(frameworkDefaultDevScript);
                    final boolean endsWith = projectDevCommand.endsWith(frameworkCommand);
                    if (isDefaultDevCommand || projectDevScripts.size() == 1) {
                        // We can assume this is the right dev script
                        if (!isDefaultDevCommand) {
                            LOG.warnf(
                                    "%s framework typically defines a '%s` script in package.json file but found '%s' instead.",
                                    framework, frameworkDefaultDevScript, projectDevScript);
                        }
                        return Optional.of(new DetectedFramework(framework, projectDevScript,
                                !isDefaultDevCommand || !endsWith));
                    } else {
                        LOG.errorf(
                                "Framework detection failed: It is probably %s framework which typically defines a '%s' script in package.json but found multiple other dev scripts instead (%s).",
                                framework, frameworkDefaultDevScript, String.join(", ", projectDevScripts.keySet()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Try and detect the framework based on the script starting with a command like "vite" or "ng"
     *
     * @param script the script to check
     * @return either NULL if no match or the matching framework if found
     */
    private static FrameworkType resolveFramework(String script) {
        final String normalizedScript = script.toLowerCase(Locale.ROOT).trim();
        for (FrameworkType value : values()) {
            for (String cliName : value.cliStartDev) {
                if (normalizedScript.contains(cliName)) {
                    return value;
                }
            }
        }
        return null;
    }

    record DetectedFramework(FrameworkType type, String devScript, boolean isCustomized) {

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            DetectedFramework that = (DetectedFramework) o;
            return isCustomized == that.isCustomized && type == that.type && Objects.equals(devScript, that.devScript);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", DetectedFramework.class.getSimpleName() + "[", "]")
                    .add("type=" + type)
                    .add("devScript='" + devScript + "'")
                    .add("isCustomized=" + isCustomized)
                    .toString();
        }
    }

}
