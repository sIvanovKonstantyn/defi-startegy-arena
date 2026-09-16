package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.web.AccessTokenHttpRequest;
import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.identity.application.UnauthorizedException;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import java.util.Optional;

public enum BearerAccessToken {
    ;

    private static final String EMPTY = "";
    private static final String INVALID_AUTHORIZATION = "invalid authorization header";

    public static AccessTokenHttpRequest from(HttpRequest request) {
        Optional<String> header = request.authorizationHeader();
        if (header.isEmpty()) {
            return new AccessTokenHttpRequest(EMPTY);
        }
        try {
            AccessTokenQuery query =
                    AccessTokenQuery.fromAuthorizationHeader(
                            new AccessTokenQuery.AuthorizationHeader(header.get()));
            return new AccessTokenHttpRequest(query.accessToken());
        } catch (UnauthorizedException exception) {
            return new AccessTokenHttpRequest(EMPTY);
        }
    }

    public static AccessTokenHttpRequest requireValid(HttpRequest request) {
        AccessTokenHttpRequest token = from(request);
        if (token.accessToken().isBlank() && request.authorizationHeader().isPresent()) {
            throw new IllegalArgumentException(INVALID_AUTHORIZATION);
        }
        return token;
    }
}
