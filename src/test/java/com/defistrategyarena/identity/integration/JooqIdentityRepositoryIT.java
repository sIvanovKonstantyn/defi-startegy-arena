package com.defistrategyarena.identity.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.crypto.Pbkdf2PasswordHasher;
import com.defistrategyarena.identity.adapter.persistence.JooqSessionRepository;
import com.defistrategyarena.identity.adapter.persistence.JooqUserRepository;
import com.defistrategyarena.identity.adapter.persistence.jooq.tables.AuthSessions;
import com.defistrategyarena.identity.adapter.persistence.jooq.tables.UserPasswords;
import com.defistrategyarena.identity.adapter.persistence.jooq.tables.Users;
import com.defistrategyarena.identity.application.DuplicateEmailException;
import com.defistrategyarena.identity.application.PasswordHasher;
import com.defistrategyarena.identity.application.SessionRepository;
import com.defistrategyarena.identity.application.UserRepository;
import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.Session;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.identity.domain.UserId;
import com.defistrategyarena.shared.kernel.IdGenerationInput;
import com.defistrategyarena.strategy.integration.H2PostgresModeSupport;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JooqIdentityRepositoryIT {

    private static final String DB_NAME = "identity_repo_it";
    private static final String EMAIL = "ada@example.co";
    private static final String OTHER_EMAIL = "other@example.co";
    private static final String DISPLAY = "Ada";
    private static final String OTHER_DISPLAY = "Other";
    private static final String PASSWORD = "secret-value";
    private static final String TOKEN_HASH = "token-hash-value-1";
    private static final String OTHER_TOKEN_HASH = "token-hash-value-2";
    private static final int SINGLE = 1;
    private static final Instant EXPIRES = Instant.parse("2030-01-01T00:00:00Z");

    private HikariDataSource dataSource;
    private DSLContext dsl;
    private JooqUserRepository users;
    private JooqSessionRepository sessions;
    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        dataSource = H2PostgresModeSupport.dataSource(new H2PostgresModeSupport.DatabaseName(DB_NAME));
        dsl = H2PostgresModeSupport.migratedDsl(dataSource);
        dsl.deleteFrom(AuthSessions.AUTH_SESSIONS).execute();
        dsl.deleteFrom(UserPasswords.USER_PASSWORDS).execute();
        dsl.deleteFrom(Users.USERS).execute();
        users = new JooqUserRepository(dsl);
        sessions = new JooqSessionRepository(dsl);
        hasher = new Pbkdf2PasswordHasher();
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void user_and_session_round_trip_with_duplicate_email_rejection() {
        User user = User.create(new User.CreateUserData(EMAIL, DISPLAY));
        PasswordHash hash = hasher.hash(new PasswordHasher.PlainPassword(PASSWORD));
        users.saveWithPassword(new UserRepository.SaveUserWithPasswordCommand(user, hash));

        assertEquals(SINGLE, users.size());
        assertTrue(users.findByEmail(new UserRepository.EmailLookup(EMAIL)).isPresent());
        assertTrue(users.findById(user.id()).isPresent());
        assertTrue(users.findPasswordHash(user.id()).isPresent());

        users.save(user);
        assertEquals(SINGLE, users.size());

        User conflicting =
                User.rehydrate(
                        new User.RehydrateUserData(
                                UserId.create(
                                        IdGenerationInput.create(
                                                new IdGenerationInput.StringListFields(
                                                        List.of(OTHER_EMAIL)))),
                                EMAIL,
                                OTHER_DISPLAY));
        assertThrows(DuplicateEmailException.class, () -> users.save(conflicting));

        Session session =
                Session.create(new Session.CreateSessionData(user.id(), TOKEN_HASH, EXPIRES));
        sessions.save(session);
        assertTrue(
                sessions
                        .findByTokenHash(new SessionRepository.TokenHashLookup(TOKEN_HASH))
                        .isPresent());

        sessions.delete(session.id());
        assertTrue(
                sessions
                        .findByTokenHash(new SessionRepository.TokenHashLookup(TOKEN_HASH))
                        .isEmpty());

        User second = User.create(new User.CreateUserData(OTHER_EMAIL, OTHER_DISPLAY));
        users.saveWithPassword(
                new UserRepository.SaveUserWithPasswordCommand(
                        second, hasher.hash(new PasswordHasher.PlainPassword(PASSWORD))));
        PasswordHash rotated = hasher.hash(new PasswordHasher.PlainPassword(PASSWORD + "-2"));
        users.saveWithPassword(new UserRepository.SaveUserWithPasswordCommand(second, rotated));
        assertEquals(rotated.encoded(), users.findPasswordHash(second.id()).orElseThrow().encoded());

        Session other =
                Session.create(new Session.CreateSessionData(second.id(), OTHER_TOKEN_HASH, EXPIRES));
        sessions.save(other);
        sessions.delete(other.id());
        assertTrue(
                sessions
                        .findByTokenHash(new SessionRepository.TokenHashLookup(OTHER_TOKEN_HASH))
                        .isEmpty());
    }

    @Test
    void rejects_null_dsl_contexts() {
        assertThrows(IllegalArgumentException.class, () -> new JooqUserRepository(null));
        assertThrows(IllegalArgumentException.class, () -> new JooqSessionRepository(null));
    }
}
