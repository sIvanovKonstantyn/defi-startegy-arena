package com.defistrategyarena.shared.infra.http.jetty;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

class JettyServerHandleTest {

    @Test
    void stop_wraps_server_failures() {
        Server server = new Server(0);
        JettyServerHandle handle =
                new JettyServerHandle(server, unused -> {
                    throw new IllegalStateException("stop failed");
                });

        assertThrows(IllegalStateException.class, handle::stop);
    }
}
