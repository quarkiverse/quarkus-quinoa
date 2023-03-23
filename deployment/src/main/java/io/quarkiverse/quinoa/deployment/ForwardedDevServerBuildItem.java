package io.quarkiverse.quinoa.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ForwardedDevServerBuildItem extends SimpleBuildItem {

    private final String host;
    private final Integer port;

    public ForwardedDevServerBuildItem(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
