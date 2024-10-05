package io.quarkiverse.quinoa.deployment.items;

import io.quarkus.builder.item.SimpleBuildItem;

public final class PublishedPackageBuildItem extends SimpleBuildItem {

    private final boolean skipped;

    public PublishedPackageBuildItem(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isSkipped() {
        return skipped;
    }
}
