package com.defistrategyarena.identity.application;

public record AuthSessionResult(String accessToken) {

    public static AuthSessionResult create(AuthSessionResult draft) {
        return new AuthSessionResult(draft.accessToken());
    }
}
