package com.defistrategyarena.shared.infra.http;

public interface HttpRouteRegistry {

    void register(HttpRouteRegistration registration);

    HttpHandler find(HttpRouteLookup lookup);
}
