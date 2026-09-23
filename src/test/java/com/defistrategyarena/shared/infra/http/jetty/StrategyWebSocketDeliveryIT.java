package com.defistrategyarena.shared.infra.http.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.bootstrap.ApplicationComposition;
import com.defistrategyarena.bootstrap.ApplicationRoutes;
import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.Test;

class StrategyWebSocketDeliveryIT {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_ACCEPTED = 202;
    private static final int SINGLE = 1;
    private static final long WAIT_SECONDS = 5L;
    private static final String EMAIL = "ws-it@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "WsIt";
    private static final String JSON_TYPE = "application/json";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CREATE_BODY =
            "{\"name\":\"ws-alpha\",\"rules\":[{\"id\":\"r1\",\"conditionType\":\"price_above\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"3000\",\"allocationPercent\":\"\"}]}";

    @Test
    void authenticated_websocket_receives_create_completed() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime =
                        new JettyHttpServerBootstrap()
                                .start(
                                        HttpServerStartData.create(
                                                new HttpServerStartData(
                                                        new HttpServerConfig(EPHEMERAL_PORT),
                                                        ApplicationRoutes.createDefaultRoutes(
                                                                composition),
                                                        Optional.of(
                                                                composition.webSocketBinding()))))) {
            String token =
                    composition
                            .identityHttp()
                            .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME))
                            .accessToken();

            WebSocketClient client = new WebSocketClient();
            client.start();
            CountDownLatch authOk = new CountDownLatch(SINGLE);
            CountDownLatch completed = new CountDownLatch(SINGLE);
            AtomicReference<String> lastMessage = new AtomicReference<>();
            ClientEndpoint endpoint = new ClientEndpoint(authOk, completed, lastMessage);
            CompletableFuture<Session> connect =
                    client.connect(
                            endpoint, URI.create("ws://localhost:" + runtime.port() + "/ws"));
            Session session = connect.get(WAIT_SECONDS, TimeUnit.SECONDS);
            session.sendText(
                    "{\"type\":\"auth\",\"accessToken\":\"" + token + "\"}", null);
            assertTrue(authOk.await(WAIT_SECONDS, TimeUnit.SECONDS));

            HttpResponse<String> response =
                    HttpClient.newHttpClient()
                            .send(
                                    HttpRequest.newBuilder(
                                                    URI.create(
                                                            "http://localhost:"
                                                                    + runtime.port()
                                                                    + "/strategies"))
                                            .timeout(Duration.ofSeconds(WAIT_SECONDS))
                                            .header("Content-Type", JSON_TYPE)
                                            .header(AUTHORIZATION, BEARER_PREFIX + token)
                                            .POST(HttpRequest.BodyPublishers.ofString(CREATE_BODY))
                                            .build(),
                                    HttpResponse.BodyHandlers.ofString());
            assertEquals(STATUS_ACCEPTED, response.statusCode());
            assertTrue(completed.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertTrue(lastMessage.get().contains("strategy.create"));
            assertTrue(lastMessage.get().contains("completed"));
            assertEquals(SINGLE, composition.strategies().size());
            session.close();
            client.stop();
        }
    }

    @WebSocket
    public static final class ClientEndpoint {
        private final CountDownLatch authOk;
        private final CountDownLatch completed;
        private final AtomicReference<String> lastMessage;

        ClientEndpoint(
                CountDownLatch authOk,
                CountDownLatch completed,
                AtomicReference<String> lastMessage) {
            this.authOk = authOk;
            this.completed = completed;
            this.lastMessage = lastMessage;
        }

        @OnWebSocketOpen
        public void onOpen(Session session) {}

        @OnWebSocketMessage
        public void onMessage(String message) {
            if (message.contains("\"status\":\"ok\"")) {
                authOk.countDown();
            }
            if (message.contains("strategy.create")) {
                lastMessage.set(message);
                completed.countDown();
            }
        }
    }
}
