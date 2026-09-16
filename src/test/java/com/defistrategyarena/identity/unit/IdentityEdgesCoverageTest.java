package com.defistrategyarena.identity.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.bootstrap.ApplicationComposition;
import com.defistrategyarena.bootstrap.ApplicationRoutes;
import com.defistrategyarena.bootstrap.BearerAccessToken;
import com.defistrategyarena.identity.adapter.crypto.Pbkdf2PasswordHasher;
import com.defistrategyarena.identity.adapter.crypto.SecureSessionTokenFactory;
import com.defistrategyarena.identity.adapter.persistence.InMemorySessionRepository;
import com.defistrategyarena.identity.adapter.persistence.InMemoryUserRepository;
import com.defistrategyarena.identity.adapter.web.AccessTokenHttpRequest;
import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.adapter.web.LoginHttpRequest;
import com.defistrategyarena.identity.adapter.web.LogoutHttpResponse;
import com.defistrategyarena.identity.adapter.web.SessionHttpResponse;
import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.identity.adapter.web.UserHttpResponse;
import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.identity.application.AuthSessionResult;
import com.defistrategyarena.identity.application.DuplicateEmailException;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.identity.application.InvalidCredentialsException;
import com.defistrategyarena.identity.application.LoginWithPassword;
import com.defistrategyarena.identity.application.LoginWithPasswordCommand;
import com.defistrategyarena.identity.application.Logout;
import com.defistrategyarena.identity.application.RegisterWithPassword;
import com.defistrategyarena.identity.application.RegisterWithPasswordCommand;
import com.defistrategyarena.identity.application.SessionIssuer;
import com.defistrategyarena.identity.application.SessionTokenFactory;
import com.defistrategyarena.identity.application.UnauthorizedException;
import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.Session;
import com.defistrategyarena.identity.domain.SessionId;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.identity.domain.UserId;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpRouteLookup;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistry;
import com.defistrategyarena.shared.kernel.IdGenerationInput;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IdentityEdgesCoverageTest {

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_NO_CONTENT = 204;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final long TTL = 60L;
    private static final String EMAIL = "edge@example.com";
    private static final String PASSWORD = "password-value";
    private static final String DISPLAY = "Edge";
    private static final String EMPTY = "";
    private static final Instant NOW = Instant.parse("2026-02-01T10:00:00Z");
    private static final Instant PAST = Instant.parse("2026-02-01T09:00:00Z");

    @Test
    void covers_commands_dtos_exceptions_and_bearer_parsing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegisterWithPasswordCommand(EMPTY, PASSWORD, DISPLAY));
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoginWithPasswordCommand(EMAIL, EMPTY));
        assertThrows(IllegalArgumentException.class, () -> new AccessTokenQuery(EMPTY));
        assertEquals(
                PASSWORD,
                AccessTokenQuery.create(new AccessTokenQuery(PASSWORD)).accessToken());
        assertEquals(
                EMAIL,
                RegisterWithPasswordCommand.create(
                                new RegisterWithPasswordCommand(EMAIL, PASSWORD, DISPLAY))
                        .email());
        assertEquals(
                EMAIL,
                LoginWithPasswordCommand.create(new LoginWithPasswordCommand(EMAIL, PASSWORD))
                        .email());
        assertEquals(
                "tok",
                AccessTokenQuery.fromAuthorizationHeader(
                                new AccessTokenQuery.AuthorizationHeader("Bearer tok"))
                        .accessToken());
        assertThrows(
                UnauthorizedException.class,
                () ->
                        AccessTokenQuery.fromAuthorizationHeader(
                                new AccessTokenQuery.AuthorizationHeader("Basic x")));
        assertThrows(
                UnauthorizedException.class,
                () ->
                        AccessTokenQuery.fromAuthorizationHeader(
                                new AccessTokenQuery.AuthorizationHeader("Bearer ")));
        assertThrows(
                UnauthorizedException.class,
                () ->
                        AccessTokenQuery.fromAuthorizationHeader(
                                new AccessTokenQuery.AuthorizationHeader(EMPTY)));
        assertEquals("m", new DuplicateEmailException(new RuntimeException("m")).getCause().getMessage());
        assertEquals(
                "m",
                new InvalidCredentialsException(new RuntimeException("m")).getCause().getMessage());
        assertEquals(
                "m", new UnauthorizedException(new RuntimeException("m")).getCause().getMessage());
        AuthSessionResult session = AuthSessionResult.create(new AuthSessionResult("token"));
        assertEquals("token", session.accessToken());
        assertEquals(
                "t",
                AccessTokenHttpRequest.create(new AccessTokenHttpRequest("t")).accessToken());
        assertEquals(
                EMAIL,
                SignupHttpRequest.create(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY)).email());
        assertEquals(
                EMAIL, LoginHttpRequest.create(new LoginHttpRequest(EMAIL, PASSWORD)).email());
        assertEquals(
                STATUS_NO_CONTENT,
                LogoutHttpResponse.create(new LogoutHttpResponse(STATUS_NO_CONTENT)).status());
        assertEquals(
                STATUS_OK,
                SessionHttpResponse.create(new SessionHttpResponse(STATUS_OK, "a")).status());
        assertEquals(
                STATUS_OK,
                UserHttpResponse.create(new UserHttpResponse(STATUS_OK, DISPLAY))
                        .status());
        assertEquals(
                EMPTY,
                BearerAccessToken.from(new HttpRequest("GET", "/auth/me", "")).accessToken());
        assertEquals(
                "abc",
                BearerAccessToken.from(
                                new HttpRequest(
                                        "GET",
                                        "/auth/me",
                                        "",
                                        Map.of(),
                                        Map.of(),
                                        Map.of("Authorization", "Bearer abc")))
                        .accessToken());
        assertEquals(
                EMPTY,
                BearerAccessToken.from(
                                new HttpRequest(
                                        "GET",
                                        "/auth/me",
                                        "",
                                        Map.of(),
                                        Map.of(),
                                        Map.of("Authorization", "Token abc")))
                        .accessToken());
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegisterWithPasswordCommand(EMAIL, PASSWORD, EMPTY));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegisterWithPasswordCommand(null, PASSWORD, DISPLAY));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegisterWithPasswordCommand(EMAIL, null, DISPLAY));
        assertThrows(
                IllegalArgumentException.class, () -> new LoginWithPasswordCommand(null, PASSWORD));
        assertThrows(
                UnauthorizedException.class,
                () ->
                        AccessTokenQuery.fromAuthorizationHeader(
                                new AccessTokenQuery.AuthorizationHeader(null)));
        SecureSessionTokenFactory tokens = new SecureSessionTokenFactory();
        assertThrows(
                IllegalArgumentException.class,
                () -> tokens.hash(new SessionTokenFactory.RawToken(null)));
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hasher.hash(
                                new com.defistrategyarena.identity.application.PasswordHasher
                                        .PlainPassword(null)));
        PasswordHash crafted = new PasswordHash("210000:YWJjZGVmZ2hpams=:YQ==");
        assertEquals(
                false,
                hasher.matches(
                        new com.defistrategyarena.identity.application.PasswordHasher
                                .PasswordMatchRequest(
                                new com.defistrategyarena.identity.application.PasswordHasher
                                        .PlainPassword(PASSWORD),
                                crafted)));
        java.util.HashMap<String, String> headers = new java.util.HashMap<>();
        headers.put(null, "x");
        headers.put("X-Test", null);
        headers.put("Authorization", "Bearer z");
        assertEquals(
                "Bearer z",
                new HttpRequest("GET", "/auth/me", "", Map.of(), Map.of(), headers)
                        .authorizationHeader()
                        .orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
        assertThrows(IllegalArgumentException.class, () -> new SessionId(null));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash(null));
        assertThrows(
                NullPointerException.class,
                () ->
                        Session.create(
                                new Session.CreateSessionData(
                                        UserId.create(
                                                IdGenerationInput.create(
                                                        new IdGenerationInput.StringListFields(
                                                                List.of(EMAIL)))),
                                        "hash",
                                        null)));
        assertThrows(
                IllegalArgumentException.class, () -> new AccessTokenQuery(null));
        assertThrows(
                IllegalArgumentException.class, () -> new LoginWithPasswordCommand(" ", PASSWORD));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegisterWithPasswordCommand(EMAIL, " ", DISPLAY));
        assertThrows(
                IllegalArgumentException.class,
                () -> User.create(new User.CreateUserData(null, DISPLAY)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Session.create(
                                new Session.CreateSessionData(
                                        UserId.create(
                                                IdGenerationInput.create(
                                                        new IdGenerationInput.StringListFields(
                                                                List.of(EMAIL)))),
                                        null,
                                        NOW)));
        Session timedSession =
                Session.create(
                        new Session.CreateSessionData(
                                UserId.create(
                                        IdGenerationInput.create(
                                                new IdGenerationInput.StringListFields(
                                                        List.of(EMAIL)))),
                                "token-hash-value",
                                NOW.plusSeconds(TTL)));
        assertEquals(NOW.plusSeconds(TTL), timedSession.expiresAt());
        assertThrows(
                IllegalArgumentException.class, () -> new LoginWithPasswordCommand(EMAIL, " "));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegisterWithPasswordCommand(EMAIL, PASSWORD, null));
    }

    @Test
    void covers_domain_and_repository_edges() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(EMPTY));
        assertThrows(IllegalArgumentException.class, () -> new SessionId(EMPTY));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash(EMPTY));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Session.create(
                                new Session.CreateSessionData(
                                        UserId.create(
                                                IdGenerationInput.create(
                                                        new IdGenerationInput.StringListFields(
                                                                List.of(EMAIL)))),
                                        EMPTY,
                                        NOW)));
        InMemorySessionRepository sessions = new InMemorySessionRepository();
        sessions.delete(
                SessionId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(List.of("missing")))));
        InMemoryUserRepository users = new InMemoryUserRepository();
        User user = User.create(new User.CreateUserData(EMAIL, DISPLAY));
        users.save(user);
        users.save(user);
        assertTrue(users.findById(user.id()).isPresent());
        assertThrows(
                DuplicateEmailException.class,
                () ->
                        users.save(
                                User.rehydrate(
                                        new User.RehydrateUserData(
                                                UserId.create(
                                                        IdGenerationInput.create(
                                                                new IdGenerationInput.StringListFields(
                                                                        List.of("other@x.com")))),
                                                EMAIL,
                                                DISPLAY))));
    }

    @Test
    void covers_auth_flows_edges_and_http_handlers() throws Exception {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemorySessionRepository sessions = new InMemorySessionRepository();
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        SecureSessionTokenFactory tokens = new SecureSessionTokenFactory();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SessionIssuer issuer =
                new SessionIssuer(new SessionIssuer.SessionIssuerDeps(sessions, tokens, clock, TTL));
        IdentityRestAdapter http =
                new IdentityRestAdapter(
                        new IdentityRestAdapter.IdentityRestAdapterDeps(
                                new RegisterWithPassword(
                                        new RegisterWithPassword.RegisterWithPasswordDeps(
                                                users, hasher, issuer)),
                                new LoginWithPassword(
                                        new LoginWithPassword.LoginWithPasswordDeps(
                                                users, hasher, issuer)),
                                new Logout(new Logout.LogoutDeps(sessions, tokens, clock)),
                                new GetCurrentUser(
                                        new GetCurrentUser.GetCurrentUserDeps(
                                                sessions, users, tokens, clock))));
        SessionHttpResponse signup =
                http.signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY));
        assertEquals(STATUS_CREATED, signup.status());
        assertEquals(STATUS_BAD_REQUEST, http.signup(new SignupHttpRequest(EMPTY, PASSWORD, DISPLAY)).status());
        assertEquals(STATUS_BAD_REQUEST, http.login(new LoginHttpRequest(EMPTY, PASSWORD)).status());
        assertEquals(STATUS_UNAUTHORIZED, http.login(new LoginHttpRequest("missing@x.com", PASSWORD)).status());
        assertEquals(STATUS_UNAUTHORIZED, http.logout(new AccessTokenHttpRequest("missing")).status());
        assertEquals(STATUS_UNAUTHORIZED, http.me(new AccessTokenHttpRequest("missing")).status());
        User passwordless =
                User.create(new User.CreateUserData("passwordless@x.com", DISPLAY));
        users.save(passwordless);
        assertEquals(
                STATUS_UNAUTHORIZED,
                http.login(new LoginHttpRequest("passwordless@x.com", PASSWORD)).status());
        Session expired =
                Session.create(
                        new Session.CreateSessionData(
                                passwordless.id(),
                                tokens.hash(new SessionTokenFactory.RawToken("raw")),
                                PAST));
        sessions.save(expired);
        assertEquals(STATUS_UNAUTHORIZED, http.me(new AccessTokenHttpRequest("raw")).status());
        assertEquals(STATUS_UNAUTHORIZED, http.logout(new AccessTokenHttpRequest("raw")).status());
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            HttpRouteRegistry routes = ApplicationRoutes.createDefaultRoutes(composition);
            int signupStatus =
                    routes.find(HttpRouteLookup.create(new HttpRouteLookup("POST", "/auth/signup")))
                            .orElseThrow()
                            .handler()
                            .handle(
                                    new HttpRequest(
                                            "POST",
                                            "/auth/signup",
                                            "{\"email\":\"h@x.com\",\"password\":\"secret-value\",\"displayName\":\"H\"}"))
                            .status();
            assertEquals(STATUS_CREATED, signupStatus);
            int badSignup =
                    routes.find(HttpRouteLookup.create(new HttpRouteLookup("POST", "/auth/signup")))
                            .orElseThrow()
                            .handler()
                            .handle(new HttpRequest("POST", "/auth/signup", "{"))
                            .status();
            assertEquals(STATUS_BAD_REQUEST, badSignup);
            int loginStatus =
                    routes.find(HttpRouteLookup.create(new HttpRouteLookup("POST", "/auth/login")))
                            .orElseThrow()
                            .handler()
                            .handle(
                                    new HttpRequest(
                                            "POST",
                                            "/auth/login",
                                            "{\"email\":\"h@x.com\",\"password\":\"secret-value\"}"))
                            .status();
            assertEquals(STATUS_OK, loginStatus);
            String token =
                    composition
                            .identityHttp()
                            .login(new LoginHttpRequest("h@x.com", "secret-value"))
                            .accessToken();
            int meStatus =
                    routes.find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/auth/me")))
                            .orElseThrow()
                            .handler()
                            .handle(
                                    new HttpRequest(
                                            "GET",
                                            "/auth/me",
                                            "",
                                            Map.of(),
                                            Map.of(),
                                            Map.of("Authorization", "Bearer " + token)))
                            .status();
            assertEquals(STATUS_OK, meStatus);
            int logoutStatus =
                    routes.find(HttpRouteLookup.create(new HttpRouteLookup("POST", "/auth/logout")))
                            .orElseThrow()
                            .handler()
                            .handle(
                                    new HttpRequest(
                                            "POST",
                                            "/auth/logout",
                                            "",
                                            Map.of(),
                                            Map.of(),
                                            Map.of("Authorization", "Bearer " + token)))
                            .status();
            assertEquals(STATUS_NO_CONTENT, logoutStatus);
            int meUnauthorized =
                    routes.find(HttpRouteLookup.create(new HttpRouteLookup("GET", "/auth/me")))
                            .orElseThrow()
                            .handler()
                            .handle(new HttpRequest("GET", "/auth/me", ""))
                            .status();
            assertEquals(STATUS_UNAUTHORIZED, meUnauthorized);
        }
        PasswordHash hash =
                hasher.hash(
                        new com.defistrategyarena.identity.application.PasswordHasher.PlainPassword(
                                PASSWORD));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hasher.matches(
                                new com.defistrategyarena.identity.application.PasswordHasher
                                        .PasswordMatchRequest(
                                        new com.defistrategyarena.identity.application.PasswordHasher
                                                .PlainPassword(EMPTY),
                                        hash)));
    }
}
