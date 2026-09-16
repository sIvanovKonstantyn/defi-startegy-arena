package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyUpdateDeleteHttpHandlerTest {

    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final String EMAIL = "update-handler@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "UpdateHandler";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String STRATEGY_ID = "abc";
    private static final String UPDATE_BODY =
            "{\"rules\":[{\"id\":\"r2\",\"conditionType\":\"price_under\",\"actionType\":\"hold\",\"instrument\":\"ETH-USD\",\"indicator\":\"\",\"threshold\":\"2500\",\"allocationPercent\":\"\"}]}";

    @Test
    void update_without_auth_returns_unauthorized() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            UpdateStrategyHttpHandler handler =
                    new UpdateStrategyHttpHandler(
                            new UpdateStrategyHttpHandler.UpdateStrategyHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "PUT",
                                    "/strategies/" + STRATEGY_ID,
                                    UPDATE_BODY,
                                    Map.of(),
                                    Map.of("strategyId", STRATEGY_ID)));
            assertEquals(STATUS_UNAUTHORIZED, response.status());
        }
    }

    @Test
    void update_with_auth_returns_accepted() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            UpdateStrategyHttpHandler handler =
                    new UpdateStrategyHttpHandler(
                            new UpdateStrategyHttpHandler.UpdateStrategyHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "PUT",
                                    "/strategies/" + STRATEGY_ID,
                                    UPDATE_BODY,
                                    Map.of(),
                                    Map.of("strategyId", STRATEGY_ID),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_ACCEPTED, response.status());
            assertTrue(response.body().contains("correlationId"));
        }
    }

    @Test
    void update_handler_returns_bad_request_for_invalid_json() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            UpdateStrategyHttpHandler handler =
                    new UpdateStrategyHttpHandler(
                            new UpdateStrategyHttpHandler.UpdateStrategyHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "PUT",
                                    "/strategies/" + STRATEGY_ID,
                                    "{",
                                    Map.of(),
                                    Map.of("strategyId", STRATEGY_ID),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_BAD_REQUEST, response.status());
        }
    }

    @Test
    void delete_without_auth_returns_unauthorized() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            PathIdStrategyHttpHandler handler =
                    new PathIdStrategyHttpHandler(
                            new PathIdStrategyHttpHandler.PathIdStrategyHttpHandlerDeps(
                                    composition.strategyPublisher(),
                                    PathIdStrategyHttpHandler.PathIdEventKind.DELETE));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "DELETE",
                                    "/strategies/" + STRATEGY_ID,
                                    "",
                                    Map.of(),
                                    Map.of("strategyId", STRATEGY_ID)));
            assertEquals(STATUS_UNAUTHORIZED, response.status());
        }
    }

    @Test
    void delete_with_auth_returns_accepted() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            PathIdStrategyHttpHandler handler =
                    new PathIdStrategyHttpHandler(
                            new PathIdStrategyHttpHandler.PathIdStrategyHttpHandlerDeps(
                                    composition.strategyPublisher(),
                                    PathIdStrategyHttpHandler.PathIdEventKind.DELETE));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "DELETE",
                                    "/strategies/" + STRATEGY_ID,
                                    "",
                                    Map.of(),
                                    Map.of("strategyId", STRATEGY_ID),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_ACCEPTED, response.status());
            assertTrue(response.body().contains("correlationId"));
        }
    }

    private static String signupToken(ApplicationComposition composition) {
        return composition
                .identityHttp()
                .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME))
                .accessToken();
    }
}
