package com.defistrategyarena.shared.infra.http;

import java.util.Map;

public record HttpRouteMatch(HttpHandler handler, Map<String, String> pathVariables) {

    private static final String HANDLER_REQUIRED = "handler must not be null";
    private static final String PATH_VARIABLES_REQUIRED = "path variables must not be null";

    public HttpRouteMatch {
        if (handler == null) {
            throw new IllegalArgumentException(HANDLER_REQUIRED);
        }
        if (pathVariables == null) {
            throw new IllegalArgumentException(PATH_VARIABLES_REQUIRED);
        }
        pathVariables = Map.copyOf(pathVariables);
    }

    public static HttpRouteMatch create(HttpRouteMatch draft) {
        return new HttpRouteMatch(draft.handler(), draft.pathVariables());
    }
}
