package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.shared.infra.http.HttpRouteLookup;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistration;
import com.defistrategyarena.shared.infra.http.InMemoryHttpRouteRegistry;
import org.junit.jupiter.api.Test;

class InMemoryHttpRouteRegistryTest {

    @Test
    void registers_and_finds_route_case_insensitively_for_method() {
        InMemoryHttpRouteRegistry registry = new InMemoryHttpRouteRegistry();
        registry.register(HttpRouteRegistration.create(new HttpRouteRegistration("get", "/health", okHandler())));

        HttpHandler handler =
                registry
                        .find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/health")))
                        .orElseThrow();
        HttpResponse response = handler.handle(new HttpRequest("GET", "/health", ""));

        assertEquals(200, response.status());
        assertTrue(registry.find(HttpRouteLookup.create(new HttpRouteLookup("POST", "/health"))).isEmpty());
    }

    private static HttpHandler okHandler() {
        return request -> new HttpResponse(200, "text/plain", "ok");
    }
}
