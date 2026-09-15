package com.defistrategyarena.shared.infra.http;

public interface HttpServerBootstrap {

    HttpServerRuntime start(HttpServerStartData startData);
}
