package io.quarkiverse.quinoa;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class QuinoaHandlerConfig {
    public final List<String> ignoredPathPrefixes;
    public final String indexPage;
    public final boolean devMode;
    public final boolean enableCompression;
    public final Set<String> compressMediaTypes;

    public final boolean devServerDirectForwarding;

    @RecordableConstructor
    public QuinoaHandlerConfig(List<String> ignoredPathPrefixes, String indexPage, boolean devMode, boolean enableCompression,
            Set<String> compressMediaTypes, boolean devServerDirectForwarding) {
        this.ignoredPathPrefixes = ignoredPathPrefixes;
        this.indexPage = "/".equals(indexPage) ? "" : indexPage;
        this.devMode = devMode;
        this.enableCompression = enableCompression;
        this.compressMediaTypes = compressMediaTypes;
        this.devServerDirectForwarding = devServerDirectForwarding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuinoaHandlerConfig that = (QuinoaHandlerConfig) o;
        return devMode == that.devMode && enableCompression == that.enableCompression
                && devServerDirectForwarding == that.devServerDirectForwarding
                && Objects.equals(ignoredPathPrefixes, that.ignoredPathPrefixes) && Objects.equals(indexPage, that.indexPage)
                && Objects.equals(compressMediaTypes, that.compressMediaTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ignoredPathPrefixes, indexPage, devMode, enableCompression, compressMediaTypes,
                devServerDirectForwarding);
    }
}
