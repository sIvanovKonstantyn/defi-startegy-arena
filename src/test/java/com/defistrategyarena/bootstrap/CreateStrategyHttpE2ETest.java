package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;

class CreateStrategyHttpE2ETest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int SINGLE = 1;
    private static final int EMPTY_STORE = 0;
    private static final String PATH = "/strategies";
    private static final String JSON_TYPE = "application/json";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EMAIL = "create-e2e@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "CreateE2E";
    private static final String NAME = "rsi-bounce";
    private static final String VALID_BODY =
            "{\"name\":\""
                    + NAME
                    + "\",\"rules\":[{\"id\":\"r1\",\"when\":{\"type\":\"price_compare\",\"instrument\":\"ETH-USD\",\"operator\":\"gt\",\"threshold\":\"3000\"},\"then\":{\"type\":\"hold\"}}]}";
    private static final String INVALID_BODY = "{not-json";

    @Test
    void creates_strategy_over_jetty_and_persists() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime = start(composition)) {
            String token = signupToken(composition);
            HttpResponse<String> response = post(runtime.port(), VALID_BODY, token);
            assertEquals(STATUS_ACCEPTED, response.statusCode());
            assertTrue(response.body().contains("correlationId"));
            assertFalse(response.body().contains("\"correlationId\":\"\""));
            assertFalse(response.body().contains("ownerId"));
            assertEquals(SINGLE, composition.strategies().size());
        }
    }

    @Test
    void duplicate_name_still_accepted_but_keeps_single_row() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime = start(composition)) {
            String token = signupToken(composition);
            assertEquals(STATUS_ACCEPTED, post(runtime.port(), VALID_BODY, token).statusCode());
            HttpResponse<String> second = post(runtime.port(), VALID_BODY, token);
            assertEquals(STATUS_ACCEPTED, second.statusCode());
            assertEquals(SINGLE, composition.strategies().size());
        }
    }

    @Test
    void rejects_missing_bearer() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime = start(composition)) {
            HttpResponse<String> response = postWithoutAuth(runtime.port(), VALID_BODY);
            assertEquals(STATUS_UNAUTHORIZED, response.statusCode());
            assertEquals(EMPTY_STORE, composition.strategies().size());
        }
    }

    @Test
    void rejects_invalid_json_over_jetty() throws Exception {
        try (ApplicationComposition composition = ApplicationComposition.createDefault();
                HttpServerRuntime runtime = start(composition)) {
            String token = signupToken(composition);
            HttpResponse<String> response = post(runtime.port(), INVALID_BODY, token);
            assertEquals(STATUS_BAD_REQUEST, response.statusCode());
            assertEquals(EMPTY_STORE, composition.strategies().size());
        }
    }

    private static String signupToken(ApplicationComposition composition) {
        return composition
                .identityHttp()
                .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME))
                .accessToken();
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

    private static HttpResponse<String> post(int port, String body, String token) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                        .header("Content-Type", JSON_TYPE)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> postWithoutAuth(int port, String body) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                        .header("Content-Type", JSON_TYPE)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
    }
}
