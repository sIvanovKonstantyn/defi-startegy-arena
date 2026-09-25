package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateStrategyHttpHandlerTest {

    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int SINGLE_STRATEGY = 1;
    private static final int EMPTY_STORE = 0;
    private static final String EMAIL = "create-handler@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "CreateHandler";
    private static final String VALID_BODY =
            "{\"name\":\"n\",\"rules\":[{\"id\":\"r1\",\"when\":{\"type\":\"price_compare\",\"instrument\":\"ETH-USD\",\"operator\":\"gt\",\"threshold\":\"3000\"},\"then\":{\"type\":\"hold\"}}]}";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Test
    void authenticated_create_returns_accepted_and_persists() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            CreateStrategyHttpHandler handler =
                    new CreateStrategyHttpHandler(
                            new CreateStrategyHttpHandler.CreateStrategyHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "POST",
                                    "/strategies",
                                    VALID_BODY,
                                    Map.of(),
                                    Map.of(),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_ACCEPTED, response.status());
            assertTrue(response.body().contains("correlationId"));
            assertFalse(response.body().contains("\"correlationId\":\"\""));
            assertEquals(SINGLE_STRATEGY, composition.strategies().size());
        }
    }

    @Test
    void missing_bearer_returns_unauthorized() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            CreateStrategyHttpHandler handler =
                    new CreateStrategyHttpHandler(
                            new CreateStrategyHttpHandler.CreateStrategyHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response = handler.handle(new HttpRequest("POST", "/strategies", VALID_BODY));
            assertEquals(STATUS_UNAUTHORIZED, response.status());
            assertEquals(EMPTY_STORE, composition.strategies().size());
        }
    }

    @Test
    void invalid_json_returns_bad_request() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            CreateStrategyHttpHandler handler =
                    new CreateStrategyHttpHandler(
                            new CreateStrategyHttpHandler.CreateStrategyHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response = handler.handle(new HttpRequest("POST", "/strategies", "{"));
            assertEquals(STATUS_BAD_REQUEST, response.status());
        }
    }

    @Test
    void write_failure_is_wrapped_as_illegal_state() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            ObjectMapper failingMapper =
                    new ObjectMapper() {
                        @Override
                        public String writeValueAsString(Object value) throws JsonProcessingException {
                            throw new JsonProcessingException("boom") {};
                        }
                    };
            CreateStrategyHttpHandler handler =
                    new CreateStrategyHttpHandler(composition.strategyPublisher(), failingMapper);
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            handler.handle(
                                    new HttpRequest(
                                            "POST",
                                            "/strategies",
                                            VALID_BODY,
                                            Map.of(),
                                            Map.of(),
                                            Map.of(AUTHORIZATION, BEARER_PREFIX + token))));
        }
    }

    private static String signupToken(ApplicationComposition composition) {
        return composition
                .identityHttp()
                .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME))
                .accessToken();
    }
}
