package com.defistrategyarena.shared.infra.http.jetty;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

class JettyHttpServerRuntimeTest {

    private static final int BIND_TIMEOUT_SECONDS = 5;
    private static final int EPHEMERAL_PORT = 0;

    @Test
    void join_returns_when_server_stops() throws Exception {
        Server server = new Server(EPHEMERAL_PORT);
        server.start();
        JettyHttpServerRuntime runtime =
                new JettyHttpServerRuntime(JettyServerHandle.create(server), server.getURI().getPort());

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<?> joined = executor.submit(joinTask(runtime));
        runtime.close();
        joined.get(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executor.close();
    }

    private static Callable<Void> joinTask(JettyHttpServerRuntime runtime) {
        return () -> {
            runtime.join();
            return null;
        };
    }

    @Test
    void close_wraps_stop_failures() {
        JettyHttpServerRuntime runtime = new JettyHttpServerRuntime(new FailingBinding(), EPHEMERAL_PORT);
        assertThrows(IllegalStateException.class, runtime::close);
        assertTrue(runtime.port() >= EPHEMERAL_PORT);
    }

    private static final class FailingBinding implements JettyServerBinding {

        @Override
        public void join() {}

        @Override
        public void stop() {
            throw new IllegalStateException("stop failed");
        }
    }
}
