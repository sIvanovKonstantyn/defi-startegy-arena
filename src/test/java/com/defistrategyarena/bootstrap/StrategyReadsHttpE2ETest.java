package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.domain.Strategy;
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
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int TWO_STRATEGIES = 2;
    private static final int PAGE_SIZE_ONE = 1;
    private static final String EMAIL = "reads-e2e@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "ReadsE2E";
    private static final String JSON_TYPE = "application/json";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String QUERY_START = "?";
    private static final String QUERY_SEP = "&";
    private static final String QUERY_EQ = "=";
    private static final String CREATE_BODY_A =
            "{\"name\":\"alpha\",\"rules\":[{\"id\":\"r1\",\"when\":{\"type\":\"price_compare\",\"instrument\":\"ETH-USD\",\"operator\":\"gt\",\"threshold\":\"3000\"},\"then\":{\"type\":\"hold\"}}]}";
    private static final String CREATE_BODY_B =
            "{\"name\":\"beta\",\"rules\":[{\"id\":\"r1\",\"when\":{\"type\":\"price_compare\",\"instrument\":\"ETH-USD\",\"operator\":\"gt\",\"threshold\":\"3000\"},\"then\":{\"type\":\"hold\"}}]}";

    @Test
    void lists_and_gets_over_jetty_with_bearer() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime = start(composition)) {
            String token = signupToken(composition);
            String ownerId = ownerId(composition, token);
            assertEquals(STATUS_ACCEPTED, post(runtime.port(), "/strategies", CREATE_BODY_A, token).statusCode());
            assertEquals(STATUS_ACCEPTED, post(runtime.port(), "/strategies", CREATE_BODY_B, token).statusCode());
            assertEquals(TWO_STRATEGIES, composition.strategies().size());

            Strategy first =
                    composition
                            .strategies()
                            .listByOwner(
                                    new ListStrategiesQuery(
                                            ownerId,
                                            ListStrategiesQuery.DEFAULT_PAGE,
                                            PAGE_SIZE_ONE,
                                            ListStrategiesQuery.SORT_NAME,
                                            ListStrategiesQuery.ORDER_ASC))
                            .items()
                            .getFirst();

            HttpResponse<String> list =
                    get(
                            runtime.port(),
                            "/strategies",
                            Map.of("page", "0", "size", "1", "sort", "name", "order", "asc"),
                            token);
            assertEquals(STATUS_ACCEPTED, list.statusCode());
            assertTrue(list.body().contains("correlationId"));
            assertFalse(list.body().contains("ownerId"));

            HttpResponse<String> detail =
                    get(runtime.port(), "/strategies/" + first.id().value(), Map.of(), token);
            assertEquals(STATUS_ACCEPTED, detail.statusCode());
            assertTrue(detail.body().contains("correlationId"));

            HttpResponse<String> unauthorized =
                    getWithoutAuth(runtime.port(), "/strategies", Map.of());
            assertEquals(STATUS_UNAUTHORIZED, unauthorized.statusCode());

            HttpResponse<String> badInt =
                    get(runtime.port(), "/strategies", Map.of("page", "x", "size", "20"), token);
            assertEquals(STATUS_BAD_REQUEST, badInt.statusCode());

            HttpResponse<String> defaults = get(runtime.port(), "/strategies", Map.of(), token);
            assertEquals(STATUS_ACCEPTED, defaults.statusCode());

            HttpResponse<String> flagOnly = sendGet(runtime.port(), "/strategies?debug", token);
            assertEquals(STATUS_ACCEPTED, flagOnly.statusCode());
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
                                        ApplicationRoutes.createDefaultRoutes(composition),
                                        java.util.Optional.of(composition.webSocketBinding()))));
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

    private static HttpResponse<String> get(
            int port, String path, Map<String, String> query, String token) throws Exception {
        return sendGetUri(uri(port, path, query), token);
    }

    private static HttpResponse<String> getWithoutAuth(
            int port, String path, Map<String, String> query) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri(port, path, query))).GET().build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> sendGet(int port, String pathAndQuery, String token)
            throws Exception {
        return sendGetUri("http://localhost:" + port + pathAndQuery, token);
    }

    private static HttpResponse<String> sendGetUri(String uri, String token) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(uri))
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .GET()
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static String uri(int port, String path, Map<String, String> query) {
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
        return uri.toString();
    }
}
