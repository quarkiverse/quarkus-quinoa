package io.quarkiverse.quinoa.deployment.config.delegate;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.quinoa.deployment.config.TauriConfig;

public class TauriConfigDelegate implements TauriConfig {
    private final TauriConfig delegate;

    public TauriConfigDelegate(TauriConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public Optional<String> dir() {
        return delegate.dir();
    }

    @Override
    public Optional<List<String>> export() {
        return delegate.export();
    }

    @Override
    public boolean buildNativeImage() {
        return delegate.buildNativeImage();
    }

    @Override
    public String sidecarName() {
        return delegate.sidecarName();
    }

    @Override
    public Optional<String> nativeImageBinary() {
        return delegate.nativeImageBinary();
    }

    @Override
    public Optional<String> mainClass() {
        return delegate.mainClass();
    }

    @Override
    public boolean managed() {
        return delegate.managed();
    }

    @Override
    public Optional<List<String>> buildArgs() {
        return delegate.buildArgs();
    }

    @Override
    public Optional<String> buildConfig() {
        return delegate.buildConfig();
    }

    @Override
    public boolean verbose() {
        return delegate.verbose();
    }

    @Override
    public int devServerPort() {
        return delegate.devServerPort();
    }
}
