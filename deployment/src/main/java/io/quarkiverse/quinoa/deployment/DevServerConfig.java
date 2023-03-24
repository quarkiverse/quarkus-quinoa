package io.quarkiverse.quinoa.deployment;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevServerConfig {

    /**
     * Enable external dev server (live coding).
     * The "dev-server.port" config is required to communicate with the dev server.
     * If not set the default is true.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "true")
    public boolean enabled;

    /**
     * When set to true, Quinoa will manage the Web UI dev server
     * When set to false, the Web UI dev server have to be started before running Quarkus dev
     */
    @ConfigItem(defaultValue = "true")
    public boolean managed;

    /**
     * Port of the server to forward requests to.
     * The dev server process (i.e npm start) is managed like a dev service by Quarkus.
     * If the external server responds with a 404, it is ignored by Quinoa and processed like any other backend request.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * Host of the server to forward requests to.
     * "localhost" is the default
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * After start, Quinoa wait for the external dev server.
     * by sending GET requests to this path waiting for a 200 status.
     *
     * If not set the default is "/".
     * If empty string "", Quinoa will not check if the dev server is up.
     */
    @ConfigItem(defaultValue = "/")
    public Optional<String> checkPath;

    /**
     * By default, Quinoa will handle request upgrade to websocket and act as proxy with the dev server.
     * If set to false, Quinoa will pass websocket upgrade request to the next Vert.x route handler.
     */
    @ConfigItem(defaultValue = "true")
    public boolean websocket;

    /**
     * Timeout in ms for the dev server to be up and running.
     * If not set the default is ~30000ms.
     */
    @ConfigItem(defaultValue = "30000")
    public int checkTimeout;

    /**
     * Enable external dev server live coding logs.
     * This is not enabled by default because most dev servers display compilation errors directly in the browser.
     * False if not set.
     */
    @ConfigItem
    public boolean logs;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DevServerConfig that = (DevServerConfig) o;
        return checkTimeout == that.checkTimeout && logs == that.logs && Objects.equals(enabled, that.enabled)
                && Objects.equals(port, that.port) && Objects.equals(checkPath, that.checkPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, port, checkPath, checkTimeout, logs);
    }
}
