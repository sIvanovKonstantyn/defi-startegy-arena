package com.defistrategyarena.identity.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;
import java.util.Objects;

public record SessionHttpResponse(int status, String accessToken) implements JsonHttpResult {

    private static final String DRAFT_REQUIRED = "session response must not be null";
    private static final String EMPTY = "";

    public static SessionHttpResponse create(SessionHttpResponse draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new SessionHttpResponse(draft.status(), draft.accessToken());
    }

    public static SessionHttpResponse empty(StatusCode status) {
        return new SessionHttpResponse(status.value(), EMPTY);
    }

    public record StatusCode(int value) {}
}
