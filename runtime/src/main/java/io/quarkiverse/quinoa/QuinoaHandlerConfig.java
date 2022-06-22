package io.quarkiverse.quinoa;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class QuinoaHandlerConfig {
    public final List<String> ignoredPathPrefixes;
    public final String indexPage;
    public final boolean prodMode;
    public final boolean enableCompression;
    public final Set<String> compressMediaTypes;

    @RecordableConstructor
    public QuinoaHandlerConfig(List<String> ignoredPathPrefixes, String indexPage, boolean prodMode, boolean enableCompression,
            Set<String> compressMediaTypes) {
        this.ignoredPathPrefixes = ignoredPathPrefixes;
        this.indexPage = indexPage;
        this.prodMode = prodMode;
        this.enableCompression = enableCompression;
        this.compressMediaTypes = compressMediaTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuinoaHandlerConfig that = (QuinoaHandlerConfig) o;
        return Objects.equals(ignoredPathPrefixes, that.ignoredPathPrefixes) && Objects.equals(indexPage, that.indexPage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ignoredPathPrefixes, indexPage);
    }
}
