package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.JsonHttpResult;

public record AcceptedHttpResponse(int status, String correlationId) implements JsonHttpResult {

    private static final String EMPTY = "";

    public static AcceptedHttpResponse empty(StatusCode status) {
        return new AcceptedHttpResponse(status.value(), EMPTY);
    }

    public record StatusCode(int value) {}
}
