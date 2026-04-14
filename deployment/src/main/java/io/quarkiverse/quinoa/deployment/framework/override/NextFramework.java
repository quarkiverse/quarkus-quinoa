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
    static final String STATIC_EXPORT_OUTPUT_VALUE = "export";
    static final String SSR_BUILD_DIR = ".next";
    static final String EXPORT_BUILD_DIR = "out";

    public NextFramework() {
        super(EXPORT_BUILD_DIR, "dev", 3000);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized, Path uiDir) {

        final boolean isStaticExport = isStaticExport(packageJson);

        if (isStaticExport) {
            LOG.info("Quinoa detected Next.js with static export (output: 'export'). Using build output from 'out/'.");
        } else {
            LOG.info("Quinoa detected Next.js App Router. SSR mode will be enabled automatically.");
        }

        // Use the correct build dir depending on whether static export is configured
        final String buildDir = isStaticExport ? EXPORT_BUILD_DIR : SSR_BUILD_DIR;
        final QuinoaConfig baseConfig = new GenericFramework(buildDir, "dev", 3000)
                .override(delegate, packageJson, detectedDevScript, isCustomized, uiDir);

        return new QuinoaConfigDelegate(baseConfig) {
            @Override
            public boolean enableSSRMode() {
                // Auto-enable SSR mode for App Router (non-static-export) builds
                return !isStaticExport || super.enableSSRMode();
            }

            @Override
            public DevServerConfig devServer() {
                return new DevServerConfigDelegate(super.devServer()) {
                    @Override
                    public Optional<String> indexPage() {
                        // In dev mode Next.js serves all routes from root "/"
                        return Optional.of(super.indexPage().orElse("/"));
                    }
                };
            }
        };
    }

    /**
     * Returns true if the package.json signals a static export build
     * (i.e. it contains {@code "output": "export"} at the top level,
     * which is the Quinoa convention for opting into {@code next export} mode).
     */
    static boolean isStaticExport(Optional<JsonObject> packageJson) {
        return packageJson
                .map(json -> STATIC_EXPORT_OUTPUT_VALUE.equals(json.getString("output", null)))
                .orElse(false);
    }
}