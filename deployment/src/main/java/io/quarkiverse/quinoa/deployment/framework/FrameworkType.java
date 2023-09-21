package io.quarkiverse.quinoa.deployment.framework;

import static io.quarkiverse.quinoa.deployment.framework.override.GenericFramework.UNKNOWN_FRAMEWORK;
import static io.quarkiverse.quinoa.deployment.framework.override.GenericFramework.generic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;

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

    REACT(Set.of("react-scripts", "react-app-rewired", "craco"), new ReactFramework()),
    VUE_LEGACY(Set.of("vue-cli-service"), generic("dist", "serve", 3000)),
    VITE(Set.of("vite"), generic("dist", "dev", 5173)),
    SOLID_START(Set.of("solid-start"), generic("dist", "dev", 3000)),
    ASTRO(Set.of("astro"), generic("dist", "dev", 3000)),
    NEXT(Set.of("next"), new NextFramework()),
    NUXT(Set.of("nuxt"), generic("dist", "dev", 3000)),
    ANGULAR(Set.of("ng serve"), new AngularFramework()),
    EMBER(Set.of("ember-cli"), generic("dist", "serve", 4200)),
    AURELIA(Set.of("aurelia-cli"), generic("dist", "start", 8080)),
    POLYMER(Set.of("polymer-cli"), generic("build", "serve", 8080)),
    QWIK(Set.of("qwik"), generic("dist", "start", 5173)),
    GATSBY(Set.of("gatsby-cli"), generic("dist", "develop", 8000)),
    CYCLEJS(Set.of("cycle"), generic("build", "start", 8000)),
    RIOTJS(Set.of("riot-cli"), generic("build", "start", 3000)),
    MIDWAYJS(Set.of("midway"), generic("dist", "dev", 7001)),
    REFINE(Set.of("refine"), generic("build", "dev", 3000)),
    WEB_COMPONENTS(Set.of("web-dev-server"), generic("dist", "start", 8003));

    private static final Logger LOG = Logger.getLogger(FrameworkType.class);

    public static final Set<String> DEV_SCRIPTS = Arrays.stream(values())
            .map(framework -> framework.factory.getFrameworkDevScriptName())
            .collect(Collectors.toCollection(TreeSet::new));

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
            return UNKNOWN_FRAMEWORK.override(config, Optional.empty());
        }

        JsonObject packageJson = null;
        try (JsonReader reader = Json.createReader(Files.newInputStream(packageJsonFile))) {
            packageJson = reader.readObject();
        } catch (IOException | JsonException e) {
            LOG.warnf("Quinoa failed to read the package.json file. %s", e.getMessage());
        }

        JsonString detectedDevScriptCommand = null;
        String detectedDevScript = null;

        if (packageJson != null) {
            JsonObject scripts = packageJson.getJsonObject("scripts");
            if (scripts != null) {
                // loop over all possible start scripts until we find one
                for (String devScript : FrameworkType.DEV_SCRIPTS) {
                    detectedDevScriptCommand = scripts.getJsonString(devScript);
                    if (detectedDevScriptCommand != null) {
                        detectedDevScript = devScript;
                        break;
                    }
                }
            }
        }

        if (detectedDevScript == null) {
            LOG.trace("Quinoa could not auto-detect the framework from package.json file.");
            return UNKNOWN_FRAMEWORK.override(config, Optional.ofNullable(packageJson));
        }

        // check if we found a script to detect which framework
        final FrameworkType frameworkType = resolveFramework(detectedDevScriptCommand.getString());
        if (frameworkType == null) {
            LOG.info("Quinoa could not auto-detect the framework from package.json file.");
            return UNKNOWN_FRAMEWORK.override(config, Optional.ofNullable(packageJson));
        }

        String expectedScript = frameworkType.factory.getFrameworkDevScriptName();
        if (launchMode.getLaunchMode().isDevOrTest() && !Objects.equals(detectedDevScript, expectedScript)) {
            LOG.warnf("%s framework typically defines a '%s` script in package.json file but found '%s' instead.",
                    frameworkType, expectedScript, detectedDevScript);
        }

        LOG.infof("Quinoa detected '%s' framework from package.json file.", frameworkType);
        return frameworkType.factory.override(config, Optional.ofNullable(packageJson));
    }

    /**
     * Try and detect the framework based on the script starting with a command like "vite" or "ng"
     *
     * @param script the script to check
     * @return either NULL if no match or the matching framework if found
     */
    private static FrameworkType resolveFramework(String script) {
        final String lowerScript = script.toLowerCase(Locale.ROOT);
        for (FrameworkType value : values()) {
            for (String cliName : value.cliStartDev) {
                if (lowerScript.contains(cliName)) {
                    return value;
                }
            }
        }
        return null;
    }

}
