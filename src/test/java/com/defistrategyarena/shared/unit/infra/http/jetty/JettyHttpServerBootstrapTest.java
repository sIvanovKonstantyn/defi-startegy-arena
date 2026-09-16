package com.defistrategyarena.shared.unit.infra.http.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistration;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;
import com.defistrategyarena.shared.infra.http.HttpServerRuntime;
import com.defistrategyarena.shared.infra.http.HttpServerStartData;
import com.defistrategyarena.shared.infra.http.InMemoryHttpRouteRegistry;
import com.defistrategyarena.shared.infra.http.jetty.JettyHttpServerBootstrap;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;

class JettyHttpServerBootstrapTest {

    private static final int STATUS_OK = 200;
    private static final int STATUS_NOT_FOUND = 404;
    private static final String METHOD_GET = "GET";
    private static final String PATH_ECHO = "/echo";
    private static final String PLAIN_TEXT = "text/plain";
    private static final String ECHO_BODY = "ping";

    @Test
    void serves_registered_route_and_returns_not_found_for_missing_route() throws Exception {
        InMemoryHttpRouteRegistry routes = new InMemoryHttpRouteRegistry();
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(METHOD_GET, PATH_ECHO, JettyHttpServerBootstrapTest::echo)));

        HttpServerStartData startData =
                HttpServerStartData.create(
                        new HttpServerStartData(new HttpServerConfig(0), routes));

        JettyHttpServerBootstrap bootstrap = new JettyHttpServerBootstrap();
        try (HttpServerRuntime runtime = bootstrap.start(startData)) {
            HttpClient client = HttpClient.newHttpClient();
            URI echoUri = URI.create("http://localhost:" + runtime.port() + PATH_ECHO);
            java.net.http.HttpRequest echoRequest =
                    java.net.http.HttpRequest.newBuilder(echoUri).GET().build();
            String echoResponse = client.send(echoRequest, BodyHandlers.ofString()).body();
            assertEquals(ECHO_BODY, echoResponse);

            URI emptyQueryUri = URI.create("http://localhost:" + runtime.port() + PATH_ECHO + "?");
            int emptyQueryStatus =
                    client.send(
                                    java.net.http.HttpRequest.newBuilder(emptyQueryUri).GET().build(),
                                    BodyHandlers.ofString())
                            .statusCode();
            assertEquals(STATUS_OK, emptyQueryStatus);

            URI missingUri = URI.create("http://localhost:" + runtime.port() + "/missing");
            Builder missingBuilder = java.net.http.HttpRequest.newBuilder(missingUri).GET();
            int missingStatus =
                    client.send(missingBuilder.build(), BodyHandlers.ofString()).statusCode();
            assertEquals(STATUS_NOT_FOUND, missingStatus);
        }
    }

    @Test
    void start_wraps_server_bind_failures() {
        InMemoryHttpRouteRegistry routes = new InMemoryHttpRouteRegistry();
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(METHOD_GET, PATH_ECHO, JettyHttpServerBootstrapTest::echo)));

        HttpServerStartData firstStart =
                HttpServerStartData.create(new HttpServerStartData(new HttpServerConfig(0), routes));
        JettyHttpServerBootstrap bootstrap = new JettyHttpServerBootstrap();
        try (HttpServerRuntime first = bootstrap.start(firstStart)) {
            HttpServerStartData secondStart =
                    HttpServerStartData.create(
                            new HttpServerStartData(new HttpServerConfig(first.port()), routes));
            assertThrows(IllegalStateException.class, () -> bootstrap.start(secondStart));
        }
    }

    @Test
    void forwards_authorization_header_to_handler() throws Exception {
        InMemoryHttpRouteRegistry routes = new InMemoryHttpRouteRegistry();
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(METHOD_GET, PATH_ECHO, request -> {
                            String auth = request.authorizationHeader().orElse(ECHO_BODY);
                            return new HttpResponse(STATUS_OK, PLAIN_TEXT, auth);
                        })));
        HttpServerStartData startData =
                HttpServerStartData.create(
                        new HttpServerStartData(new HttpServerConfig(0), routes));
        JettyHttpServerBootstrap bootstrap = new JettyHttpServerBootstrap();
        try (HttpServerRuntime runtime = bootstrap.start(startData)) {
            HttpClient client = HttpClient.newHttpClient();
            URI echoUri = URI.create("http://localhost:" + runtime.port() + PATH_ECHO);
            java.net.http.HttpRequest echoRequest =
                    java.net.http.HttpRequest.newBuilder(echoUri)
                            .header("Authorization", "Bearer jetty-token")
                            .GET()
                            .build();
            String body = client.send(echoRequest, BodyHandlers.ofString()).body();
            assertEquals("Bearer jetty-token", body);

            java.net.http.HttpRequest blankAuth =
                    java.net.http.HttpRequest.newBuilder(echoUri)
                            .header("Authorization", " ")
                            .GET()
                            .build();
            assertEquals(ECHO_BODY, client.send(blankAuth, BodyHandlers.ofString()).body());
        }
    }

    private static HttpResponse echo(com.defistrategyarena.shared.infra.http.HttpRequest request) {
        java.util.Objects.requireNonNull(request);
        return new HttpResponse(STATUS_OK, PLAIN_TEXT, ECHO_BODY);
    }
}
