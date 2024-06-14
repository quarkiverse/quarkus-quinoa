package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.QuinoaRecorder.QUINOA_SPA_ROUTE_ORDER;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.isDevServerMode;
import static io.quarkiverse.quinoa.deployment.config.QuinoaConfig.toDevProxyHandlerConfig;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner.DEV_PROCESS_THREAD_PREDICATE;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;
import static java.lang.String.join;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.QuinoaDevProxyHandlerConfig;
import io.quarkiverse.quinoa.QuinoaRecorder;
import io.quarkiverse.quinoa.deployment.config.DevServerConfig;
import io.quarkiverse.quinoa.deployment.config.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.items.ConfiguredQuinoaBuildItem;
import io.quarkiverse.quinoa.deployment.items.ForwardedDevServerBuildItem;
import io.quarkiverse.quinoa.deployment.items.InstalledPackageManagerBuildItem;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.resteasy.reactive.server.spi.ResumeOn404BuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.WebsocketSubProtocolsBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;

public class ForwardedDevProcessor {

    private static final Logger LOG = Logger.getLogger(ForwardedDevProcessor.class);
    private static final String DEV_SERVICE_NAME = "quinoa-dev-server";
    private static volatile DevServicesResultBuildItem.RunningDevService devService;

    @BuildStep(onlyIf = IsDevelopment.class)
    public ForwardedDevServerBuildItem prepareDevService(
            LaunchModeBuildItem launchMode,
            ConfiguredQuinoaBuildItem configuredQuinoa,
            InstalledPackageManagerBuildItem installedPackageManager,
            QuinoaConfig userConfig,
            BuildProducer<DevServicesResultBuildItem> devServices,
            Optional<ConsoleInstalledBuildItem> consoleInstalled,
            LoggingSetupBuildItem loggingSetup,
            CuratedApplicationShutdownBuildItem shutdown,
            LiveReloadBuildItem liveReload) {
        if (configuredQuinoa == null) {
            return null;
        }
        QuinoaConfig oldConfig = liveReload.getContextObject(QuinoaConfig.class);
        final QuinoaConfig resolvedConfig = configuredQuinoa.resolvedConfig();
        final DevServerConfig devServerConfig = resolvedConfig.devServer();
        liveReload.setContextObject(QuinoaConfig.class, resolvedConfig);
        final String configuredDevServerHost = devServerConfig.host();
        final boolean configuredTls = devServerConfig.tls();
        final boolean configuredTlsAllowInsecure = devServerConfig.tlsAllowInsecure();
        final PackageManagerRunner packageManagerRunner = installedPackageManager.getPackageManager();
        final String checkPath = resolvedConfig.devServer().checkPath().orElse(null);
        if (devService != null) {

            boolean shouldShutdownTheBroker = !QuinoaConfig.isEqual(resolvedConfig, oldConfig)
                    || QuinoaProcessor.isPackageJsonLiveReloadChanged(configuredQuinoa, liveReload);
            if (!shouldShutdownTheBroker) {
                if (devServerConfig.port().isEmpty()) {
                    throw new IllegalStateException(
                            "Quinoa package manager live coding shouldn't running with an empty the dev-server.port");
                }
                LOG.debug("Quinoa config did not change; no need to restart.");
                devServices.produce(devService.toBuildItem());
                final String resolvedDevServerHost = PackageManagerRunner.isDevServerUp(devServerConfig.tls(),
                        devServerConfig.tlsAllowInsecure(),
                        devServerConfig.host(),
                        devServerConfig.port().get(),
                        checkPath);
                return new ForwardedDevServerBuildItem(resolvedDevServerHost, devServerConfig.port().get());
            }
            shutdownDevService();
        }

        if (oldConfig == null) {
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownDevService();
                }
                devService = null;
            };
            shutdown.addCloseTask(closeTask, true);
        }

        if (!isDevServerMode(configuredQuinoa.resolvedConfig())) {
            return null;
        }
        final Integer port = devServerConfig.port().get();

        if (!devServerConfig.managed()) {
            // No need to start the dev-service it is not managed by Quinoa
            // We just check that it is up
            final String resolvedHostIPAddress = PackageManagerRunner.isDevServerUp(configuredTls, configuredTlsAllowInsecure,
                    configuredDevServerHost, port, checkPath);
            if (resolvedHostIPAddress != null) {
                return new ForwardedDevServerBuildItem(resolvedHostIPAddress, port);
            } else {
                throw new IllegalStateException(
                        "The Web UI dev server (configured as not managed by Quinoa) is not started on port: " + port);
            }
        }

        final int checkTimeout = devServerConfig.checkTimeout();
        if (checkTimeout < 1000) {
            throw new ConfigurationException("quarkus.quinoa.dev-server.check-timeout must be greater than 1000ms");
        }
        final long start = Instant.now().toEpochMilli();
        final AtomicReference<Process> dev = new AtomicReference<>();
        PackageManagerRunner.DevServer devServer = null;
        try {
            devServer = packageManagerRunner.dev(consoleInstalled, loggingSetup, configuredTls, configuredTlsAllowInsecure,
                    configuredDevServerHost,
                    port,
                    checkPath,
                    checkTimeout);
            dev.set(devServer.process());
            devServer.logCompressor().close();
            final LiveCodingLogOutputFilter logOutputFilter = new LiveCodingLogOutputFilter(
                    devServerConfig.logs());
            if (checkPath != null) {
                LOG.infof("Quinoa package manager live coding is up and running on port: %d (in %dms)",
                        port, Instant.now().toEpochMilli() - start);
            }
            final Closeable onClose = () -> {
                logOutputFilter.close();
                packageManagerRunner.stopDev(dev.get());
            };
            Map<String, String> devServerConfigMap = createDevServiceMapForDevUI(userConfig);
            devService = new DevServicesResultBuildItem.RunningDevService(
                    DEV_SERVICE_NAME, null, onClose, devServerConfigMap);
            devServices.produce(devService.toBuildItem());
            return new ForwardedDevServerBuildItem(devServer.hostIPAddress(), port);
        } catch (Throwable t) {
            packageManagerRunner.stopDev(dev.get());
            if (devServer != null) {
                devServer.logCompressor().closeAndDumpCaptured();
            }
            throw new RuntimeException(t);
        }
    }

    private static Map<String, String> createDevServiceMapForDevUI(QuinoaConfig quinoaConfig) {
        Map<String, String> devServerConfigMap = new LinkedHashMap<>();
        devServerConfigMap.put("quarkus.quinoa.dev-server.host", quinoaConfig.devServer().host());
        devServerConfigMap.put("quarkus.quinoa.dev-server.port",
                quinoaConfig.devServer().port().map(p -> p.toString()).orElse(""));
        devServerConfigMap.put("quarkus.quinoa.dev-server.check-timeout",
                Integer.toString(quinoaConfig.devServer().checkTimeout()));
        devServerConfigMap.put("quarkus.quinoa.dev-server.check-path", quinoaConfig.devServer().checkPath().orElse(""));
        devServerConfigMap.put("quarkus.quinoa.dev-server.managed", Boolean.toString(quinoaConfig.devServer().managed()));
        devServerConfigMap.put("quarkus.quinoa.dev-server.logs", Boolean.toString(quinoaConfig.devServer().logs()));
        devServerConfigMap.put("quarkus.quinoa.dev-server.websocket", Boolean.toString(quinoaConfig.devServer().websocket()));
        return devServerConfigMap;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(RUNTIME_INIT)
    public void runtimeInit(
            QuinoaRecorder recorder,
            HttpBuildTimeConfig httpBuildTimeConfig,
            Optional<ForwardedDevServerBuildItem> devProxy,
            Optional<ConfiguredQuinoaBuildItem> configuredQuinoa,
            CoreVertxBuildItem vertx,
            HttpRootPathBuildItem httpRootPath,
            NonApplicationRootPathBuildItem nonApplicationRootPath,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<WebsocketSubProtocolsBuildItem> websocketSubProtocols,
            BuildProducer<ResumeOn404BuildItem> resumeOn404) throws IOException {

        if (configuredQuinoa.isPresent() && devProxy.isPresent()) {
            final QuinoaConfig quinoaConfig = configuredQuinoa.get().resolvedConfig();
            if (quinoaConfig.justBuild()) {
                LOG.info("Quinoa is in build only mode");
                return;
            }
            LOG.infof("Quinoa is forwarding unhandled requests to port: %d", devProxy.get().getPort());
            final QuinoaDevProxyHandlerConfig handlerConfig = toDevProxyHandlerConfig(quinoaConfig, httpBuildTimeConfig,
                    nonApplicationRootPath);
            String uiRootPath = QuinoaConfig.getNormalizedUiRootPath(quinoaConfig);
            recorder.logUiRootPath(httpRootPath.relativePath(uiRootPath));
            // note that the uiRootPath is resolved relative to 'quarkus.http.root-path' by the RouteBuildItem
            routes.produce(RouteBuildItem.builder().orderedRoute(uiRootPath + "*", QUINOA_ROUTE_ORDER)
                    .handler(recorder.quinoaProxyDevHandler(handlerConfig, vertx.getVertx(), devProxy.get().getHost(),
                            devProxy.get().getPort(),
                            quinoaConfig.devServer().websocket()))
                    .build());
            if (quinoaConfig.devServer().websocket()) {
                websocketSubProtocols.produce(new WebsocketSubProtocolsBuildItem("*"));
            }
            if (quinoaConfig.enableSPARouting()) {
                resumeOn404.produce(new ResumeOn404BuildItem());
                routes.produce(RouteBuildItem.builder().orderedRoute(uiRootPath + "*", QUINOA_SPA_ROUTE_ORDER)
                        .handler(recorder.quinoaSPARoutingHandler(handlerConfig.ignoredPathPrefixes))
                        .build());
            }
        }
    }

    private void shutdownDevService() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                LOG.error("Failed to stop Quinoa package manager live coding", e);
            } finally {
                devService = null;
            }
        }
    }

    private static class LiveCodingLogOutputFilter implements Closeable, BiPredicate<String, Boolean> {
        private final ScheduledThreadPoolExecutor executor;
        private final Thread thread;
        private final List<String> buffer = Collections.synchronizedList(new ArrayList<>());
        private final boolean enableLogs;
        private final AtomicReference<ScheduledFuture<?>> scheduled = new AtomicReference<>();

        public LiveCodingLogOutputFilter(boolean enableLogs) {
            this.enableLogs = enableLogs;
            if (QuarkusConsole.INSTANCE.isAnsiSupported()) {
                executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
                        new LiveCodingLoggingThreadFactory());
                executor.setRemoveOnCancelPolicy(true);
                QuarkusConsole.installRedirects();
                this.thread = Thread.currentThread();
                QuarkusConsole.addOutputFilter(this);
            } else {
                executor = null;
                thread = null;
            }
        }

        @Override
        public void close() {
            if (thread == null) {
                return;
            }
            QuarkusConsole.removeOutputFilter(this);
            executor.shutdown();
        }

        @Override
        public boolean test(final String s, final Boolean err) {
            Thread current = Thread.currentThread();
            if (DEV_PROCESS_THREAD_PREDICATE.test(current) && !err) {
                if (!enableLogs) {
                    return false;
                }
                buffer.add(s.replaceAll("\\x1b\\[[0-9;]*[a-zA-Z]", ""));
                scheduled.getAndUpdate(scheduledFuture -> {
                    if (scheduledFuture != null && !scheduledFuture.isDone()) {
                        scheduledFuture.cancel(true);
                    }
                    return executor.schedule(() -> {
                        if (!buffer.isEmpty()) {
                            LOG.infof("\u001b[33mQuinoa package manager live coding server has spoken: " + RESET
                                    + " \n%s", join("", buffer));
                            buffer.clear();
                        }
                    }, 200, TimeUnit.MILLISECONDS);
                });
                return false;
            }
            return true;
        }

        static class LiveCodingLoggingThreadFactory implements ThreadFactory {
            public Thread newThread(Runnable r) {
                return new Thread(r, "Live coding server");
            }
        }

    }

}
