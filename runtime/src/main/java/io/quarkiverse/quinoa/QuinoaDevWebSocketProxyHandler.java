package io.quarkiverse.quinoa;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.runtime.util.StringUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.web.RoutingContext;

class QuinoaDevWebSocketProxyHandler {
    private static final Logger LOG = Logger.getLogger(QuinoaDevWebSocketProxyHandler.class);
    private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    private static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
    private final WebSocketClient webSocketClient;
    private final QuinoaNetworkConfiguration networkConfiguration;

    QuinoaDevWebSocketProxyHandler(Vertx vertx, QuinoaNetworkConfiguration network) {
        WebSocketClientOptions options = new WebSocketClientOptions();
        options.setSsl(network.isTls());
        options.setTrustAll(network.isTlsAllowInsecure());
        options.setVerifyHost(!network.isTlsAllowInsecure());
        this.webSocketClient = vertx.createWebSocketClient(options);
        this.networkConfiguration = network;
    }

    public void handle(final RoutingContext ctx) {
        final HttpServerRequest request = ctx.request();
        // the browser handshake is completed only once the dev server accepted the WebSocket
        request.pause();
        final String forwardUri = request.uri();

        // some servers use sub-protocols like Vite which must be negotiated with the dev server
        final List<String> subProtocols = new ArrayList<>(1);
        for (String header : request.headers().getAll(SEC_WEBSOCKET_PROTOCOL)) {
            for (String subProtocol : header.split(",")) {
                if (!StringUtil.isNullOrEmpty(subProtocol.trim())) {
                    subProtocols.add(subProtocol.trim());
                }
            }
        }
        if (!subProtocols.isEmpty()) {
            LOG.debugf("Quinoa Dev WebSocket SubProtocols: %s", subProtocols);
        }

        // the extensions offered by the browser (e.g. permessage-deflate) are negotiated with Quarkus, not forwarded:
        // the dev server would use them with a client that doesn't support them
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap().addAll(request.headers())
                .remove(SEC_WEBSOCKET_EXTENSIONS);
        final WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(networkConfiguration.getHost())
                .setPort(networkConfiguration.getPort())
                .setURI(forwardUri)
                .setHeaders(headers)
                .setSubProtocols(subProtocols)
                .setAllowOriginHeader(false);
        webSocketClient.connect(options).onComplete(clientContext -> {
            if (clientContext.failed()) {
                // rejecting the handshake lets the browser client know the WebSocket was never opened
                final String error = String.format("Quinoa failed to forward WebSocket request '%s', see logs.",
                        forwardUri);
                LOG.error(error, clientContext.cause());
                ctx.response().setStatusCode(500).end(error);
                return;
            }
            LOG.infof("Quinoa Dev WebSocket Client Connected: %s:%s%s", networkConfiguration.getHost(),
                    networkConfiguration.getPort(), forwardUri);
            final WebSocket clientWs = clientContext.result();
            // messages sent by the dev server right after the handshake (e.g. Vite 'connected') are held until
            // the browser side is upgraded
            clientWs.pause();
            request.toWebSocket(r -> {
                if (r.failed()) {
                    LOG.error("Error while upgrading request to WebSocket", r.cause());
                    clientWs.close();
                    return;
                }
                final ServerWebSocket serverWs = r.result();
                LOG.debugf("Quinoa Dev WebSocket Server Connected: %s:%s%s", networkConfiguration.getHost(),
                        networkConfiguration.getPort(), forwardUri);
                if (!sameSubProtocol(serverWs.subProtocol(), clientWs.subProtocol())) {
                    // e.g. when Quarkus HTTP is virtual only, the sub-protocols accepted by Quinoa are not applied
                    LOG.errorf("Quinoa Dev WebSocket SubProtocol mismatch, browser: '%s', dev server: '%s'",
                            serverWs.subProtocol(), clientWs.subProtocol());
                    clientWs.close();
                    // the browser handshake is invalid, don't wait for a close handshake that can't happen
                    request.connection().close();
                    return;
                }

                // messages from browser forwarded to Node.js
                serverWs.exceptionHandler(
                        (e) -> LOG.errorf(e, "Quinoa Dev WebSocket Server closed with error: %s", e.getMessage()))
                        .closeHandler((__) -> {
                            if (!clientWs.isClosed()) {
                                clientWs.close();
                            }
                            LOG.debug("Quinoa Dev WebSocket Server is closed");
                        }).textMessageHandler((msg) -> {
                            LOG.debugf("Quinoa Dev WebSocket Server message:  %s", msg);
                            if (!clientWs.isClosed()) {
                                clientWs.writeTextMessage(msg);
                            }
                        }).binaryMessageHandler((buffer) -> {
                            LOG.debugf("Quinoa Dev WebSocket Server binary message: %d bytes", buffer.length());
                            if (!clientWs.isClosed()) {
                                clientWs.writeBinaryMessage(buffer);
                            }
                        });

                // messages from Node.js forwarded back to browser
                clientWs.exceptionHandler(
                        (e) -> LOG.errorf(e, "Quinoa Dev WebSocket Client closed with error: %s", e.getMessage()))
                        .closeHandler((__) -> {
                            LOG.debug("Quinoa Dev WebSocket Client is closed");
                            serverWs.close();
                        }).textMessageHandler((msg) -> {
                            LOG.debugf("Quinoa Dev WebSocket Client message: %s", msg);
                            serverWs.writeTextMessage(msg);
                        }).binaryMessageHandler((buffer) -> {
                            LOG.debugf("Quinoa Dev WebSocket Client binary message: %d bytes", buffer.length());
                            serverWs.writeBinaryMessage(buffer);
                        });

                if (clientWs.isClosed()) {
                    // the dev server closed the WebSocket before the browser side was upgraded
                    serverWs.close();
                } else {
                    clientWs.resume();
                }
            });
        });
    }

    private static boolean sameSubProtocol(String serverSubProtocol, String clientSubProtocol) {
        // no sub-protocol can be reported as null or empty
        if (StringUtil.isNullOrEmpty(serverSubProtocol)) {
            return StringUtil.isNullOrEmpty(clientSubProtocol);
        }
        return serverSubProtocol.equals(clientSubProtocol);
    }
}
