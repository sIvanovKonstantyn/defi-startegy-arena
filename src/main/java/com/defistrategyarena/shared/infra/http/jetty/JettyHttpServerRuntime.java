package com.defistrategyarena.shared.infra.http.jetty;

import com.defistrategyarena.shared.infra.http.HttpServerRuntime;

public final class JettyHttpServerRuntime implements HttpServerRuntime {

    private final JettyServerBinding binding;
    private final int boundPort;

    JettyHttpServerRuntime(JettyServerBinding binding, int boundPort) {
        this.binding = binding;
        this.boundPort = boundPort;
    }

    @Override
    public int port() {
        return boundPort;
    }

    @Override
    public void join() throws InterruptedException {
        binding.join();
    }

    @Override
    public void close() {
        binding.stop();
    }
}
