package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyUpdateDeleteHttpHandlerTest {

    private static final int STATUS_BAD_REQUEST = 400;

    @Test
    void update_handler_returns_bad_request_for_invalid_json() {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        UpdateStrategyHttpHandler handler =
                new UpdateStrategyHttpHandler(
                        new UpdateStrategyHttpHandler.UpdateStrategyHttpHandlerDeps(
                                composition.strategyHttp()));
        HttpResponse response =
                handler.handle(
                        new HttpRequest(
                                "PUT",
                                "/strategies/abc",
                                "{",
                                Map.of("ownerId", "owner"),
                                Map.of("strategyId", "abc")));
        assertEquals(STATUS_BAD_REQUEST, response.status());
    }

    @Test
    void delete_handler_returns_bad_request_for_blank_strategy_id() {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        DeleteStrategyHttpHandler handler =
                new DeleteStrategyHttpHandler(
                        new DeleteStrategyHttpHandler.DeleteStrategyHttpHandlerDeps(
                                composition.strategyHttp()));
        HttpResponse response =
                handler.handle(
                        new HttpRequest(
                                "DELETE",
                                "/strategies/ ",
                                "",
                                Map.of("ownerId", "owner"),
                                Map.of("strategyId", " ")));
        assertEquals(STATUS_BAD_REQUEST, response.status());
    }
}
