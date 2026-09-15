package com.defistrategyarena.shared.infra.http;

record ValidatedHttpRouteKey(String method, String path) {

    private static final String METHOD_REQUIRED = "method must not be blank";
    private static final String PATH_REQUIRED = "path must not be blank";

    ValidatedHttpRouteKey {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException(METHOD_REQUIRED);
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(PATH_REQUIRED);
        }
    }
}
