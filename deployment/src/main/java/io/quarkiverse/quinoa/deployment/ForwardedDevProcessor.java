package io.quarkiverse.quinoa.deployment;

import static io.quarkiverse.quinoa.deployment.PackageManager.autoDetectPackageManager;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;
import static java.lang.String.join;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.QuinoaRecorder;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.resteasy.reactive.server.spi.ResumeOn404BuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

public class ForwardedDevProcessor {

    private static final Logger LOG = Logger.getLogger(ForwardedDevProcessor.class);
    private static final Predicate<Thread> PROCESS_THREAD_PREDICATE = new Predicate<>() {
        @Override
        public boolean test(Thread thread) {
            return thread.getName().matches("Process (stdout|stderr) streamer");
        }
    };
    private static final int DEFAULT_DEV_SERVER_TIMEOUT = 30000;

    private static volatile DevServicesResultBuildItem.RunningDevService devService;
    private static volatile QuinoaConfig cfg;
    private static volatile boolean first = true;

    @BuildStep(onlyIf = IsDevelopment.class)
    public ForwardedDevServerBuildItem prepareDevService(
            QuinoaConfig quinoaConfig,
            LaunchModeBuildItem launchMode,
            Optional<QuinoaDirectoryBuildItem> quinoaDir,
            BuildProducer<DevServicesResultBuildItem> devServices,
            Optional<ConsoleInstalledBuildItem> consoleInstalled,
            LoggingSetupBuildItem loggingSetup,
            CuratedApplicationShutdownBuildItem shutdown) {
        if (!quinoaDir.isPresent()) {
            return null;
        }
        if (devService != null) {
            boolean shouldShutdownTheBroker = !quinoaConfig.equals(cfg);
            if (!shouldShutdownTheBroker) {
                if (quinoaConfig.devServerPort.isEmpty()) {
                    throw new IllegalStateException(
                            "Quinoa package manager live coding shouldn't running with an empty the dev-server-port");
                }
                devServices.produce(devService.toBuildItem());
                return new ForwardedDevServerBuildItem(quinoaConfig.devServerPort.getAsInt());
            }
            shutdownDevService();
            cfg = null;
        }

        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (devService != null) {
                        shutdownDevService();
                    }
                    first = true;
                    devService = null;
                    cfg = null;
                }
            };
            shutdown.addCloseTask(closeTask, true);
        }

        if (quinoaConfig.devServerPort.isEmpty()) {
            return null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Quinoa package manager live coding dev service starting:",
                consoleInstalled,
                loggingSetup,
                PROCESS_THREAD_PREDICATE);

        PackageManager packageManager = autoDetectPackageManager(quinoaConfig.packageManager, quinoaDir.get().getDirectory());
        final AtomicReference<Process> dev = new AtomicReference<>();
        try {
            final int devServerPort = quinoaConfig.devServerPort.getAsInt();
            final int timeout = quinoaConfig.devServerTimeout.orElse(DEFAULT_DEV_SERVER_TIMEOUT);
            if (timeout < 1000) {
                throw new ConfigurationException("dev-server-timeout must be greater than 1000ms");
            }
            final long start = Instant.now().toEpochMilli();
            dev.set(packageManager.dev(devServerPort, timeout));
            compressor.close();
            final LiveCodingLogOutputFilter logOutputFilter = new LiveCodingLogOutputFilter(
                    quinoaConfig.enableDevServerLogs.orElse(false));
            LOG.infof("Quinoa package manager live coding is up and running on port: %d (in %dms)",
                    devServerPort, Instant.now().toEpochMilli() - start);
            final Closeable onClose = new Closeable() {
                @Override
                public void close() throws IOException {
                    logOutputFilter.close();
                    packageManager.stopDev(dev.get());
                }
            };
            devService = new DevServicesResultBuildItem.RunningDevService(
                    "quinoa-node-dev-process", null, onClose, Collections.emptyMap());
            devServices.produce(devService.toBuildItem());
            return new ForwardedDevServerBuildItem(devServerPort);
        } catch (Throwable t) {
            packageManager.stopDev(dev.get());
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(
            QuinoaConfig quinoaConfig,
            QuinoaRecorder recorder,
            Optional<ForwardedDevServerBuildItem> devProxy,
            CoreVertxBuildItem vertx,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<ResumeOn404BuildItem> resumeOn404) throws IOException {
        if (quinoaConfig.devServerPort.isPresent() && devProxy.isPresent()) {
            LOG.infof("Quinoa is forwarding unhandled requests to port: %d", quinoaConfig.devServerPort.getAsInt());
            resumeOn404.produce(new ResumeOn404BuildItem());
            routes.produce(RouteBuildItem.builder().orderedRoute("/*", VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 2)
                    .handler(recorder.quinoaProxyDevHandler(vertx.getVertx(), devProxy.get().getPort()))
                    .build());
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
        private AtomicReference<ScheduledFuture<?>> scheduled = new AtomicReference<>();

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
        public void close() throws IOException {
            if (thread == null) {
                return;
            }
            QuarkusConsole.removeOutputFilter(this);
            executor.shutdown();
        }

        @Override
        public boolean test(final String s, final Boolean err) {
            Thread current = Thread.currentThread();
            if (PROCESS_THREAD_PREDICATE.test(current) && !err) {
                if (!enableLogs) {
                    return false;
                }
                buffer.add(s.replaceAll("\\x1b\\[[0-9;]*[a-zA-Z]", ""));
                scheduled.getAndUpdate(new UnaryOperator<ScheduledFuture<?>>() {
                    @Override
                    public ScheduledFuture<?> apply(ScheduledFuture<?> scheduledFuture) {
                        if (scheduledFuture != null && !scheduledFuture.isDone()) {
                            scheduledFuture.cancel(true);
                        }
                        return executor.schedule(new Runnable() {
                            @Override
                            public void run() {
                                if (!buffer.isEmpty()) {
                                    LOG.infof("\u001b[33mQuinoa package manager live coding server has spoken: " + RESET
                                            + " \n%s", join("", buffer));
                                    buffer.clear();
                                }
                            }
                        }, 200, TimeUnit.MILLISECONDS);
                    }
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
