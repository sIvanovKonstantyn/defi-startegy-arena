package com.defistrategyarena.identity.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.Session;
import com.defistrategyarena.identity.domain.SessionId;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.identity.domain.UserId;
import com.defistrategyarena.shared.kernel.IdGenerationInput;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityDomainTest {

    private static final String EMAIL = "User@Example.com";
    private static final String NORMALIZED = "user@example.com";
    private static final String DISPLAY = "Ada";
    private static final String EMPTY = "";
    private static final String TOKEN_HASH = "abc123";
    private static final Instant EXPIRES = Instant.parse("2026-01-16T00:00:00Z");
    private static final Instant BEFORE = Instant.parse("2026-01-15T23:59:59Z");
    private static final Instant AFTER = Instant.parse("2026-01-16T00:00:00Z");

    @Test
    void user_create_normalizes_email_and_is_idempotent() {
        User first = User.create(new User.CreateUserData(EMAIL, DISPLAY));
        User second = User.create(new User.CreateUserData(EMAIL, DISPLAY));
        assertEquals(first.id(), second.id());
        assertEquals(NORMALIZED, first.normalizedEmail());
        assertEquals(EMAIL.trim(), first.email());
    }

    @Test
    void user_rejects_blank_fields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.create(new User.CreateUserData(EMPTY, DISPLAY)));
        assertThrows(
                IllegalArgumentException.class,
                () -> User.create(new User.CreateUserData(EMAIL, EMPTY)));
    }

    @Test
    void session_expiry_and_rehydrate() {
        UserId userId =
                UserId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(List.of(NORMALIZED))));
        Session session =
                Session.create(new Session.CreateSessionData(userId, TOKEN_HASH, EXPIRES));
        assertFalse(session.isExpiredAt(new Session.InstantReference(BEFORE)));
        assertTrue(session.isExpiredAt(new Session.InstantReference(AFTER)));
        Session restored =
                Session.rehydrate(
                        new Session.RehydrateSessionData(
                                session.id(), userId, TOKEN_HASH, EXPIRES));
        assertEquals(session.id(), restored.id());
        assertEquals(SessionId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(List.of(TOKEN_HASH)))),
                session.id());
    }

    @Test
    void password_hash_and_user_rehydrate() {
        PasswordHash hash = PasswordHash.create(new PasswordHash("encoded-value"));
        assertEquals("encoded-value", hash.encoded());
        User user = User.create(new User.CreateUserData(EMAIL, DISPLAY));
        User restored =
                User.rehydrate(new User.RehydrateUserData(user.id(), EMAIL, DISPLAY));
        assertEquals(user.id(), restored.id());
    }
}
