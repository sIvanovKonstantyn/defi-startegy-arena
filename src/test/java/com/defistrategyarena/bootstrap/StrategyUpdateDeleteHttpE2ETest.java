package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;

class StrategyUpdateDeleteHttpE2ETest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int VERSION_TWO = 2;
    private static final int EMPTY_STORE = 0;
    private static final int SINGLE = 1;
    private static final String EMAIL = "update-e2e@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "UpdateE2E";
    private static final String JSON_TYPE = "application/json";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CREATE_BODY =
            "{\"name\":\"alpha\",\"rules\":[{\"id\":\"r1\",\"conditionType\":\"price_above\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"3000\",\"allocationPercent\":\"\"}]}";
    private static final String UPDATE_BODY =
            "{\"rules\":[{\"id\":\"r2\",\"conditionType\":\"price_under\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"2500\",\"allocationPercent\":\"\"}]}";
    private static final String EMPTY_RULES_BODY = "{\"rules\":[]}";

    @Test
    void updates_and_deletes_over_jetty_with_bearer() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime = start(composition)) {
            String token = signupToken(composition);
            String ownerId = ownerId(composition, token);

            assertEquals(STATUS_ACCEPTED, post(runtime.port(), "/strategies", CREATE_BODY, token).statusCode());
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

            HttpResponse<String> updated =
                    put(runtime.port(), "/strategies/" + strategyId, UPDATE_BODY, token);
            assertEquals(STATUS_ACCEPTED, updated.statusCode());
            assertTrue(updated.body().contains("correlationId"));
            assertEquals(
                    VERSION_TWO,
                    composition
                            .strategies()
                            .get(new StrategyId(strategyId))
                            .orElseThrow()
                            .current()
                            .number());

            HttpResponse<String> unauthorizedUpdate =
                    putWithoutAuth(runtime.port(), "/strategies/" + strategyId, UPDATE_BODY);
            assertEquals(STATUS_UNAUTHORIZED, unauthorizedUpdate.statusCode());

            HttpResponse<String> badRules =
                    put(runtime.port(), "/strategies/" + strategyId, EMPTY_RULES_BODY, token);
            assertEquals(STATUS_ACCEPTED, badRules.statusCode());
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
            assertTrue(composition.strategies().get(new StrategyId(strategyId)).isEmpty());
            assertEquals(EMPTY_STORE, composition.strategies().size());

            HttpResponse<String> unauthorizedDelete =
                    deleteWithoutAuth(runtime.port(), "/strategies/" + strategyId);
            assertEquals(STATUS_UNAUTHORIZED, unauthorizedDelete.statusCode());
        }
    }

    @Test
    void update_invalid_json_returns_bad_request() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime = start(composition)) {
            String token = signupToken(composition);
            HttpResponse<String> response =
                    put(runtime.port(), "/strategies/any", "{", token);
            assertEquals(STATUS_BAD_REQUEST, response.statusCode());
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

    private static HttpServerRuntime start(ApplicationComposition composition) {
        return new JettyHttpServerBootstrap()
                .start(
                        HttpServerStartData.create(
                                new HttpServerStartData(
                                        new HttpServerConfig(EPHEMERAL_PORT),
                                        ApplicationRoutes.createDefaultRoutes(composition))));
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

    private static HttpResponse<String> putWithoutAuth(int port, String path, String body)
            throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", JSON_TYPE)
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

    private static HttpResponse<String> deleteWithoutAuth(int port, String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).DELETE().build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }
}
