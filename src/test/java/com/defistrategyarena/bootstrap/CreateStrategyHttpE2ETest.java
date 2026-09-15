package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;

class CreateStrategyHttpE2ETest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_CONFLICT = 409;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int SINGLE = 1;
    private static final String PATH = "/strategies";
    private static final String JSON_TYPE = "application/json";
    private static final String OWNER = "user-42";
    private static final String NAME = "rsi-bounce";
    private static final String VALID_BODY =
            "{\"ownerId\":\""
                    + OWNER
                    + "\",\"name\":\""
                    + NAME
                    + "\",\"rules\":[{\"id\":\"r1\",\"type\":\"price_above\",\"instrument\":\"ETH-USD\",\"threshold\":\"3000\"}]}";
    private static final String INVALID_BODY = "{not-json";

    @Test
    void creates_strategy_over_jetty_and_persists() throws Exception {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        try (HttpServerRuntime runtime = start(composition)) {
            HttpResponse<String> response = post(runtime.port(), VALID_BODY);
            assertEquals(STATUS_CREATED, response.statusCode());
            assertTrue(response.body().contains("strategyId"));
            assertFalse(response.body().contains("\"strategyId\":\"\""));
            String strategyId = extractStrategyId(response.body());
            assertTrue(composition.strategies().get(new StrategyId(strategyId)).isPresent());
        }
    }

    @Test
    void rejects_duplicate_over_jetty() throws Exception {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        try (HttpServerRuntime runtime = start(composition)) {
            assertEquals(STATUS_CREATED, post(runtime.port(), VALID_BODY).statusCode());
            HttpResponse<String> second = post(runtime.port(), VALID_BODY);
            assertEquals(STATUS_CONFLICT, second.statusCode());
            assertEquals(
                    SINGLE,
                    composition
                            .strategies()
                            .countByOwnerAndName(new OwnerStrategyName(OWNER, NAME)));
        }
    }

    @Test
    void rejects_invalid_json_over_jetty() throws Exception {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        try (HttpServerRuntime runtime = start(composition)) {
            HttpResponse<String> response = post(runtime.port(), INVALID_BODY);
            assertEquals(STATUS_BAD_REQUEST, response.statusCode());
            assertEquals(0, composition.strategies().size());
        }
    }

    private static HttpServerRuntime start(ApplicationComposition composition) {
        return new JettyHttpServerBootstrap()
                .start(
                        HttpServerStartData.create(
                                new HttpServerStartData(
                                        new HttpServerConfig(EPHEMERAL_PORT),
                                        ApplicationRoutes.createDefaultRoutes(composition))));
    }

    private static HttpResponse<String> post(int port, String body) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                        .header("Content-Type", JSON_TYPE)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static String extractStrategyId(String json) {
        String marker = "\"strategyId\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
