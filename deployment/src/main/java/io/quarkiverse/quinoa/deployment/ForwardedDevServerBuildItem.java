package io.quarkiverse.quinoa.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ForwardedDevServerBuildItem extends SimpleBuildItem {

    private final Integer port;

    public ForwardedDevServerBuildItem(Integer port) {
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }
}
