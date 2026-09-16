package com.defistrategyarena.identity.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;
import java.util.Objects;

public record LogoutHttpResponse(int status) implements JsonHttpResult {

    private static final String DRAFT_REQUIRED = "logout response must not be null";

    public static LogoutHttpResponse create(LogoutHttpResponse draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new LogoutHttpResponse(draft.status());
    }
}
