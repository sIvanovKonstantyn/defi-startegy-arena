package com.defistrategyarena.identity.adapter.web;

import java.util.Objects;

public record AccessTokenHttpRequest(String accessToken) {

    private static final String DRAFT_REQUIRED = "access token request must not be null";

    public static AccessTokenHttpRequest create(AccessTokenHttpRequest draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new AccessTokenHttpRequest(draft.accessToken());
    }
}
