package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistration;
import org.junit.jupiter.api.Test;

class HttpRouteRegistrationTest {

    @Test
    void rejects_null_handler() {
        assertThrows(IllegalArgumentException.class, () -> new HttpRouteRegistration("GET", "/x", null));
    }

    @Test
    void rejects_blank_method() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpRouteRegistration(" ", "/x", echoHandler()));
    }

    private static HttpHandler echoHandler() {
        return HttpRouteRegistrationTest::echo;
    }

    private static HttpResponse echo(HttpRequest request) {
        return new HttpResponse(200, "text/plain", request.body());
    }
}
