package com.github.eirslett.maven.plugins.frontend.lib;

import java.nio.file.Path;

import io.vertx.core.Vertx;

public class PackageManagerInstallFactory {

    private static final Platform defaultPlatform = Platform.guess();
    private static final String DEFAULT_CACHE_PATH = "cache";
    private final Vertx vertx;

    private final Path uiDir;
    private final Path installDir;
    private final CacheResolver cacheResolver;
    private final VertxFileDownloader fileDownloader;
    private final String userName;
    private final String password;

    public PackageManagerInstallFactory(Vertx vertx, Path uiDir, Path installDir, String userName, String password) {
        this.vertx = vertx;
        this.uiDir = uiDir;
        this.installDir = installDir;
        this.cacheResolver = getDefaultCacheResolver(installDir);
        this.userName = userName;
        this.password = password;
        fileDownloader = new VertxFileDownloader(vertx);
    }

    public NodeInstaller getNodeInstaller() {
        NodeInstaller nodeInstaller = new NodeInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
        nodeInstaller.setUserName(this.userName);
        nodeInstaller.setPassword(this.password);
        return nodeInstaller;
    }

    public NPMInstaller getNPMInstaller() {
        NPMInstaller npmInstaller = new NPMInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
        npmInstaller.setUserName(this.userName);
        npmInstaller.setPassword(this.password);
        return npmInstaller;
    }

    public PnpmInstaller getPnpmInstaller() {
        PnpmInstaller pnpmInstaller = new PnpmInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
        pnpmInstaller.setUserName(this.userName);
        pnpmInstaller.setPassword(this.password);
        return pnpmInstaller;
    }

    public YarnInstaller getYarnInstaller() {
        YarnInstaller yarnInstaller = new YarnInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
        yarnInstaller.setUserName(this.userName);
        yarnInstaller.setPassword(this.password);
        return yarnInstaller;
    }

    private NodeExecutorConfig getExecutorConfig() {
        return new InstallNodeExecutorConfig(this.getInstallConfig());
    }

    private InstallConfig getInstallConfig() {
        return new DefaultInstallConfig(installDir.toFile(), uiDir.toFile(), this.cacheResolver, defaultPlatform);
    }

    private static final CacheResolver getDefaultCacheResolver(Path root) {
        return new DirectoryCacheResolver(root.resolve("cache").toFile());
    }

}
