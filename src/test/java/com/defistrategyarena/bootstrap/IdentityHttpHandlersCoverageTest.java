package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IdentityHttpHandlersCoverageTest {

    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_OK = 200;
    private static final int STATUS_NO_CONTENT = 204;

    @Test
    void handlers_cover_bad_request_and_auth_header_paths() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            IdentityRestAdapter identity = composition.identityHttp();
            ObjectMapper mapper = new ObjectMapper();
            SignupHttpHandler signup = new SignupHttpHandler(identity, mapper);
            assertEquals(
                    STATUS_BAD_REQUEST,
                    signup.handle(new HttpRequest("POST", "/auth/signup", "{")).status());
            assertEquals(
                    STATUS_CREATED,
                    signup.handle(
                                    new HttpRequest(
                                            "POST",
                                            "/auth/signup",
                                            "{\"email\":\"h1@x.com\",\"password\":\"secret-value\",\"displayName\":\"H\"}"))
                            .status());
            LoginHttpHandler login = new LoginHttpHandler(identity, mapper);
            assertEquals(
                    STATUS_BAD_REQUEST,
                    login.handle(new HttpRequest("POST", "/auth/login", "{")).status());
            assertEquals(
                    STATUS_OK,
                    login.handle(
                                    new HttpRequest(
                                            "POST",
                                            "/auth/login",
                                            "{\"email\":\"h1@x.com\",\"password\":\"secret-value\"}"))
                            .status());
            String token =
                    identity
                            .login(
                                    new com.defistrategyarena.identity.adapter.web.LoginHttpRequest(
                                            "h1@x.com", "secret-value"))
                            .accessToken();
            MeHttpHandler me = new MeHttpHandler(new MeHttpHandler.MeHttpHandlerDeps(identity));
            assertEquals(
                    STATUS_UNAUTHORIZED, me.handle(new HttpRequest("GET", "/auth/me", "")).status());
            assertEquals(
                    STATUS_OK,
                    me.handle(
                                    new HttpRequest(
                                            "GET",
                                            "/auth/me",
                                            "",
                                            Map.of(),
                                            Map.of(),
                                            Map.of("Authorization", "Bearer " + token)))
                            .status());
            LogoutHttpHandler logout =
                    new LogoutHttpHandler(new LogoutHttpHandler.LogoutHttpHandlerDeps(identity));
            assertEquals(
                    STATUS_UNAUTHORIZED,
                    logout.handle(new HttpRequest("POST", "/auth/logout", "")).status());
            assertEquals(
                    STATUS_NO_CONTENT,
                    logout.handle(
                                    new HttpRequest(
                                            "POST",
                                            "/auth/logout",
                                            "",
                                            Map.of(),
                                            Map.of(),
                                            Map.of("Authorization", "Bearer " + token)))
                            .status());
            assertEquals(
                    STATUS_UNAUTHORIZED,
                    me.handle(
                                    new HttpRequest(
                                            "GET",
                                            "/auth/me",
                                            "",
                                            Map.of(),
                                            Map.of(),
                                            Map.of("Authorization", "Token nope")))
                            .status());
            assertEquals(
                    STATUS_UNAUTHORIZED,
                    logout.handle(
                                    new HttpRequest(
                                            "POST",
                                            "/auth/logout",
                                            "",
                                            Map.of(),
                                            Map.of(),
                                            Map.of("Authorization", "Token nope")))
                            .status());
        }
    }
}
