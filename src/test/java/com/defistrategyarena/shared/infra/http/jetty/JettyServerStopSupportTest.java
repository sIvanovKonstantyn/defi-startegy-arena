package com.defistrategyarena.shared.infra.http.jetty;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JettyServerStopSupportTest {

    @Test
    void run_wraps_checked_exceptions() {
        assertThrows(
                IllegalStateException.class,
                () -> JettyServerStopSupport.run(() -> {
                    throw new Exception("stop failed");
                }));
    }
}
