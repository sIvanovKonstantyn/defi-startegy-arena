package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import org.junit.jupiter.api.Test;

class HttpRequestTest {

    @Test
    void rejects_null_body() {
        assertThrows(IllegalArgumentException.class, () -> new HttpRequest("GET", "/health", null));
    }

    @Test
    void rejects_blank_method() {
        assertThrows(IllegalArgumentException.class, () -> new HttpRequest(" ", "/health", ""));
    }

    @Test
    void rejects_null_method() {
        assertThrows(IllegalArgumentException.class, () -> new HttpRequest(null, "/health", ""));
    }

    @Test
    void rejects_blank_path() {
        assertThrows(IllegalArgumentException.class, () -> new HttpRequest("GET", " ", ""));
    }

    @Test
    void rejects_null_path() {
        assertThrows(IllegalArgumentException.class, () -> new HttpRequest("GET", null, ""));
    }

    @Test
    void rejects_null_query_map() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpRequest("GET", "/health", "", null, java.util.Map.of()));
    }

    @Test
    void rejects_null_path_variables_map() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpRequest("GET", "/health", "", java.util.Map.of(), null));
    }

    @Test
    void create_factory_copies_query_and_path_variables() {
        HttpRequest request =
                HttpRequest.create(
                        new HttpRequest(
                                "GET",
                                "/strategies/x",
                                "",
                                java.util.Map.of("ownerId", "o"),
                                java.util.Map.of("strategyId", "x")));
        assertEquals("o", request.query().get("ownerId"));
        assertEquals("x", request.pathVariables().get("strategyId"));
    }
}
