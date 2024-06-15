package io.quarkiverse.quinoa.deployment.config;

import java.util.Objects;
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
     * When set to true, Quinoa requests will be forwarded with tls enabled.
     */
    @WithDefault("false")
    boolean tls();

    /**
     * When set to true, Quinoa will accept any certificate with any hostname.
     */
    @WithDefault("false")
    boolean tlsAllowInsecure();

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
    @ConfigDocDefault("auto-detected falling back to index.html")
    Optional<String> indexPage();

    /**
     * Quinoa deals with SPA routing by itself (see quarkus.quinoa.enable-spa-routing), some dev-server have this feature
     * enabled by default.
     * This is a problem for proxying as it prevents other Quarkus resources (REST, ...) to answer.
     * By default, Quinoa will try to detect when the dev server is answering with a html page for non-existing resources
     * (SPA-Routing)
     * in which case it will instead allow other Quarkus resources (REST, ...) to answer.
     * Set this to true (direct) when the other Quarkus resources use a specific path prefix (and marked as ignored by Quinoa)
     * or if the dev-server is configured without SPA routing.
     */
    @WithDefault("false")
    boolean directForwarding();

    static boolean isEqual(DevServerConfig d1, DevServerConfig d2) {
        if (!Objects.equals(d1.enabled(), d2.enabled())) {
            return false;
        }
        if (!Objects.equals(d1.managed(), d2.managed())) {
            return false;
        }
        if (!Objects.equals(d1.port(), d2.port())) {
            return false;
        }
        if (!Objects.equals(d1.host(), d2.host())) {
            return false;
        }
        if (!Objects.equals(d1.tls(), d2.tls())) {
            return false;
        }
        if (!Objects.equals(d1.tlsAllowInsecure(), d2.tlsAllowInsecure())) {
            return false;
        }
        if (!Objects.equals(d1.checkPath(), d2.checkPath())) {
            return false;
        }
        if (!Objects.equals(d1.websocket(), d2.websocket())) {
            return false;
        }
        if (!Objects.equals(d1.checkTimeout(), d2.checkTimeout())) {
            return false;
        }
        if (!Objects.equals(d1.logs(), d2.logs())) {
            return false;
        }
        if (!Objects.equals(d1.indexPage(), d2.indexPage())) {
            return false;
        }
        if (!Objects.equals(d1.directForwarding(), d2.directForwarding())) {
            return false;
        }
        return true;
    }
}
