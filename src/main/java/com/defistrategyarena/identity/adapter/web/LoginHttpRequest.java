package com.defistrategyarena.identity.adapter.web;

import java.util.Objects;

public record LoginHttpRequest(String email, String password) {

    private static final String DRAFT_REQUIRED = "login request must not be null";

    public static LoginHttpRequest create(LoginHttpRequest draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new LoginHttpRequest(draft.email(), draft.password());
    }
}
