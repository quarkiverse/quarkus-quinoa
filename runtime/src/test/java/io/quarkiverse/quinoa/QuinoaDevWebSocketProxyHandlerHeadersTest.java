package io.quarkiverse.quinoa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.web.Router;

/**
 * What the dev server receives in the WebSocket handshake forwarded by Quinoa. Dev servers (Vite, webpack-dev-server,
 * Next.js) decide from these headers whether the browser is allowed to connect, so the proxy must relay them as they
 * were sent, and must not invent any.
 */
class QuinoaDevWebSocketProxyHandlerHeadersTest {
    private static final String HOST = "127.0.0.1";
    private static final String SUB_PROTOCOL = "vite-hmr";

    private Vertx vertx;
    // Vert.x 5 closes a client once it is no longer reachable, it is kept until Vert.x is closed
    private WebSocketClient client;
    /** completed with the handshake request headers and URI, as received by the dev server */
    private final CompletableFuture<Handshake> devServerHandshake = new CompletableFuture<>();

    record Handshake(String uri, MultiMap headers) {
    }

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        client = vertx.createWebSocketClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        await(vertx.close());
    }

    @Test
    void forwardsTheBrowserOriginUnchanged() throws Exception {
        final int proxyPort = startProxy(startRecordingDevServer());
        final String browserOrigin = "http://localhost:" + proxyPort;
        final Handshake handshake = connect(proxyPort, "/ws", browserOrigin);
        // Vite requires its token only when an Origin is present, Next.js and webpack-dev-server validate the Origin
        // host: a proxy that drops or rewrites the header defeats those checks
        assertEquals(browserOrigin, handshake.headers().get(HttpHeaders.ORIGIN));
    }

    @Test
    void doesNotAddAnOriginWhenTheClientSentNone() throws Exception {
        final int proxyPort = startProxy(startRecordingDevServer());
        final Handshake handshake = connect(proxyPort, "/ws", null);
        // a non-browser client sends no Origin and dev servers treat it as such; the WebSocket client must not
        // generate one from the dev server URL
        assertNull(handshake.headers().get(HttpHeaders.ORIGIN));
    }

    @Test
    void forwardsTheRequestUriWithItsQuery() throws Exception {
        final int proxyPort = startProxy(startRecordingDevServer());
        // Vite puts its per-process token in the query of the HMR WebSocket URL
        final Handshake handshake = connect(proxyPort, "/ws?token=abc123", "http://localhost:" + proxyPort);
        assertEquals("/ws?token=abc123", handshake.uri());
    }

    @Test
    void forwardsTheHostAndSubProtocolUnchanged() throws Exception {
        final int proxyPort = startProxy(startRecordingDevServer());
        final Handshake handshake = connect(proxyPort, "/ws", "http://localhost:" + proxyPort);
        // Vite (allowedHosts), Next.js (allowedDevOrigins) and webpack-dev-server validate the Host header
        assertEquals("localhost:" + proxyPort, handshake.headers().get(HttpHeaders.HOST));
        assertEquals(SUB_PROTOCOL, handshake.headers().get("Sec-WebSocket-Protocol"));
    }

    /** Dev server accepting WebSockets on /ws and recording the handshake it received. */
    private int startRecordingDevServer() throws Exception {
        final HttpServerOptions options = new HttpServerOptions().setWebSocketSubProtocols(List.of(SUB_PROTOCOL));
        return await(vertx.createHttpServer(options).requestHandler(request -> {
            if (!request.path().equals("/ws")) {
                request.response().setStatusCode(404).end();
                return;
            }
            final MultiMap headers = MultiMap.caseInsensitiveMultiMap().addAll(request.headers());
            request.toWebSocket().onSuccess(ws -> {
                devServerHandshake.complete(new Handshake(request.uri(), headers));
                ws.writeTextMessage("connected");
            }).onFailure(devServerHandshake::completeExceptionally);
        }).listen(0, HOST)).actualPort();
    }

    private int startProxy(int devServerPort) throws Exception {
        final QuinoaDevWebSocketProxyHandler handler = new QuinoaDevWebSocketProxyHandler(vertx,
                new QuinoaNetworkConfiguration(false, false, HOST, devServerPort, true));
        final Router router = Router.router(vertx);
        router.route().handler(handler::handle);
        // what Quinoa registers on the Quarkus HTTP server
        final HttpServerOptions options = new HttpServerOptions().setWebSocketSubProtocols(List.of("*"));
        return await(vertx.createHttpServer(options).requestHandler(router).listen(0, HOST)).actualPort();
    }

    /** Connects like a browser would: an explicit Host, an Origin when given, and the sub-protocol. */
    private Handshake connect(int proxyPort, String uri, String origin) throws Exception {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap().set(HttpHeaders.HOST, "localhost:" + proxyPort);
        if (origin != null) {
            headers.set(HttpHeaders.ORIGIN, origin);
        }
        final WebSocketConnectOptions options = new WebSocketConnectOptions().setHost(HOST).setPort(proxyPort).setURI(uri)
                .setHeaders(headers)
                // with false the Vert.x client removes the Origin header, with true it keeps the one set above (and
                // would generate one if absent): this sends exactly the headers a browser would
                .setAllowOriginHeader(origin != null)
                .addSubProtocol(SUB_PROTOCOL);
        final WebSocket ws = await(client.connect(options));
        try {
            return devServerHandshake.get(5, TimeUnit.SECONDS);
        } finally {
            ws.close();
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
}
