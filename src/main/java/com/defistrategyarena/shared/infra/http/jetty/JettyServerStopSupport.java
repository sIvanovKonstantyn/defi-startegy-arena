package com.defistrategyarena.shared.infra.http.jetty;

import org.eclipse.jetty.server.Server;

final class JettyServerStopSupport {

    private static final String STOP_FAILED = "failed to stop jetty server";

    private JettyServerStopSupport() {}

    static void stop(Server server) {
        run(server::stop);
    }

    static void run(StopAttempt attempt) {
        try {
            attempt.run();
        } catch (Exception exception) {
            throw new IllegalStateException(STOP_FAILED, exception);
        }
    }

    @FunctionalInterface
    interface StopAttempt {
        void run() throws Exception;
    }
}
