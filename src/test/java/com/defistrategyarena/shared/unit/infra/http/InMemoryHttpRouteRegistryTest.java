package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.shared.infra.http.HttpRouteLookup;
import com.defistrategyarena.shared.infra.http.HttpRouteMatch;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistration;
import com.defistrategyarena.shared.infra.http.InMemoryHttpRouteRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryHttpRouteRegistryTest {

    @Test
    void registers_and_finds_route_case_insensitively_for_method() {
        InMemoryHttpRouteRegistry registry = new InMemoryHttpRouteRegistry();
        registry.register(HttpRouteRegistration.create(new HttpRouteRegistration("get", "/health", okHandler())));

        HttpHandler handler =
                registry
                        .find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/health")))
                        .orElseThrow()
                        .handler();
        HttpResponse response = handler.handle(new HttpRequest("GET", "/health", ""));

        assertEquals(200, response.status());
        assertTrue(registry.find(HttpRouteLookup.create(new HttpRouteLookup("POST", "/health"))).isEmpty());
    }

    @Test
    void matches_single_segment_path_template() {
        InMemoryHttpRouteRegistry registry = new InMemoryHttpRouteRegistry();
        registry.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration("GET", "/strategies/{strategyId}", okHandler())));

        var match =
                registry
                        .find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/strategies/abc")))
                        .orElseThrow();
        assertEquals("abc", match.pathVariables().get("strategyId"));
        assertEquals(200, match.handler().handle(new HttpRequest("GET", "/strategies/abc", "")).status());
    }

    @Test
    void template_method_mismatch_and_segment_mismatch_miss() {
        InMemoryHttpRouteRegistry registry = new InMemoryHttpRouteRegistry();
        registry.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration("GET", "/strategies/{strategyId}", okHandler())));
        assertTrue(registry.find(HttpRouteLookup.create(new HttpRouteLookup("POST", "/strategies/abc"))).isEmpty());
        assertTrue(registry.find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/other/abc"))).isEmpty());
        assertTrue(registry.find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/strategies/abc/extra"))).isEmpty());
    }

    @Test
    void malformed_template_segment_does_not_match() {
        InMemoryHttpRouteRegistry registry = new InMemoryHttpRouteRegistry();
        registry.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration("GET", "/strategies/{", okHandler())));
        assertTrue(registry.find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/strategies/abc"))).isEmpty());
        registry.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration("GET", "/items/{open", okHandler())));
        assertTrue(registry.find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/items/abc"))).isEmpty());
    }

    @Test
    void route_match_factory_and_null_guards() {
        HttpHandler handler = okHandler();
        HttpRouteMatch match = HttpRouteMatch.create(new HttpRouteMatch(handler, Map.of("id", "1")));
        assertEquals("1", match.pathVariables().get("id"));
        assertThrows(IllegalArgumentException.class, () -> new HttpRouteMatch(null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new HttpRouteMatch(handler, null));
    }

    private static HttpHandler okHandler() {
        return request -> new HttpResponse(200, "text/plain", "ok");
    }
}
