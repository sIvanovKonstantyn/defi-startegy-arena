package com.defistrategyarena.identity.adapter.web;

import java.util.Objects;

public record SignupHttpRequest(String email, String password, String displayName) {

    private static final String DRAFT_REQUIRED = "signup request must not be null";

    public static SignupHttpRequest create(SignupHttpRequest draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new SignupHttpRequest(draft.email(), draft.password(), draft.displayName());
    }
}
