package com.defistrategyarena.shared.unit.infra.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import org.junit.jupiter.api.Test;

class HttpServerConfigTest {

    @Test
    void allows_ephemeral_port_zero() {
        HttpServerConfig config = HttpServerConfig.create(new HttpServerConfig(0));
        assertEquals(0, config.port());
    }

    @Test
    void rejects_negative_port() {
        assertThrows(IllegalArgumentException.class, () -> new HttpServerConfig(-1));
    }
}
