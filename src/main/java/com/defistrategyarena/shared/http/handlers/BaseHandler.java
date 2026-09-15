package com.defistrategyarena.shared.http.handlers;

import com.defistrategyarena.shared.http.JsonHttpResult;
import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseHandler<Req, Res extends JsonHttpResult> implements HttpHandler {

    protected static final int STATUS_BAD_REQUEST = 400;
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String WRITE_FAILED = "failed to write json response";

    private final Class<Req> requestType;
    private final ObjectMapper objectMapper;

    protected BaseHandler(Class<Req> requestType) {
        this(requestType, new ObjectMapper());
    }

    protected BaseHandler(Class<Req> requestType, ObjectMapper objectMapper) {
        this.requestType = requestType;
        this.objectMapper = objectMapper;
    }

    @Override
    public final HttpResponse handle(HttpRequest request) {
        try {
            Req payload = objectMapper.readValue(request.body(), requestType);
            return toHttpResponse(execute(payload));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return toHttpResponse(badRequestBody());
        }
    }

    protected abstract Res execute(Req payload);

    protected abstract Res badRequestBody();

    private HttpResponse toHttpResponse(Res result) {
        return new HttpResponse(result.status(), JSON_CONTENT_TYPE, writeJson(result));
    }

    private String writeJson(Res result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(WRITE_FAILED, exception);
        }
    }
}
