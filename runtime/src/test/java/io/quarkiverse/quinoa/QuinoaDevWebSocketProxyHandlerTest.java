package io.quarkiverse.quinoa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;

class QuinoaDevWebSocketProxyHandlerTest {
    private static final String HOST = "127.0.0.1";
    private static final String SUB_PROTOCOL = "test-protocol";

    private Vertx vertx;
    private final CompletableFuture<Void> devServerWebSocketClosed = new CompletableFuture<>();

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        await(vertx.close());
    }

    @Test
    void relaysMessagesWithSubProtocol() throws Exception {
        final int proxyPort = startProxy(startDevServer(false), false, true);
        assertRelaysMessages(proxyPort, SUB_PROTOCOL, new WebSocketClientOptions());
    }

    @Test
    void relaysMessagesWithoutSubProtocol() throws Exception {
        final int proxyPort = startProxy(startDevServer(false), false, true);
        assertRelaysMessages(proxyPort, null, new WebSocketClientOptions());
    }

    @Test
    void relaysMessagesToTlsDevServer() throws Exception {
        final int proxyPort = startProxy(startDevServer(true), true, true);
        assertRelaysMessages(proxyPort, SUB_PROTOCOL, new WebSocketClientOptions());
    }

    @Test
    void relaysMessagesWhenBrowserOffersCompression() throws Exception {
        // browsers always offer permessage-deflate, the dev server must not negotiate it with the Quinoa client
        final int proxyPort = startProxy(startDevServer(false), false, true);
        assertRelaysMessages(proxyPort, SUB_PROTOCOL, new WebSocketClientOptions().setTryUsePerMessageCompression(true));
    }

    @Test
    void rejectsHandshakeWhenDevServerRefusesUpgrade() throws Exception {
        final int proxyPort = startProxy(startDevServer(false), false, true);
        assertHandshakeRejected(proxyPort, "/not-a-websocket");
    }

    @Test
    void rejectsHandshakeWhenDevServerIsDown() throws Exception {
        final HttpServer devServer = vertx.createHttpServer();
        final int devServerPort = await(devServer.requestHandler(r -> r.response().end()).listen(0, HOST)).actualPort();
        await(devServer.close());
        final int proxyPort = startProxy(devServerPort, false, true);
        assertHandshakeRejected(proxyPort, "/ws");
    }

    @Test
    void closesDevServerWebSocketOnSubProtocolMismatch() throws Exception {
        // without the '*' sub-protocol Quinoa registers, the browser side negotiates no sub-protocol while the dev
        // server does, like with a virtual only Quarkus HTTP server
        final int proxyPort = startProxy(startDevServer(false), false, false);
        // a raw socket, so nothing on the browser side reacts to the invalid handshake, like with the Lambda event
        // server: the proxy itself must release both sides
        final CompletableFuture<Void> browserSocketClosed = new CompletableFuture<>();
        final NetSocket socket = await(vertx.createNetClient().connect(proxyPort, HOST));
        socket.closeHandler(__ -> browserSocketClosed.complete(null));
        socket.write("GET /ws HTTP/1.1\r\n"
                + "Host: " + HOST + ":" + proxyPort + "\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
                + "Sec-WebSocket-Version: 13\r\n"
                + "Sec-WebSocket-Protocol: " + SUB_PROTOCOL + "\r\n\r\n");
        devServerWebSocketClosed.get(5, TimeUnit.SECONDS);
        browserSocketClosed.get(5, TimeUnit.SECONDS);
    }

    /**
     * Dev server accepting WebSockets on /ws only: it sends 'connected' right after the handshake (like Vite does) and
     * echoes text messages. It accepts permessage-deflate when it is offered.
     */
    private int startDevServer(boolean tls) throws Exception {
        final HttpServerOptions options = new HttpServerOptions().setWebSocketSubProtocols(List.of(SUB_PROTOCOL))
                .setPerMessageWebSocketCompressionSupported(true);
        if (tls) {
            options.setSsl(true).setKeyCertOptions(SelfSignedCertificate.create().keyCertOptions());
        }
        final HttpServer server = vertx.createHttpServer(options).requestHandler(request -> {
            if (!"/ws".equals(request.path())) {
                request.response().setStatusCode(404).end();
                return;
            }
            request.toWebSocket().onSuccess(ws -> {
                ws.closeHandler(__ -> devServerWebSocketClosed.complete(null));
                ws.textMessageHandler(msg -> ws.writeTextMessage("echo:" + msg));
                ws.writeTextMessage("connected");
            });
        });
        return await(server.listen(0, HOST)).actualPort();
    }

    private int startProxy(int devServerPort, boolean tls, boolean acceptAnySubProtocol) throws Exception {
        final QuinoaDevWebSocketProxyHandler handler = new QuinoaDevWebSocketProxyHandler(vertx,
                new QuinoaNetworkConfiguration(tls, true, HOST, devServerPort, true));
        final Router router = Router.router(vertx);
        router.route().handler(handler::handle);
        // a browser that never answers the close handshake is released after this timeout (10s by default)
        final HttpServerOptions options = new HttpServerOptions().setWebSocketClosingTimeout(1);
        if (acceptAnySubProtocol) {
            // what Quinoa registers on the Quarkus HTTP server
            options.setWebSocketSubProtocols(List.of("*"));
        }
        return await(vertx.createHttpServer(options).requestHandler(router).listen(0, HOST)).actualPort();
    }

    private void assertRelaysMessages(int proxyPort, String subProtocol, WebSocketClientOptions clientOptions)
            throws Exception {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        // the handler is set on connection, so no message can arrive before it
        final WebSocket ws = await(vertx.createWebSocketClient(clientOptions)
                .connect(connectOptions(proxyPort, "/ws", subProtocol))
                .onSuccess(w -> w.textMessageHandler(messages::add)));
        if (subProtocol != null) {
            assertEquals(subProtocol, ws.subProtocol());
        }
        // sent by the dev server right after its handshake, it must not be lost while the browser side is upgraded
        assertEquals("connected", messages.poll(5, TimeUnit.SECONDS));
        await(ws.writeTextMessage("hello"));
        assertEquals("echo:hello", messages.poll(5, TimeUnit.SECONDS));
        await(ws.close());
        devServerWebSocketClosed.get(5, TimeUnit.SECONDS);
    }

    private void assertHandshakeRejected(int proxyPort, String path) {
        final ExecutionException e = assertThrows(ExecutionException.class,
                () -> await(vertx.createWebSocketClient().connect(connectOptions(proxyPort, path, SUB_PROTOCOL))));
        assertEquals(500, assertInstanceOf(UpgradeRejectedException.class, e.getCause()).getStatus());
    }

    private static WebSocketConnectOptions connectOptions(int port, String uri, String subProtocol) {
        final WebSocketConnectOptions options = new WebSocketConnectOptions().setHost(HOST).setPort(port).setURI(uri);
        if (subProtocol != null) {
            options.addSubProtocol(subProtocol);
        }
        return options;
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
}
