package io.quarkiverse.quinoa;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Non-generic route handler type used to register the Quinoa handlers as {@code ServiceRegistrar} services
 * (generic types such as {@code Handler<RoutingContext>} cannot be used as service types).
 */
public interface QuinoaRouteHandler extends Handler<RoutingContext> {
}
