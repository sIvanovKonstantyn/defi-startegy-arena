package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

public final class CreateStrategyHttpHandler implements HttpHandler {

    private static final int STATUS_BAD_REQUEST = 400;
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String EMPTY_STRATEGY_ID = "";
    private static final String ADAPTER_REQUIRED = "strategy rest adapter must not be null";
    private static final String MAPPER_REQUIRED = "object mapper must not be null";
    private static final String WRITE_FAILED = "failed to write create strategy response";

    private final StrategyRestAdapter strategyHttp;
    private final ObjectMapper objectMapper;

    public CreateStrategyHttpHandler(CreateStrategyHttpHandlerDeps deps) {
        this(deps.strategyHttp(), new ObjectMapper());
    }

    CreateStrategyHttpHandler(StrategyRestAdapter strategyHttp, ObjectMapper objectMapper) {
        this.strategyHttp = Objects.requireNonNull(strategyHttp, ADAPTER_REQUIRED);
        this.objectMapper = Objects.requireNonNull(objectMapper, MAPPER_REQUIRED);
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        try {
            CreateStrategyHttpRequest payload =
                    objectMapper.readValue(request.body(), CreateStrategyHttpRequest.class);
            CreateStrategyHttpResponse result = strategyHttp.create(payload);
            return new HttpResponse(result.status(), JSON_CONTENT_TYPE, writeJson(result));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return new HttpResponse(
                    STATUS_BAD_REQUEST,
                    JSON_CONTENT_TYPE,
                    writeJson(new CreateStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY_STRATEGY_ID)));
        }
    }

    private String writeJson(CreateStrategyHttpResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(WRITE_FAILED, exception);
        }
    }

    public record CreateStrategyHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
