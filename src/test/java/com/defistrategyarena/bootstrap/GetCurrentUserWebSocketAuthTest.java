package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.infra.http.WebSocketAccessTokenAuth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetCurrentUserWebSocketAuthTest {

    private static final String EMAIL = "ws-auth@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY = "WsAuth";
    private static final String BAD = "not-a-token";

    @Test
    void authenticates_valid_token_and_rejects_invalid() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            String token =
                    composition
                            .identityHttp()
                            .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY))
                            .accessToken();
            WebSocketAccessTokenAuth auth = composition.webSocketBinding().auth();
            Optional<WebSocketAccessTokenAuth.WebSocketPrincipal> ok =
                    auth.authenticate(new WebSocketAccessTokenAuth.AccessTokenCommand(token));
            assertTrue(ok.isPresent());
            assertEquals(
                    composition.getCurrentUser()
                            .execute(
                                    new com.defistrategyarena.identity.application.AccessTokenQuery(
                                            token))
                            .id()
                            .value(),
                    ok.get().userId());
            assertTrue(
                    auth.authenticate(new WebSocketAccessTokenAuth.AccessTokenCommand(BAD))
                            .isEmpty());
        }
    }
}
