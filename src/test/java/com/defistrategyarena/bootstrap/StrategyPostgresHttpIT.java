package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import com.defistrategyarena.shared.infra.persistence.JdbcSettings;
import com.defistrategyarena.strategy.integration.H2PostgresModeSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyPostgresHttpIT {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_NOT_FOUND = 404;
    private static final String JSON_TYPE = "application/json";
    private static final String OWNER = "owner-pg-http";
    private static final String DB_NAME = "strategy_http_it";
    private static final String CREATE_BODY =
            """
            {"ownerId":"owner-pg-http","name":"pg-crud","rules":[{"id":"r1","conditionType":"price_above","actionType":"hold","instrument":"ETH-USD","indicator":"","threshold":"3000","allocationPercent":""}]}
            """;
    private static final String UPDATE_BODY =
            """
            {"rules":[{"id":"r1","conditionType":"price_under","actionType":"hold","instrument":"ETH-USD","indicator":"","threshold":"2500","allocationPercent":""}]}
            """;

    @Test
    void http_crud_against_postgres_mode_h2() throws Exception {
        JdbcSettings jdbc =
                H2PostgresModeSupport.jdbcSettings(new H2PostgresModeSupport.DatabaseName(DB_NAME));
        AppConfig config =
                AppConfig.load(
                        AppConfig.LoadRequest.fromEnvironment(
                                key ->
                                        switch (key) {
                                            case "DSA_PERSISTENCE_MODE" -> "postgres";
                                            case "DSA_JDBC_DRIVER" -> jdbc.driver();
                                            case "DSA_JDBC_URL" -> jdbc.url();
                                            case "DSA_JDBC_USER" -> jdbc.user();
                                            case "DSA_JDBC_PASSWORD" -> jdbc.password();
                                            case "DSA_HTTP_PORT" -> String.valueOf(EPHEMERAL_PORT);
                                            default -> null;
                                        }));
        try (ApplicationComposition composition = ApplicationComposition.create(config);
                HttpServerRuntime runtime =
                        new JettyHttpServerBootstrap()
                                .start(
                                        HttpServerStartData.create(
                                                new HttpServerStartData(
                                                        HttpServerConfig.create(
                                                                new HttpServerConfig(EPHEMERAL_PORT)),
                                                        ApplicationRoutes.createDefaultRoutes(
                                                                composition))))) {
            HttpResponse<String> created = post(runtime.port(), "/strategies", CREATE_BODY);
            assertEquals(STATUS_CREATED, created.statusCode());
            String strategyId = extractField(created.body(), "strategyId");

            HttpResponse<String> listed =
                    get(runtime.port(), "/strategies", Map.of("ownerId", OWNER));
            assertEquals(STATUS_OK, listed.statusCode());
            assertTrue(listed.body().contains(strategyId));

            HttpResponse<String> detail =
                    get(
                            runtime.port(),
                            "/strategies/" + strategyId,
                            Map.of("ownerId", OWNER));
            assertEquals(STATUS_OK, detail.statusCode());

            HttpResponse<String> updated =
                    put(
                            runtime.port(),
                            "/strategies/" + strategyId,
                            Map.of("ownerId", OWNER),
                            UPDATE_BODY);
            assertEquals(STATUS_OK, updated.statusCode());
            assertTrue(updated.body().contains("\"versionNumber\":2"));

            HttpResponse<String> deleted =
                    delete(
                            runtime.port(),
                            "/strategies/" + strategyId,
                            Map.of("ownerId", OWNER));
            assertEquals(STATUS_OK, deleted.statusCode());

            HttpResponse<String> gone =
                    get(
                            runtime.port(),
                            "/strategies/" + strategyId,
                            Map.of("ownerId", OWNER));
            assertEquals(STATUS_NOT_FOUND, gone.statusCode());
        }
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
        StringBuilder builder = new StringBuilder("http://localhost:" + port + path + "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            first = false;
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static String extractField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
