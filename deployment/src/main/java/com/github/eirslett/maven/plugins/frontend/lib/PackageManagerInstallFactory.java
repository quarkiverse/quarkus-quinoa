package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.nio.file.Path;

import io.vertx.core.Vertx;

public class PackageManagerInstallFactory {

    private static final Platform defaultPlatform = Platform.guess();
    private static final String DEFAULT_CACHE_PATH = "cache";
    private final Vertx vertx;
    private final Path installDirectory;
    private final CacheResolver cacheResolver;
    private final VertxFileDownloader fileDownloader;

    public PackageManagerInstallFactory(Vertx vertx, Path installDirectory) {
        this.vertx = vertx;
        this.installDirectory = installDirectory;
        this.cacheResolver = getDefaultCacheResolver(installDirectory);
        fileDownloader = new VertxFileDownloader(vertx);
    }

    public NodeInstaller getNodeInstaller() {

        return new NodeInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
    }

    public NPMInstaller getNPMInstaller() {
        return new NPMInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
    }

    public PnpmInstaller getPnpmInstaller() {
        return new PnpmInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
    }

    public YarnInstaller getYarnInstaller() {
        return new YarnInstaller(this.getInstallConfig(), new DefaultArchiveExtractor(), fileDownloader);
    }

    private NodeExecutorConfig getExecutorConfig() {
        return new InstallNodeExecutorConfig(this.getInstallConfig());
    }

    private InstallConfig getInstallConfig() {
        final File installDirFile = this.installDirectory.toFile();
        return new DefaultInstallConfig(installDirFile, installDirFile, this.cacheResolver, defaultPlatform);
    }

    private static final CacheResolver getDefaultCacheResolver(Path root) {
        return new DirectoryCacheResolver(root.resolve("cache").toFile());
    }

}
