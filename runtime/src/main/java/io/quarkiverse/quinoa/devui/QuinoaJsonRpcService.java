package io.quarkiverse.quinoa.devui;

import java.util.Collections;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.dev.console.DevConsoleManager;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
public class QuinoaJsonRpcService {

    private static final Logger LOG = Logger.getLogger(QuinoaJsonRpcService.class);

    @PostConstruct
    void init() {
        // no-op for now
    }

    public Multi<String> install() throws Exception {
        LOG.info("Stopping Node server, installing new packages, and restarting...");
        Map<String, String> params = Collections.emptyMap();

        // For now, the JSON RPC are called on the event loop, but the action is blocking,
        // So, work around this by invoking the action on a worker thread.
        Multi<String> install = Uni.createFrom().item(() -> DevConsoleManager
                .<String> invoke("quinoa-install-action", params))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // It's a blocking action.
                .toMulti();

        return Multi.createBy().concatenating()
                .streams(Multi.createFrom().item("started"), install);
    }

}