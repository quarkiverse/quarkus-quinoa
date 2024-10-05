package io.quarkiverse.quinoa.deployment.config.delegate;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.quinoa.deployment.config.DevServerConfig;
import io.quarkiverse.quinoa.deployment.config.FrameworkConfig;
import io.quarkiverse.quinoa.deployment.config.PackageManagerCommandConfig;
import io.quarkiverse.quinoa.deployment.config.PackageManagerInstallConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;

public class QuinoaConfigDelegate implements QuinoaConfig {
    private final QuinoaConfig delegate;

    public QuinoaConfigDelegate(QuinoaConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Boolean> enabled() {
        return delegate.enabled();
    }

    @Override
    public boolean justBuild() {
        return delegate.justBuild();
    }

    @Override
    public String uiRootPath() {
        return delegate.uiRootPath();
    }

    @Override
    public String uiDir() {
        return delegate.uiDir();
    }

    @Override
    public Optional<String> buildDir() {
        return delegate.buildDir();
    }

    @Override
    public Optional<String> packageManager() {
        return delegate.packageManager();
    }

    @Override
    public PackageManagerInstallConfig packageManagerInstall() {
        return delegate.packageManagerInstall();
    }

    @Override
    public PackageManagerCommandConfig packageManagerCommand() {
        return delegate.packageManagerCommand();
    }

    @Override
    public boolean runTests() {
        return delegate.runTests();
    }

    @Override
    public Optional<Boolean> ci() {
        return delegate.ci();
    }

    @Override
    public boolean forceInstall() {
        return delegate.forceInstall();
    }

    @Override
    public FrameworkConfig framework() {
        return delegate.framework();
    }

    @Override
    public boolean enableSPARouting() {
        return delegate.enableSPARouting();
    }

    @Override
    public Optional<List<String>> ignoredPathPrefixes() {
        return delegate.ignoredPathPrefixes();
    }

    @Override
    public DevServerConfig devServer() {
        return delegate.devServer();
    }

    @Override
    public boolean publish() {
        return delegate.publish();
    }

}
