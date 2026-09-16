package com.defistrategyarena.identity.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.identity.application.LoginWithPassword;
import com.defistrategyarena.identity.application.Logout;
import com.defistrategyarena.identity.application.RegisterWithPassword;
import com.defistrategyarena.identity.application.SessionIssuer;
import com.defistrategyarena.identity.application.SessionTokenFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityAuthE2ETest {

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_NO_CONTENT = 204;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_CONFLICT = 409;
    private static final int SINGLE_USER = 1;
    private static final long SESSION_TTL_SECONDS = 86_400L;
    private static final String EMAIL = "a@b.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY_NAME = "Ada";
    private static final String WRONG_PASSWORD = "wrong-password";
    private static final String EMPTY = "";
    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T12:00:00Z");

    private InMemoryUserRepository users;
    private IdentityRestAdapter http;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        InMemorySessionRepository sessions = new InMemorySessionRepository();
        Pbkdf2PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        SessionTokenFactory tokens = new SecureSessionTokenFactory();
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        SessionIssuer sessionIssuer =
                new SessionIssuer(
                        new SessionIssuer.SessionIssuerDeps(
                                sessions, tokens, clock, SESSION_TTL_SECONDS));
        http =
                new IdentityRestAdapter(
                        new IdentityRestAdapter.IdentityRestAdapterDeps(
                                new RegisterWithPassword(
                                        new RegisterWithPassword.RegisterWithPasswordDeps(
                                                users, passwordHasher, sessionIssuer)),
                                new LoginWithPassword(
                                        new LoginWithPassword.LoginWithPasswordDeps(
                                                users, passwordHasher, sessionIssuer)),
                                new Logout(new Logout.LogoutDeps(sessions, tokens, clock)),
                                new GetCurrentUser(
                                        new GetCurrentUser.GetCurrentUserDeps(
                                                sessions, users, tokens, clock))));
    }

    @Test
    void s1_signup_with_email_password_returns_session_and_me() {
        SessionHttpResponse signup =
                http.signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME));

        assertEquals(STATUS_CREATED, signup.status());
        assertNotEquals(EMPTY, signup.accessToken());
        assertFalse(signup.accessToken().contains(PASSWORD));

        UserHttpResponse me = http.me(new AccessTokenHttpRequest(signup.accessToken()));
        assertEquals(STATUS_OK, me.status());
        assertEquals(DISPLAY_NAME, me.displayName());
        assertEquals(SINGLE_USER, users.size());
    }

    @Test
    void s2_rejects_duplicate_email() {
        http.signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME));

        SessionHttpResponse duplicate =
                http.signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME));

        assertEquals(STATUS_CONFLICT, duplicate.status());
        assertEquals(SINGLE_USER, users.size());
    }

    @Test
    void s3_login_success_and_failure() {
        http.signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME));

        SessionHttpResponse ok = http.login(new LoginHttpRequest(EMAIL, PASSWORD));
        assertEquals(STATUS_OK, ok.status());
        assertNotEquals(EMPTY, ok.accessToken());

        SessionHttpResponse bad = http.login(new LoginHttpRequest(EMAIL, WRONG_PASSWORD));
        assertEquals(STATUS_UNAUTHORIZED, bad.status());
    }

    @Test
    void s4_logout_invalidates_session() {
        SessionHttpResponse signup =
                http.signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY_NAME));

        LogoutHttpResponse logout = http.logout(new AccessTokenHttpRequest(signup.accessToken()));
        assertEquals(STATUS_NO_CONTENT, logout.status());

        UserHttpResponse me = http.me(new AccessTokenHttpRequest(signup.accessToken()));
        assertEquals(STATUS_UNAUTHORIZED, me.status());
    }
}
