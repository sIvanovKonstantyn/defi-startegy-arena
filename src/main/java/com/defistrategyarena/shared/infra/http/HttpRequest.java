package com.defistrategyarena.shared.infra.http;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record HttpRequest(
        String method,
        String path,
        String body,
        Map<String, String> query,
        Map<String, String> pathVariables,
        Map<String, String> headers) {

    private static final String BODY_REQUIRED = "body must not be null";
    private static final String QUERY_REQUIRED = "query must not be null";
    private static final String PATH_VARIABLES_REQUIRED = "path variables must not be null";
    private static final String HEADERS_REQUIRED = "headers must not be null";
    private static final String AUTHORIZATION = "authorization";

    public HttpRequest(String method, String path, String body) {
        this(method, path, body, Map.of(), Map.of(), Map.of());
    }

    public HttpRequest(
            String method,
            String path,
            String body,
            Map<String, String> query,
            Map<String, String> pathVariables) {
        this(method, path, body, query, pathVariables, Map.of());
    }

    public HttpRequest {
        ValidatedHttpRouteKey routeKey = new ValidatedHttpRouteKey(method, path);
        method = routeKey.method();
        path = routeKey.path();
        if (body == null) {
            throw new IllegalArgumentException(BODY_REQUIRED);
        }
        if (query == null) {
            throw new IllegalArgumentException(QUERY_REQUIRED);
        }
        if (pathVariables == null) {
            throw new IllegalArgumentException(PATH_VARIABLES_REQUIRED);
        }
        if (headers == null) {
            throw new IllegalArgumentException(HEADERS_REQUIRED);
        }
        query = Map.copyOf(query);
        pathVariables = Map.copyOf(pathVariables);
        headers = normalizeHeaders(headers);
    }

    public static HttpRequest create(HttpRequest draft) {
        return new HttpRequest(
                draft.method(),
                draft.path(),
                draft.body(),
                draft.query(),
                draft.pathVariables(),
                draft.headers());
    }

    public Optional<String> authorizationHeader() {
        String value = headers.get(AUTHORIZATION);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static Map<String, String> normalizeHeaders(Map<String, String> raw) {
        java.util.LinkedHashMap<String, String> normalized = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return Map.copyOf(normalized);
    }
}
