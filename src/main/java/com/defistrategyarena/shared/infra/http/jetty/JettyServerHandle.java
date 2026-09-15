package com.defistrategyarena.shared.infra.http.jetty;

import org.eclipse.jetty.server.Server;

record JettyServerHandle(Server server, StopAction stopAction) implements JettyServerBinding {

    @FunctionalInterface
    interface StopAction {
        void run(Server server);
    }

    static JettyServerHandle create(Server server) {
        return new JettyServerHandle(server, JettyServerStopSupport::stop);
    }

    @Override
    public void join() throws InterruptedException {
        server.join();
    }

    @Override
    public void stop() {
        stopAction.run(server);
    }
}
