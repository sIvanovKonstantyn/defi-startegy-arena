package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyReadsHttpE2ETest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_NOT_FOUND = 404;
    private static final String OWNER = "owner-http-reads";
    private static final String OTHER = "other-http";
    private static final String JSON_TYPE = "application/json";
    private static final String QUERY_START = "?";
    private static final String QUERY_SEP = "&";
    private static final String QUERY_EQ = "=";
    private static final String CREATE_BODY_A =
            "{\"ownerId\":\""
                    + OWNER
                    + "\",\"name\":\"alpha\",\"rules\":[{\"id\":\"r1\",\"conditionType\":\"price_above\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"3000\",\"allocationPercent\":\"\"}]}";
    private static final String CREATE_BODY_B =
            "{\"ownerId\":\""
                    + OWNER
                    + "\",\"name\":\"beta\",\"rules\":[{\"id\":\"r1\",\"conditionType\":\"price_above\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"3000\",\"allocationPercent\":\"\"}]}";

    @Test
    void lists_and_gets_over_jetty() throws Exception {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        try (HttpServerRuntime runtime = start(composition)) {
            assertEquals(STATUS_CREATED, post(runtime.port(), "/strategies", CREATE_BODY_A).statusCode());
            assertEquals(STATUS_CREATED, post(runtime.port(), "/strategies", CREATE_BODY_B).statusCode());

            HttpResponse<String> list =
                    get(
                            runtime.port(),
                            "/strategies",
                            Map.of(
                                    "ownerId", OWNER,
                                    "page", "0",
                                    "size", "1",
                                    "sort", "name",
                                    "order", "asc"));
            assertEquals(STATUS_OK, list.statusCode());
            assertTrue(list.body().contains("\"totalElements\":2"));
            assertTrue(list.body().contains("\"name\":\"alpha\""));

            String strategyId = extractStrategyId(list.body());
            HttpResponse<String> detail =
                    get(runtime.port(), "/strategies/" + strategyId, Map.of("ownerId", OWNER));
            assertEquals(STATUS_OK, detail.statusCode());
            assertTrue(detail.body().contains("\"conditionType\":\"price_above\""));

            HttpResponse<String> forbidden =
                    get(runtime.port(), "/strategies/" + strategyId, Map.of("ownerId", OTHER));
            assertEquals(STATUS_NOT_FOUND, forbidden.statusCode());

            HttpResponse<String> badPage =
                    get(
                            runtime.port(),
                            "/strategies",
                            Map.of("ownerId", OWNER, "page", "-1", "size", "20"));
            assertEquals(STATUS_BAD_REQUEST, badPage.statusCode());

            HttpResponse<String> blankOwner =
                    get(runtime.port(), "/strategies", Map.of("ownerId", " "));
            assertEquals(STATUS_BAD_REQUEST, blankOwner.statusCode());

            HttpResponse<String> missingOwner = get(runtime.port(), "/strategies", Map.of());
            assertEquals(STATUS_BAD_REQUEST, missingOwner.statusCode());

            HttpResponse<String> badInt =
                    get(
                            runtime.port(),
                            "/strategies",
                            Map.of("ownerId", OWNER, "page", "x", "size", "20"));
            assertEquals(STATUS_BAD_REQUEST, badInt.statusCode());

            HttpResponse<String> defaults =
                    get(runtime.port(), "/strategies", Map.of("ownerId", OWNER));
            assertEquals(STATUS_OK, defaults.statusCode());
            assertTrue(defaults.body().contains("\"sort\":\"name\""));

            HttpResponse<String> flagOnly =
                    sendGet(runtime.port(), "/strategies?ownerId=" + OWNER + "&debug");
            assertEquals(STATUS_OK, flagOnly.statusCode());

            HttpResponse<String> emptyQuery = sendGet(runtime.port(), "/strategies?");
            assertEquals(STATUS_BAD_REQUEST, emptyQuery.statusCode());
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

    private static HttpResponse<String> post(int port, String path, String body) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", JSON_TYPE)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(int port, String path, Map<String, String> query)
            throws Exception {
        StringBuilder uri = new StringBuilder("http://localhost:" + port + path);
        if (!query.isEmpty()) {
            uri.append(QUERY_START);
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    uri.append(QUERY_SEP);
                }
                first = false;
                uri.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                uri.append(QUERY_EQ);
                uri.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sendGetUri(uri.toString());
    }

    private static HttpResponse<String> sendGet(int port, String pathAndQuery) throws Exception {
        return sendGetUri("http://localhost:" + port + pathAndQuery);
    }

    private static HttpResponse<String> sendGetUri(String uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static String extractStrategyId(String json) {
        String marker = "\"strategyId\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
