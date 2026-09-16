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

class StrategyUpdateDeleteHttpE2ETest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_NOT_FOUND = 404;
    private static final String OWNER = "owner-http-update";
    private static final String OTHER = "other-http";
    private static final String JSON_TYPE = "application/json";
    private static final String QUERY_START = "?";
    private static final String QUERY_SEP = "&";
    private static final String QUERY_EQ = "=";
    private static final String CREATE_BODY =
            "{\"ownerId\":\""
                    + OWNER
                    + "\",\"name\":\"alpha\",\"rules\":[{\"id\":\"r1\",\"conditionType\":\"price_above\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"3000\",\"allocationPercent\":\"\"}]}";
    private static final String UPDATE_BODY =
            "{\"rules\":[{\"id\":\"r2\",\"conditionType\":\"price_under\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"2500\",\"allocationPercent\":\"\"}]}";
    private static final String EMPTY_RULES_BODY = "{\"rules\":[]}";

    @Test
    void updates_and_deletes_over_jetty() throws Exception {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        try (HttpServerRuntime runtime = start(composition)) {
            HttpResponse<String> created = post(runtime.port(), "/strategies", CREATE_BODY);
            assertEquals(STATUS_CREATED, created.statusCode());
            String strategyId = extractField(created.body(), "strategyId");

            HttpResponse<String> updated =
                    put(
                            runtime.port(),
                            "/strategies/" + strategyId,
                            Map.of("ownerId", OWNER),
                            UPDATE_BODY);
            assertEquals(STATUS_OK, updated.statusCode());
            assertTrue(updated.body().contains("\"versionNumber\":2"));

            HttpResponse<String> detail =
                    get(runtime.port(), "/strategies/" + strategyId, Map.of("ownerId", OWNER));
            assertEquals(STATUS_OK, detail.statusCode());
            assertTrue(detail.body().contains("\"name\":\"alpha\""));
            assertTrue(detail.body().contains("\"conditionType\":\"price_under\""));

            HttpResponse<String> forbiddenUpdate =
                    put(
                            runtime.port(),
                            "/strategies/" + strategyId,
                            Map.of("ownerId", OTHER),
                            UPDATE_BODY);
            assertEquals(STATUS_NOT_FOUND, forbiddenUpdate.statusCode());

            HttpResponse<String> badRules =
                    put(
                            runtime.port(),
                            "/strategies/" + strategyId,
                            Map.of("ownerId", OWNER),
                            EMPTY_RULES_BODY);
            assertEquals(STATUS_BAD_REQUEST, badRules.statusCode());

            HttpResponse<String> deleted =
                    delete(runtime.port(), "/strategies/" + strategyId, Map.of("ownerId", OWNER));
            assertEquals(STATUS_OK, deleted.statusCode());

            HttpResponse<String> gone =
                    get(runtime.port(), "/strategies/" + strategyId, Map.of("ownerId", OWNER));
            assertEquals(STATUS_NOT_FOUND, gone.statusCode());

            HttpResponse<String> blankOwner =
                    delete(runtime.port(), "/strategies/" + strategyId, Map.of("ownerId", " "));
            assertEquals(STATUS_BAD_REQUEST, blankOwner.statusCode());
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

    private static HttpResponse<String> put(
            int port, String path, Map<String, String> query, String body) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(uri(port, path, query)))
                        .header("Content-Type", JSON_TYPE)
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(int port, String path, Map<String, String> query)
            throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(uri(port, path, query))).DELETE().build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(int port, String path, Map<String, String> query)
            throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(uri(port, path, query))).GET().build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static String uri(int port, String path, Map<String, String> query) {
        StringBuilder uri = new StringBuilder("http://localhost:" + port + path + QUERY_START);
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
        return uri.toString();
    }

    private static String extractField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
