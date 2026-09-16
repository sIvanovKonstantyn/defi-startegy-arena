package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.adapter.web.UserHttpResponse;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;

public final class MeHttpHandler extends BaseHandler<UserHttpResponse> {

    private static final int STATUS_UNAUTHORIZED = 401;

    private final IdentityRestAdapter identityHttp;

    public MeHttpHandler(MeHttpHandlerDeps deps) {
        this.identityHttp = deps.identityHttp();
    }

    @Override
    protected UserHttpResponse execute(HttpRequest request) {
        return identityHttp.me(BearerAccessToken.requireValid(request));
    }

    @Override
    protected UserHttpResponse badRequestBody() {
        return UserHttpResponse.empty(new UserHttpResponse.StatusCode(STATUS_UNAUTHORIZED));
    }

    public record MeHttpHandlerDeps(IdentityRestAdapter identityHttp) {}
}
