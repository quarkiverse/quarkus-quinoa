package io.quarkiverse.quinoa.deployment.config.delegate;

import java.util.Optional;

import io.quarkiverse.quinoa.deployment.config.DevServerConfig;

public class DevServerConfigDelegate implements DevServerConfig {
    private final DevServerConfig delegate;

    public DevServerConfigDelegate(DevServerConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public boolean managed() {
        return delegate.managed();
    }

    @Override
    public Optional<Integer> port() {
        return delegate.port();
    }

    @Override
    public String host() {
        return delegate.host();
    }

    @Override
    public boolean tls() {
        return delegate.tls();
    }

    @Override
    public boolean tlsAllowInsecure() {
        return delegate.tlsAllowInsecure();
    }

    @Override
    public Optional<String> checkPath() {
        return delegate.checkPath();
    }

    @Override
    public boolean websocket() {
        return delegate.websocket();
    }

    @Override
    public int checkTimeout() {
        return delegate.checkTimeout();
    }

    @Override
    public boolean logs() {
        return delegate.logs();
    }

    @Override
    public Optional<String> indexPage() {
        return delegate.indexPage();
    }

    @Override
    public boolean directForwarding() {
        return delegate.directForwarding();
    }
}
