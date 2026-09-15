package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyReadsHttpHandlerTest {

    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_OK = 200;

    @Test
    void list_handler_returns_bad_request_for_blank_owner() {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        ListStrategiesHttpHandler handler =
                new ListStrategiesHttpHandler(
                        new ListStrategiesHttpHandler.ListStrategiesHttpHandlerDeps(
                                composition.strategyHttp()));
        HttpResponse response =
                handler.handle(
                        new HttpRequest(
                                "GET",
                                "/strategies",
                                "",
                                Map.of("ownerId", " "),
                                Map.of()));
        assertEquals(STATUS_BAD_REQUEST, response.status());
    }

    @Test
    void list_handler_uses_defaults_when_optional_query_missing() {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        ListStrategiesHttpHandler handler =
                new ListStrategiesHttpHandler(
                        new ListStrategiesHttpHandler.ListStrategiesHttpHandlerDeps(
                                composition.strategyHttp()));
        HttpResponse response =
                handler.handle(
                        new HttpRequest(
                                "GET",
                                "/strategies",
                                "",
                                Map.of("ownerId", "owner-handler", "page", "", "size", "", "sort", "", "order", ""),
                                Map.of()));
        assertEquals(STATUS_OK, response.status());
    }

    @Test
    void get_handler_returns_bad_request_for_blank_owner() {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        GetStrategyHttpHandler handler =
                new GetStrategyHttpHandler(
                        new GetStrategyHttpHandler.GetStrategyHttpHandlerDeps(composition.strategyHttp()));
        HttpResponse response =
                handler.handle(
                        new HttpRequest(
                                "GET",
                                "/strategies/abc",
                                "",
                                Map.of(),
                                Map.of("strategyId", "abc")));
        assertEquals(STATUS_BAD_REQUEST, response.status());
    }
}
