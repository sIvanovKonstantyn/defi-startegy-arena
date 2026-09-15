package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpResponse;
import org.junit.jupiter.api.Test;

class HttpResponseTest {

    @Test
    void normalizes_null_body_to_empty() {
        HttpResponse response = HttpResponse.create(new HttpResponse(200, "text/plain", null));
        assertEquals("", response.body());
    }

    @Test
    void rejects_blank_content_type() {
        assertThrows(IllegalArgumentException.class, () -> new HttpResponse(200, " ", "ok"));
    }

    @Test
    void rejects_null_content_type() {
        assertThrows(IllegalArgumentException.class, () -> new HttpResponse(200, null, "ok"));
    }
}
