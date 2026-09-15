package com.defistrategyarena.shared.infra.http.jetty;

import com.defistrategyarena.shared.infra.http.HttpServerBootstrap;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import java.util.concurrent.Executors;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public final class JettyHttpServerBootstrap implements HttpServerBootstrap {

    private static final int ACCEPTORS = 1;
    private static final int SELECTORS = 1;
    private static final String PLATFORM_POOL_NAME = "jetty-platform";
    private static final String START_FAILED = "failed to start jetty server";

    @Override
    public HttpServerRuntime start(HttpServerStartData startData) {
        try {
            return startServer(startData);
        } catch (Exception exception) {
            throw new IllegalStateException(START_FAILED, exception);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private static HttpServerRuntime startServer(HttpServerStartData startData) throws Exception {
        QueuedThreadPool platformPool = new QueuedThreadPool();
        platformPool.setName(PLATFORM_POOL_NAME);
        platformPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

        Server server = new Server(platformPool);
        ServerConnector connector = new ServerConnector(server, ACCEPTORS, SELECTORS);
        connector.setPort(startData.config().port());
        server.addConnector(connector);

        Handler handler = new JettyRouteDispatchHandler(startData.routes());
        server.setHandler(handler);
        server.start();

        return new JettyHttpServerRuntime(
                JettyServerHandle.create(server), connector.getLocalPort());
    }
}
