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
    static final String EXPORT_BUILD_DIR = "out";
    static final String SSR_BUILD_DIR = ".next";
    private static final String DEV_SCRIPT_NAME = "dev";
    private static final int DEV_SERVER_PORT = 3000;

    public NextFramework() {
        super(EXPORT_BUILD_DIR, DEV_SCRIPT_NAME, DEV_SERVER_PORT);
    }

    @Override
    public QuinoaConfig override(QuinoaConfig delegate, Optional<JsonObject> packageJson,
            Optional<String> detectedDevScript, boolean isCustomized, Path uiDir) {
        // 'quarkus.quinoa.enable-ssr-mode' alone decides between a static export (default) and a server-rendered app
        final boolean ssrMode = delegate.enableSSRMode();
        final QuinoaConfig baseConfig = ssrMode
                ? generic(SSR_BUILD_DIR, DEV_SCRIPT_NAME, DEV_SERVER_PORT)
                        .override(delegate, packageJson, detectedDevScript, isCustomized, uiDir)
                : super.override(delegate, packageJson, detectedDevScript, isCustomized, uiDir);

        final String buildDir = baseConfig.buildDir().orElseThrow();
        if (ssrMode) {
            LOG.infof("Quinoa is using Next.js in SSR mode (build directory: '%s'), "
                    + "as 'quarkus.quinoa.enable-ssr-mode' is enabled.", buildDir);
        } else {
            LOG.infof("Quinoa is using Next.js in static export mode (build directory: '%s'). "
                    + "Set 'quarkus.quinoa.enable-ssr-mode=true' for a server-rendered app.", buildDir);
        }

        return new QuinoaConfigDelegate(baseConfig) {
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
}
