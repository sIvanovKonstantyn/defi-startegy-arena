package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyReadsHttpHandlerTest {

    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final String EMAIL = "reads-handler@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "ReadsHandler";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String STRATEGY_ID = "abc";

    @Test
    void list_without_auth_returns_unauthorized() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            ListStrategiesHttpHandler handler =
                    new ListStrategiesHttpHandler(
                            new ListStrategiesHttpHandler.ListStrategiesHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response = handler.handle(new HttpRequest("GET", "/strategies", ""));
            assertEquals(STATUS_UNAUTHORIZED, response.status());
        }
    }

    @Test
    void list_with_auth_returns_accepted_using_defaults() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            ListStrategiesHttpHandler handler =
                    new ListStrategiesHttpHandler(
                            new ListStrategiesHttpHandler.ListStrategiesHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "GET",
                                    "/strategies",
                                    "",
                                    Map.of("page", "", "size", "", "sort", "", "order", ""),
                                    Map.of(),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_ACCEPTED, response.status());
            assertTrue(response.body().contains("correlationId"));
        }
    }

    @Test
    void list_rejects_non_integer_page() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            ListStrategiesHttpHandler handler =
                    new ListStrategiesHttpHandler(
                            new ListStrategiesHttpHandler.ListStrategiesHttpHandlerDeps(
                                    composition.strategyPublisher()));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "GET",
                                    "/strategies",
                                    "",
                                    Map.of("page", "x", "size", "20"),
                                    Map.of(),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_BAD_REQUEST, response.status());
        }
    }

    @Test
    void get_without_auth_returns_unauthorized() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            PathIdStrategyHttpHandler handler =
                    new PathIdStrategyHttpHandler(
                            new PathIdStrategyHttpHandler.PathIdStrategyHttpHandlerDeps(
                                    composition.strategyPublisher(),
                                    PathIdStrategyHttpHandler.PathIdEventKind.GET));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "GET",
                                    "/strategies/" + STRATEGY_ID,
                                    "",
                                    Map.of(),
                                    Map.of("strategyId", STRATEGY_ID)));
            assertEquals(STATUS_UNAUTHORIZED, response.status());
        }
    }

    @Test
    void get_with_auth_returns_accepted() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            PathIdStrategyHttpHandler handler =
                    new PathIdStrategyHttpHandler(
                            new PathIdStrategyHttpHandler.PathIdStrategyHttpHandlerDeps(
                                    composition.strategyPublisher(),
                                    PathIdStrategyHttpHandler.PathIdEventKind.GET));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "GET",
                                    "/strategies/" + STRATEGY_ID,
                                    "",
                                    Map.of(),
                                    Map.of("strategyId", STRATEGY_ID),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_ACCEPTED, response.status());
            assertTrue(response.body().contains("correlationId"));
        }
    }

    @Test
    void get_with_missing_strategy_id_returns_bad_request() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token = signupToken(composition);
            PathIdStrategyHttpHandler handler =
                    new PathIdStrategyHttpHandler(
                            new PathIdStrategyHttpHandler.PathIdStrategyHttpHandlerDeps(
                                    composition.strategyPublisher(),
                                    PathIdStrategyHttpHandler.PathIdEventKind.GET));
            HttpResponse response =
                    handler.handle(
                            new HttpRequest(
                                    "GET",
                                    "/strategies/",
                                    "",
                                    Map.of(),
                                    Map.of(),
                                    Map.of(AUTHORIZATION, BEARER_PREFIX + token)));
            assertEquals(STATUS_BAD_REQUEST, response.status());
        }
    }

    private static String signupToken(ApplicationComposition composition) {
        return composition
                .identityHttp()
                .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME))
                .accessToken();
    }
}
