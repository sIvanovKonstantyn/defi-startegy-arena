package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CreateStrategyHttpHandlerTest {

    private static final int STATUS_BAD_REQUEST = 400;

    @Test
    void invalid_json_returns_bad_request() {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        CreateStrategyHttpHandler handler =
                new CreateStrategyHttpHandler(
                        new CreateStrategyHttpHandler.CreateStrategyHttpHandlerDeps(
                                composition.strategyHttp()));
        HttpResponse response = handler.handle(new HttpRequest("POST", "/strategies", "{"));
        assertEquals(STATUS_BAD_REQUEST, response.status());
    }

    @Test
    void write_failure_is_wrapped_as_illegal_state() {
        ApplicationComposition composition = ApplicationComposition.createDefault();
        ObjectMapper failingMapper =
                new ObjectMapper() {
                    @Override
                    public String writeValueAsString(Object value) throws JsonProcessingException {
                        throw new JsonProcessingException("boom") {};
                    }
                };
        CreateStrategyHttpHandler handler =
                new CreateStrategyHttpHandler(composition.strategyHttp(), failingMapper);
        assertThrows(
                IllegalStateException.class,
                () ->
                        handler.handle(
                                new HttpRequest(
                                        "POST",
                                        "/strategies",
                                        "{\"ownerId\":\"o\",\"name\":\"n\",\"rules\":[]}")));
    }
}
