package io.quarkiverse.quinoa;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the network configuration settings for Quinoa,
 * including TLS options, host, port, and websocket settings.
 */
public class QuinoaNetworkConfiguration {

    private final boolean tls;
    private final boolean tlsAllowInsecure;
    private String host;
    private Integer port;
    private final boolean websocket;

    /**
     * Constructs a new {@code QuinoaNetworkConfiguration} with the specified settings.
     *
     * @param tls whether TLS is enabled
     * @param tlsAllowInsecure whether insecure TLS connections are allowed
     * @param host the hostname or IP address of the server
     * @param port the port number on which the server is listening
     * @param websocket whether websocket is enabled
     */
    public QuinoaNetworkConfiguration(boolean tls, boolean tlsAllowInsecure, String host, Integer port, boolean websocket) {
        this.tls = tls;
        this.tlsAllowInsecure = tlsAllowInsecure;
        this.host = host;
        this.port = port;
        this.websocket = websocket;
    }

    /**
     * Returns whether TLS is enabled.
     *
     * @return {@code true} if TLS is enabled, {@code false} otherwise
     */
    public boolean isTls() {
        return tls;
    }

    /**
     * Returns whether insecure TLS connections are allowed.
     *
     * @return {@code true} if insecure TLS connections are allowed, {@code false} otherwise
     */
    public boolean isTlsAllowInsecure() {
        return tlsAllowInsecure;
    }

    /**
     * Returns the hostname or IP address of the server.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the port number on which the server is listening.
     *
     * @return the port number
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Returns whether websocket is enabled.
     *
     * @return {@code true} if websocket is enabled, {@code false} otherwise
     */
    public boolean isWebsocket() {
        return websocket;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuinoaNetworkConfiguration that = (QuinoaNetworkConfiguration) o;
        return isTls() == that.isTls() && isTlsAllowInsecure() == that.isTlsAllowInsecure()
                && isWebsocket() == that.isWebsocket() && Objects.equals(getHost(), that.getHost())
                && Objects.equals(getPort(), that.getPort());
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(isTls(), isTlsAllowInsecure(), getHost(), getPort(), isWebsocket());
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", QuinoaNetworkConfiguration.class.getSimpleName() + "[", "]")
                .add("tls=" + tls)
                .add("tlsAllowInsecure=" + tlsAllowInsecure)
                .add("host='" + host + "'")
                .add("port=" + port)
                .add("websocket=" + websocket)
                .toString();
    }
}