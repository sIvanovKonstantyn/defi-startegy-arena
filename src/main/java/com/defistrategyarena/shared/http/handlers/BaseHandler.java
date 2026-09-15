package com.defistrategyarena.shared.http.handlers;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseHandler implements HttpHandler {

    protected static final int STATUS_BAD_REQUEST = 400;
    protected static final String JSON_CONTENT_TYPE = "application/json";

    private static final String WRITE_FAILED = "failed to write json response";

    private final ObjectMapper objectMapper;

    protected BaseHandler() {
        this.objectMapper = new ObjectMapper();
    }

    protected BaseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public final HttpResponse handle(HttpRequest request) {
        try {
            return doHandle(request);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return badRequest();
        }
    }

    protected abstract HttpResponse doHandle(HttpRequest request) throws JsonProcessingException;

    protected abstract HttpResponse badRequest();

    protected final <T> T readJson(ReadJsonCommand<T> command) throws JsonProcessingException {
        return objectMapper.readValue(command.body(), command.type());
    }

    protected final String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(WRITE_FAILED, exception);
        }
    }

    protected final HttpResponse jsonResponse(JsonResponseCommand command) {
        return new HttpResponse(command.status(), JSON_CONTENT_TYPE, writeJson(command.body()));
    }

    public record ReadJsonCommand<T>(String body, Class<T> type) {}

    public record JsonResponseCommand(int status, Object body) {}
}
