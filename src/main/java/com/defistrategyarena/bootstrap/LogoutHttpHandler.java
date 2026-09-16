package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.adapter.web.LogoutHttpResponse;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;

public final class LogoutHttpHandler extends BaseHandler<LogoutHttpResponse> {

    private static final int STATUS_UNAUTHORIZED = 401;

    private final IdentityRestAdapter identityHttp;

    public LogoutHttpHandler(LogoutHttpHandlerDeps deps) {
        this.identityHttp = deps.identityHttp();
    }

    @Override
    protected LogoutHttpResponse execute(HttpRequest request) {
        return identityHttp.logout(BearerAccessToken.requireValid(request));
    }

    @Override
    protected LogoutHttpResponse badRequestBody() {
        return new LogoutHttpResponse(STATUS_UNAUTHORIZED);
    }

    public record LogoutHttpHandlerDeps(IdentityRestAdapter identityHttp) {}
}
