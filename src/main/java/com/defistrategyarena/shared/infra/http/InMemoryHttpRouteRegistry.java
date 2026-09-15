package com.defistrategyarena.shared.infra.http;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class InMemoryHttpRouteRegistry implements HttpRouteRegistry {

    private final Map<RouteKey, HttpHandler> routes = new HashMap<>();

    @Override
    public void register(HttpRouteRegistration registration) {
        RouteKey key = RouteKey.create(new RouteKeyData(registration.method(), registration.path()));
        routes.put(key, registration.handler());
    }

    @Override
    public Optional<HttpHandler> find(HttpRouteLookup lookup) {
        RouteKey key = RouteKey.create(new RouteKeyData(lookup.method(), lookup.path()));
        return Optional.ofNullable(routes.get(key));
    }

    private record RouteKey(String method, String path) {
        private static RouteKey create(RouteKeyData data) {
            return new RouteKey(data.method().toUpperCase(Locale.ROOT), data.path());
        }
    }

    private record RouteKeyData(String method, String path) {}
}
