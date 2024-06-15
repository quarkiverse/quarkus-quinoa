package io.quarkiverse.quinoa.deployment.items;

import io.quarkiverse.quinoa.QuinoaNetworkConfiguration;
import io.quarkus.builder.item.SimpleBuildItem;

public final class ForwardedDevServerBuildItem extends SimpleBuildItem {

    private final QuinoaNetworkConfiguration networkConfiguration;

    public ForwardedDevServerBuildItem(QuinoaNetworkConfiguration networkConfiguration) {
        this.networkConfiguration = networkConfiguration;
    }

    public QuinoaNetworkConfiguration getNetworkConfiguration() {
        return networkConfiguration;
    }

    public boolean isTls() {
        return networkConfiguration.isTls();
    }

    public boolean isTlsAllowInsecure() {
        return networkConfiguration.isTlsAllowInsecure();
    }

    public String getHost() {
        return networkConfiguration.getHost();
    }

    public Integer getPort() {
        return networkConfiguration.getPort();
    }
}