package com.defistrategyarena;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.bootstrap.ApplicationStartCommand;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import org.junit.jupiter.api.Test;

class DefiStrategyArenaApplicationTest {

    private static final int EPHEMERAL_PORT = 0;

    @Test
    void starts_jetty_runtime_via_bootstrap_selection() throws Exception {
        HttpServerConfig config = HttpServerConfig.create(new HttpServerConfig(EPHEMERAL_PORT));
        ApplicationStartCommand command =
                ApplicationStartCommand.create(
                        new ApplicationStartCommand(new JettyHttpServerBootstrap(), config));
        try (HttpServerRuntime runtime = DefiStrategyArenaApplication.start(command)) {
            assertTrue(runtime.port() > 0);
        }
    }

    @Test
    void selects_jetty_bootstrap() {
        assertTrue(DefiStrategyArenaApplication.selectBootstrap() instanceof JettyHttpServerBootstrap);
    }
}
