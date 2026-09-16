package com.defistrategyarena.identity.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.adapter.crypto.Pbkdf2PasswordHasher;
import com.defistrategyarena.identity.application.PasswordHasher;
import com.defistrategyarena.identity.domain.PasswordHash;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class Pbkdf2PasswordHasherTest {

    private static final String PASSWORD = "secret-value";
    private static final String OTHER = "other-value";
    private static final String EMPTY = "";
    private static final String BAD_ENCODED = "not-a-hash";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    @Test
    void hashes_and_verifies_password() {
        Pbkdf2PasswordHasher hasher =
                new Pbkdf2PasswordHasher(
                        new Pbkdf2PasswordHasher.HasherConfig(new SecureRandom(), ALGORITHM));
        PasswordHash hash = hasher.hash(new PasswordHasher.PlainPassword(PASSWORD));
        assertTrue(
                hasher.matches(
                        new PasswordHasher.PasswordMatchRequest(
                                new PasswordHasher.PlainPassword(PASSWORD), hash)));
        assertFalse(
                hasher.matches(
                        new PasswordHasher.PasswordMatchRequest(
                                new PasswordHasher.PlainPassword(OTHER), hash)));
        assertNotEquals(PASSWORD, hash.encoded());
    }

    @Test
    void rejects_blank_password() {
        Pbkdf2PasswordHasher hasher =
                new Pbkdf2PasswordHasher(
                        new Pbkdf2PasswordHasher.HasherConfig(new SecureRandom(), ALGORITHM));
        assertThrows(
                IllegalArgumentException.class,
                () -> hasher.hash(new PasswordHasher.PlainPassword(EMPTY)));
    }

    @Test
    void rejects_malformed_encoded_hash() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hasher.matches(
                                new PasswordHasher.PasswordMatchRequest(
                                        new PasswordHasher.PlainPassword(PASSWORD),
                                        new PasswordHash(BAD_ENCODED))));
    }

    @Test
    void wraps_unknown_algorithm() {
        Pbkdf2PasswordHasher hasher =
                new Pbkdf2PasswordHasher(
                        new Pbkdf2PasswordHasher.HasherConfig(new SecureRandom(), "NoSuchAlgo"));
        assertThrows(
                IllegalStateException.class,
                () -> hasher.hash(new PasswordHasher.PlainPassword(PASSWORD)));
    }
}
