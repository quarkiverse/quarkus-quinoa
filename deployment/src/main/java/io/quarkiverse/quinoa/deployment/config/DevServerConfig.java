package io.quarkiverse.quinoa.deployment.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface DevServerConfig {

    /**
     * Enable external dev server (live coding).
     * If the "dev-server.port" config is not detected or defined it will be disabled.
     */
    @WithParentName
    @WithDefault("true")
    boolean enabled();

    /**
     * When set to true, Quinoa will manage the Web UI dev server
     * When set to false, the Web UI dev server have to be started before running Quarkus dev
     */
    @WithDefault("true")
    boolean managed();

    /**
     * Port of the server to forward requests to.
     * The dev server process (i.e npm start) is managed like a dev service by Quarkus.
     * If the external server responds with a 404, it is ignored by Quinoa and processed like any other backend request.
     */
    @ConfigDocDefault("framework detection or fallback to empty")
    Optional<Integer> port();

    /**
     * Host of the server to forward requests to.
     */
    @WithDefault("localhost")
    String host();

    /**
     * After start, Quinoa wait for the external dev server.
     * by sending GET requests to this path waiting for a 200 status.
     * If forced empty, Quinoa will not check if the dev server is up.
     */
    @WithDefault("/")
    Optional<String> checkPath();

    /**
     * By default, Quinoa will handle request upgrade to websocket and act as proxy with the dev server.
     * If set to false, Quinoa will pass websocket upgrade request to the next Vert.x route handler.
     */
    @WithDefault("true")
    boolean websocket();

    /**
     * Timeout in ms for the dev server to be up and running.
     */
    @WithDefault("30000")
    int checkTimeout();

    /**
     * Enable external dev server live coding logs.
     * This is not enabled by default because most dev servers display compilation errors directly in the browser.
     */
    @WithDefault("false")
    boolean logs();

    /**
     * Set this value if the index page is different for the dev-server
     */
    @ConfigDocDefault("auto-detected falling back to the quinoa.index-page")
    Optional<String> indexPage();
}
