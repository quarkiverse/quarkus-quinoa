package io.quarkiverse.quinoa.deployment.items;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ForwardedDevServerBuildItem extends SimpleBuildItem {

    private final boolean tls;
    private final boolean tlsAllowInsecure;
    private final String host;
    private final Integer port;

    public ForwardedDevServerBuildItem(boolean tls, boolean tlsAllowInsecure, String host, Integer port) {
        this.tls = tls;
        this.tlsAllowInsecure = tlsAllowInsecure;
        this.host = host;
        this.port = port;
    }

    public boolean isTls() {
        return tls;
    }

    public boolean isTlsAllowInsecure() {
        return tlsAllowInsecure;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
