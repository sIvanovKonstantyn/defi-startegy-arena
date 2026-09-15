package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CreateStrategyHttpHandler
        extends BaseHandler<CreateStrategyHttpRequest, CreateStrategyHttpResponse> {

    private static final String EMPTY_STRATEGY_ID = "";

    private final StrategyRestAdapter strategyHttp;

    public CreateStrategyHttpHandler(CreateStrategyHttpHandlerDeps deps) {
        super(CreateStrategyHttpRequest.class);
        this.strategyHttp = deps.strategyHttp();
    }

    CreateStrategyHttpHandler(StrategyRestAdapter strategyHttp, ObjectMapper objectMapper) {
        super(CreateStrategyHttpRequest.class, objectMapper);
        this.strategyHttp = strategyHttp;
    }

    @Override
    protected CreateStrategyHttpResponse execute(CreateStrategyHttpRequest payload) {
        return strategyHttp.create(payload);
    }

    @Override
    protected CreateStrategyHttpResponse badRequestBody() {
        return new CreateStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY_STRATEGY_ID);
    }

    public record CreateStrategyHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
