package com.defistrategyarena;

import com.defistrategyarena.bootstrap.AppConfig;
import com.defistrategyarena.bootstrap.ApplicationComposition;
import com.defistrategyarena.bootstrap.ApplicationRoutes;
import com.defistrategyarena.bootstrap.ApplicationStartCommand;
import com.defistrategyarena.shared.infra.http.HttpServerBootstrap;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;

public final class DefiStrategyArenaApplication {

    private DefiStrategyArenaApplication() {}

    public static void main(String[] args) throws InterruptedException {
        AppConfig config = AppConfig.load();
        try (ApplicationComposition composition = ApplicationComposition.create(config);
                HttpServerRuntime runtime =
                        start(new RuntimeStartRequest(selectBootstrap(), config, composition))) {
            runtime.join();
        }
    }

    static HttpServerBootstrap selectBootstrap() {
        return new JettyHttpServerBootstrap();
    }

    static HttpServerRuntime start(HttpServerBootstrap bootstrap) {
        return start(
                new RuntimeStartRequest(
                        bootstrap, AppConfig.load(), ApplicationComposition.createDefault()));
    }

    static HttpServerRuntime start(ApplicationStartCommand command) {
        return start(new CommandStartRequest(command, ApplicationComposition.createDefault()));
    }

    static HttpServerRuntime start(RuntimeStartRequest request) {
        return start(
                new CommandStartRequest(
                        ApplicationStartCommand.create(
                                new ApplicationStartCommand(
                                        request.bootstrap(),
                                        HttpServerConfig.create(
                                                new HttpServerConfig(request.config().httpPort())))),
                        request.composition()));
    }

    static HttpServerRuntime start(CommandStartRequest request) {
        HttpServerStartData startData =
                HttpServerStartData.create(
                        new HttpServerStartData(
                                request.command().config(),
                                ApplicationRoutes.createDefaultRoutes(request.composition()),
                                java.util.Optional.of(request.composition().webSocketBinding())));
        return request.command().bootstrap().start(startData);
    }

    record RuntimeStartRequest(
            HttpServerBootstrap bootstrap, AppConfig config, ApplicationComposition composition) {}

    record CommandStartRequest(
            ApplicationStartCommand command, ApplicationComposition composition) {}
}
