package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.adapter.web.LoginHttpRequest;
import com.defistrategyarena.identity.adapter.web.SessionHttpResponse;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class LoginHttpHandler extends BaseHandler<SessionHttpResponse> {

    private final IdentityRestAdapter identityHttp;

    public LoginHttpHandler(LoginHttpHandlerDeps deps) {
        this.identityHttp = deps.identityHttp();
    }

    LoginHttpHandler(IdentityRestAdapter identityHttp, ObjectMapper objectMapper) {
        super(objectMapper);
        this.identityHttp = identityHttp;
    }

    @Override
    protected SessionHttpResponse execute(HttpRequest request) throws JsonProcessingException {
        LoginHttpRequest payload =
                readJson(new ReadJsonCommand<>(request.body(), LoginHttpRequest.class));
        return identityHttp.login(payload);
    }

    @Override
    protected SessionHttpResponse badRequestBody() {
        return SessionHttpResponse.empty(
                new SessionHttpResponse.StatusCode(STATUS_BAD_REQUEST));
    }

    public record LoginHttpHandlerDeps(IdentityRestAdapter identityHttp) {}
}
