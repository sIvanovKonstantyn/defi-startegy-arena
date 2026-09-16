package com.defistrategyarena.identity.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;
import java.util.Objects;

public record UserHttpResponse(int status, String displayName) implements JsonHttpResult {

    private static final String DRAFT_REQUIRED = "user response must not be null";
    private static final String EMPTY = "";

    public static UserHttpResponse create(UserHttpResponse draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new UserHttpResponse(draft.status(), draft.displayName());
    }

    public static UserHttpResponse empty(StatusCode status) {
        return new UserHttpResponse(status.value(), EMPTY);
    }

    public record StatusCode(int value) {}
}
