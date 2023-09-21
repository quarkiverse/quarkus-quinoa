package io.quarkiverse.quinoa.deployment.framework.override;

import java.util.Optional;

import jakarta.json.JsonObject;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.deployment.config.DevServerConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.config.delegate.DevServerConfigDelegate;
import io.quarkiverse.quinoa.deployment.config.delegate.QuinoaConfigDelegate;

public class NextFramework extends GenericFramework {

    private static final Logger LOG = Logger.getLogger(NextFramework.class);

    public NextFramework() {
        super("out", "dev", 3000);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson) {
        if (delegate.packageManagerCommand().build().equals("run build") && packageJson.isPresent()) {
            JsonObject scripts = packageJson.get().getJsonObject("scripts");
            if (scripts != null) {
                if (!scripts.getString("build").contains("next export")) {
                    LOG.warn(
                            "Make sure you define  \"build\": \"next build && next export\", in the package.json to make Next work with Quinoa.");
                }
            }
        }
        return new QuinoaConfigDelegate(super.override(delegate, packageJson)) {

            @Override
            public DevServerConfig devServer() {
                return new DevServerConfigDelegate(super.devServer()) {
                    @Override
                    public Optional<String> indexPage() {
                        // In Dev mode Next.js serves everything out of root "/" but in PRD mode its the normal "/index.html".
                        return Optional.of(super.indexPage().orElse("/"));
                    }
                };
            }
        };
    }
}
