package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.InMemoryHttpRouteRegistry;
import org.junit.jupiter.api.Test;

class HttpServerStartDataTest {

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
                () -> new HttpServerStartData(new HttpServerConfig(8080), null));
    }
}
