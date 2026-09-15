package com.defistrategyarena;

import com.defistrategyarena.bootstrap.ApplicationRoutes;
import com.defistrategyarena.bootstrap.ApplicationStartCommand;
import com.defistrategyarena.shared.infra.http.HttpServerBootstrap;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;

public final class DefiStrategyArenaApplication {

    private static final int DEFAULT_PORT = 8080;

    private DefiStrategyArenaApplication() {}

    public static void main(String[] args) throws InterruptedException {
        try (HttpServerRuntime runtime = start(selectBootstrap())) {
            runtime.join();
        }
    }

    static HttpServerBootstrap selectBootstrap() {
        return new JettyHttpServerBootstrap();
    }

    static HttpServerRuntime start(HttpServerBootstrap bootstrap) {
        return start(
                ApplicationStartCommand.create(
                        new ApplicationStartCommand(
                                bootstrap,
                                HttpServerConfig.create(new HttpServerConfig(DEFAULT_PORT)))));
    }

    static HttpServerRuntime start(ApplicationStartCommand command) {
        HttpServerStartData startData =
                HttpServerStartData.create(
                        new HttpServerStartData(command.config(), ApplicationRoutes.createDefaultRoutes()));
        return command.bootstrap().start(startData);
    }
}
