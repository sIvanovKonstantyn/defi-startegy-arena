package com.defistrategyarena.shared.infra.http.jetty;

interface JettyServerBinding {

    void join() throws InterruptedException;

    void stop();
}
