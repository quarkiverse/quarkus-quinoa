package io.quarkiverse.quinoa.deployment.config.delegate;

import java.util.Map;
import java.util.Optional;

import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;

public class PackageManagerCommandConfigDelegate implements PackageManagerCommandConfig {

    private final PackageManagerCommandConfig delegate;

    public PackageManagerCommandConfigDelegate(PackageManagerCommandConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<String> ci() {
        return delegate.ci();
    }

    @Override
    public Map<String, String> ciEnv() {
        return delegate.ciEnv();
    }

    @Override
    public Optional<String> install() {
        return delegate.install();
    }

    @Override
    public Map<String, String> installEnv() {
        return delegate.installEnv();
    }

    @Override
    public Optional<String> build() {
        return delegate.build();
    }

    @Override
    public Map<String, String> buildEnv() {
        return delegate.buildEnv();
    }

    @Override
    public Optional<String> test() {
        return delegate.test();
    }

    @Override
    public Map<String, String> testEnv() {
        return delegate.testEnv();
    }

    @Override
    public Optional<String> dev() {
        return delegate.dev();
    }

    @Override
    public Map<String, String> devEnv() {
        return delegate.devEnv();
    }
}
