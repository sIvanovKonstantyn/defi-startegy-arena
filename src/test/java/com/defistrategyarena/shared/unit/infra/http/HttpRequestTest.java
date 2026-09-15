package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import org.junit.jupiter.api.Test;

class HttpRequestTest {

    @Test
    void normalizes_null_body_to_empty() {
        HttpRequest request = HttpRequest.create(new HttpRequest("GET", "/health", null));
        assertEquals("", request.body());
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
}
