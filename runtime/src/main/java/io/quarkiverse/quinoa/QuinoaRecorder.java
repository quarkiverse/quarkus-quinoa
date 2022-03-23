package io.quarkiverse.quinoa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class QuinoaRecorder {
    private static final Logger log = Logger.getLogger(QuinoaRecorder.class);
    public static final String META_INF_UI = "META-INF/ui";

    public Consumer<Route> start(final String directory, final Set<String> uiResources) throws IOException {
        List<Handler<RoutingContext>> handlers = new ArrayList<>();
        if (!uiResources.isEmpty()) {
            handlers.add(new QuinoaUIResourceHandler(directory, uiResources));
        }
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                for (Handler<RoutingContext> handler : handlers) {
                    route.handler(handler);
                }
            }
        };
    }

    private static class QuinoaUIResourceHandler implements Handler<RoutingContext> {
        private final Set<String> uiResources;
        private final Handler<RoutingContext> staticHandler;
        private final ClassLoader currentClassLoader;

        private QuinoaUIResourceHandler(String directory, Set<String> uiResources) {
            this.uiResources = uiResources;
            this.staticHandler = directory != null ? StaticHandler.create(FileSystemAccess.ROOT, directory)
                    : StaticHandler.create(META_INF_UI);
            currentClassLoader = Thread.currentThread().getContextClassLoader();
        }

        @Override
        public void handle(RoutingContext ctx) {
            String rel = ctx.mountPoint() == null ? ctx.normalizedPath()
                    : ctx.normalizedPath().substring(
                            // let's be extra careful here in case Vert.x normalizes the mount points at some point
                            ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
            if (uiResources.contains(rel) || Objects.equals(rel, "/")) {
                log.infof("Quinoa is serving: '%s'", rel);
                staticHandler.handle(ctx);
            } else {
                // make sure we don't lose the correct TCCL to Vert.x...
                Thread.currentThread().setContextClassLoader(currentClassLoader);
                ctx.next();
            }
        }
    }

}
