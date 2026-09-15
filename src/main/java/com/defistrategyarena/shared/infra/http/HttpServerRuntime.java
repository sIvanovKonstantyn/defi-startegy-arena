package com.defistrategyarena.shared.infra.http;

public interface HttpServerRuntime extends AutoCloseable {

    int port();

    void join() throws InterruptedException;

    @Override
    void close();
}
