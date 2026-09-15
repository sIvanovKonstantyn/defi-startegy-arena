package com.defistrategyarena.shared.infra.http;

import java.util.Optional;

public interface HttpRouteRegistry {

    void register(HttpRouteRegistration registration);

    Optional<HttpRouteMatch> find(HttpRouteLookup lookup);
}
