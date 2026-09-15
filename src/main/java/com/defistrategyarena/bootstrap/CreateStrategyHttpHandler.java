package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CreateStrategyHttpHandler extends BaseHandler<CreateStrategyHttpResponse> {

    private static final String EMPTY_STRATEGY_ID = "";

    private final StrategyRestAdapter strategyHttp;

    public CreateStrategyHttpHandler(CreateStrategyHttpHandlerDeps deps) {
        this.strategyHttp = deps.strategyHttp();
    }

    CreateStrategyHttpHandler(StrategyRestAdapter strategyHttp, ObjectMapper objectMapper) {
        super(objectMapper);
        this.strategyHttp = strategyHttp;
    }

    @Override
    protected CreateStrategyHttpResponse execute(HttpRequest request) throws JsonProcessingException {
        CreateStrategyHttpRequest payload =
                readJson(new ReadJsonCommand<>(request.body(), CreateStrategyHttpRequest.class));
        return strategyHttp.create(payload);
    }

    @Override
    protected CreateStrategyHttpResponse badRequestBody() {
        return new CreateStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY_STRATEGY_ID);
    }

    public record CreateStrategyHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
