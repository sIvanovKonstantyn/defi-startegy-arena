package com.defistrategyarena.shared.infra.http.jetty;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.shared.infra.http.HttpRouteLookup;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class JettyRouteDispatchHandler extends Handler.Abstract {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String NOT_FOUND_BODY = "not found";
    private static final String PLAIN_TEXT = "text/plain";

    private final HttpRouteRegistry routes;

    public JettyRouteDispatchHandler(HttpRouteRegistry routes) {
        this.routes = routes;
    }

    @Override
    @SuppressWarnings("PMD.MethodsTakeAtMostOneDtoParameter")
    public boolean handle(Request request, Response response, Callback callback) throws IOException {
        String method = request.getMethod();
        String path = Request.getPathInContext(request);
        HttpRouteLookup lookup = HttpRouteLookup.create(new HttpRouteLookup(method, path));
        Optional<HttpHandler> handler = routes.find(lookup);
        if (handler.isEmpty()) {
            writeResponse(
                    JettyResponseWriteData.create(
                            new JettyResponseWriteData(
                                    response,
                                    callback,
                                    new HttpResponse(HttpStatus.NOT_FOUND_404, PLAIN_TEXT, NOT_FOUND_BODY))));
            return true;
        }

        HttpRequest httpRequest = new HttpRequest(method, path, readBody(request));
        HttpResponse httpResponse = handler.get().handle(httpRequest);
        writeResponse(
                JettyResponseWriteData.create(
                        new JettyResponseWriteData(response, callback, httpResponse)));
        return true;
    }

    private static String readBody(Request request) throws IOException {
        try (InputStream input = Request.asInputStream(request)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeResponse(JettyResponseWriteData writeData) {
        Response response = writeData.response();
        Callback callback = writeData.callback();
        HttpResponse httpResponse = writeData.httpResponse();
        response.setStatus(httpResponse.status());
        response.getHeaders().put(CONTENT_TYPE_HEADER, httpResponse.contentType());
        byte[] payload = httpResponse.body().getBytes(StandardCharsets.UTF_8);
        response.write(true, ByteBuffer.wrap(payload), callback);
    }
}
