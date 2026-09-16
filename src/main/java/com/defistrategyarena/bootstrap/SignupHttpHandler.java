package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.adapter.web.SessionHttpResponse;
import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SignupHttpHandler extends BaseHandler<SessionHttpResponse> {

    private final IdentityRestAdapter identityHttp;

    public SignupHttpHandler(SignupHttpHandlerDeps deps) {
        this.identityHttp = deps.identityHttp();
    }

    SignupHttpHandler(IdentityRestAdapter identityHttp, ObjectMapper objectMapper) {
        super(objectMapper);
        this.identityHttp = identityHttp;
    }

    @Override
    protected SessionHttpResponse execute(HttpRequest request) throws JsonProcessingException {
        SignupHttpRequest payload =
                readJson(new ReadJsonCommand<>(request.body(), SignupHttpRequest.class));
        return identityHttp.signup(payload);
    }

    @Override
    protected SessionHttpResponse badRequestBody() {
        return SessionHttpResponse.empty(
                new SessionHttpResponse.StatusCode(STATUS_BAD_REQUEST));
    }

    public record SignupHttpHandlerDeps(IdentityRestAdapter identityHttp) {}
}
