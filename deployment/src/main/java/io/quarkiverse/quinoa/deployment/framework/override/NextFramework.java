package io.quarkiverse.quinoa.deployment.framework.override;

import java.nio.file.Path;
import java.util.Optional;

import jakarta.json.JsonObject;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.deployment.config.DevServerConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.DevServerConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;

public class NextFramework extends GenericFramework {

    private static final Logger LOG = Logger.getLogger(NextFramework.class);
    private static final String EXPECTED_OUTPUT_VALUE = "export";

    public NextFramework() {
        super("out", "dev", 3000);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized, Path uiDir) {
        LOG.warn("Next.js version 13 and above are not fully supported yet. Please make sure to use version 12 or below.");

        if (delegate.packageManagerCommand().build().orElse("???").equals("run build") && packageJson.isPresent()) {
            JsonObject scripts = packageJson.get().getJsonObject("scripts");
            if (scripts != null) {
                String buildScript = scripts.getString("build");
                if (buildScript == null || buildScript.isEmpty()) {
                    LOG.warn(
                            "Make sure you define  \"build\": \"next build \", in the package.json to make Next work with Quinoa.");
                }

                String output = packageJson.get().getString("output", null);
                if (!EXPECTED_OUTPUT_VALUE.equals(output)) {
                    LOG.warn(
                            "Make sure you define  \"output\": \"export \", in the package.json to make Next work with Quinoa.");
                }
            }
        }

        return new QuinoaConfigDelegate(super.override(delegate, packageJson, detectedDevScript, isCustomized, uiDir)) {

            @Override
            public DevServerConfig devServer() {
                return new DevServerConfigDelegate(super.devServer()) {
                    @Override
                    public Optional<String> indexPage() {
                        // In Dev mode Next.js serves everything out of root "/" but in PRD mode it is the
                        // normal "/index.html".
                        return Optional.of(super.indexPage().orElse("/"));
                    }
                };
            }
        };
    }
}