package com.defistrategyarena.shared.infra.http.jetty;

import com.defistrategyarena.shared.infra.http.HttpServerBootstrap;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.WebSocketBinding;
import java.util.concurrent.Executors;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;
import org.eclipse.jetty.websocket.server.WebSocketCreator;

public final class JettyHttpServerBootstrap implements HttpServerBootstrap {

    private static final int ACCEPTORS = 1;
    private static final int SELECTORS = 1;
    private static final String PLATFORM_POOL_NAME = "jetty-platform";
    private static final String START_FAILED = "failed to start jetty server";
    private static final String CONTEXT_PATH = "/";
    private static final String WS_PATH = "/ws";

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

        Handler httpHandler = new JettyRouteDispatchHandler(startData.routes());
        if (startData.webSocket().isEmpty()) {
            server.setHandler(httpHandler);
        } else {
            server.setHandler(
                    withWebSocket(new WebSocketHandlerInput(server, startData.webSocket().orElseThrow(), httpHandler)));
        }
        server.start();

        return new JettyHttpServerRuntime(
                JettyServerHandle.create(server), connector.getLocalPort());
    }

    private static Handler withWebSocket(WebSocketHandlerInput input) {
        ContextHandler context = new ContextHandler(CONTEXT_PATH);
        ServerWebSocketContainer container = ServerWebSocketContainer.ensure(input.server(), context);
        WebSocketCreator creator =
                (request, response, callback) -> new JettyUserSessionWebSocket(input.binding());
        container.addMapping(WS_PATH, creator);
        return new WebSocketThenHttpHandler(
                new WebSocketThenHttpHandler.WebSocketThenHttpHandlerDeps(
                        container, input.httpHandler()));
    }

    private record WebSocketHandlerInput(
            Server server, WebSocketBinding binding, Handler httpHandler) {}

    private static final class WebSocketThenHttpHandler extends Handler.Abstract {

        private final ServerWebSocketContainer webSockets;
        private final Handler httpHandler;

        private WebSocketThenHttpHandler(WebSocketThenHttpHandlerDeps deps) {
            this.webSockets = deps.webSockets();
            this.httpHandler = deps.httpHandler();
        }

        @Override
        @SuppressWarnings("PMD.MethodsTakeAtMostOneDtoParameter")
        public boolean handle(Request request, Response response, Callback callback)
                throws Exception {
            boolean websocketHandled = webSockets.handle(request, response, callback);
            if (websocketHandled) {
                return websocketHandled;
            }
            return httpHandler.handle(request, response, callback);
        }

        private record WebSocketThenHttpHandlerDeps(
                ServerWebSocketContainer webSockets, Handler httpHandler) {}
    }
}
