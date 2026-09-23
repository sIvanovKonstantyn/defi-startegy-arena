package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.InMemoryHttpRouteRegistry;
import org.junit.jupiter.api.Test;

class HttpServerStartDataTest {

    private static final int PORT = 8080;

    @Test
    void rejects_null_config() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpServerStartData(null, new InMemoryHttpRouteRegistry()));
    }

    @Test
    void rejects_null_routes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpServerStartData(new HttpServerConfig(PORT), null));
    }

    @Test
    void rejects_null_websocket_optional() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new HttpServerStartData(
                                        new HttpServerConfig(PORT),
                                        new InMemoryHttpRouteRegistry(),
                                        null));
        assertTrue(thrown.getMessage().contains("websocket"));
    }

    @Test
    void create_preserves_empty_websocket() {
        HttpServerStartData created =
                HttpServerStartData.create(
                        new HttpServerStartData(
                                new HttpServerConfig(PORT), new InMemoryHttpRouteRegistry()));
        assertTrue(created.webSocket().isEmpty());
    }
}
