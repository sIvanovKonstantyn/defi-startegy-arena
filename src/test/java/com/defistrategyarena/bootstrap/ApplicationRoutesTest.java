package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpRouteLookup;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistry;
import org.junit.jupiter.api.Test;

class ApplicationRoutesTest {

    @Test
    void registers_health_route() {
        HttpRouteRegistry routes = ApplicationRoutes.createDefaultRoutes();
        HttpHandler handler =
                routes.find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/health"))).orElseThrow();
        assertEquals(200, handler.handle(new HttpRequest("GET", "/health", "")).status());
    }

    @Test
    void enum_catalog_helpers_are_reachable() {
        assertEquals(0, ApplicationRoutes.values().length);
    }
}
