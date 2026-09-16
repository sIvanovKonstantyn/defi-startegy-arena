package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import com.defistrategyarena.shared.infra.persistence.JdbcSettings;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.domain.StrategyId;
import com.defistrategyarena.strategy.integration.H2PostgresModeSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;

class StrategyPostgresHttpIT {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_ACCEPTED = 202;
    private static final int VERSION_TWO = 2;
    private static final int SINGLE = 1;
    private static final int EMPTY_STORE = 0;
    private static final String JSON_TYPE = "application/json";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EMAIL = "pg-http@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "PgHttp";
    private static final String DB_NAME = "strategy_http_it";
    private static final String CREATE_BODY =
            """
            {"name":"pg-crud","rules":[{"id":"r1","conditionType":"price_above","actionType":"hold","instrument":"ETH-USD","indicator":"","threshold":"3000","allocationPercent":""}]}
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
            String token = signupToken(composition);
            String ownerId = ownerId(composition, token);

            HttpResponse<String> created = post(runtime.port(), "/strategies", CREATE_BODY, token);
            assertEquals(STATUS_ACCEPTED, created.statusCode());
            assertTrue(created.body().contains("correlationId"));
            assertEquals(SINGLE, composition.strategies().size());

            String strategyId =
                    composition
                            .strategies()
                            .listByOwner(
                                    new ListStrategiesQuery(
                                            ownerId,
                                            ListStrategiesQuery.DEFAULT_PAGE,
                                            SINGLE,
                                            ListStrategiesQuery.SORT_NAME,
                                            ListStrategiesQuery.ORDER_ASC))
                            .items()
                            .getFirst()
                            .id()
                            .value();

            HttpResponse<String> listed = get(runtime.port(), "/strategies", token);
            assertEquals(STATUS_ACCEPTED, listed.statusCode());

            HttpResponse<String> detail =
                    get(runtime.port(), "/strategies/" + strategyId, token);
            assertEquals(STATUS_ACCEPTED, detail.statusCode());

            HttpResponse<String> updated =
                    put(runtime.port(), "/strategies/" + strategyId, UPDATE_BODY, token);
            assertEquals(STATUS_ACCEPTED, updated.statusCode());
            assertEquals(
                    VERSION_TWO,
                    composition
                            .strategies()
                            .get(new StrategyId(strategyId))
                            .orElseThrow()
                            .current()
                            .number());

            HttpResponse<String> deleted =
                    delete(runtime.port(), "/strategies/" + strategyId, token);
            assertEquals(STATUS_ACCEPTED, deleted.statusCode());
            assertEquals(EMPTY_STORE, composition.strategies().size());
        }
    }

    private static String signupToken(ApplicationComposition composition) {
        return composition
                .identityHttp()
                .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME))
                .accessToken();
    }

    private static String ownerId(ApplicationComposition composition, String token) {
        return composition
                .getCurrentUser()
                .execute(new AccessTokenQuery(token))
                .id()
                .value();
    }

    private static HttpResponse<String> post(int port, String path, String body, String token)
            throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", JSON_TYPE)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> put(int port, String path, String body, String token)
            throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", JSON_TYPE)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(int port, String path, String token) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .DELETE()
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(int port, String path, String token) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .GET()
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }
}
