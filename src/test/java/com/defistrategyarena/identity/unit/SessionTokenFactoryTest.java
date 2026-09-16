package com.defistrategyarena.identity.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.identity.adapter.crypto.SecureSessionTokenFactory;
import com.defistrategyarena.identity.application.SessionTokenFactory;
import com.defistrategyarena.identity.application.SessionTokenFactory.IssuedToken;
import com.defistrategyarena.identity.application.SessionTokenFactory.RawToken;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class SessionTokenFactoryTest {

    private static final String EMPTY = "";

    @Test
    void issues_opaque_token_and_stable_hash() {
        SessionTokenFactory factory =
                new SecureSessionTokenFactory(
                        new SecureSessionTokenFactory.FactoryConfig(new SecureRandom(), "SHA-256"));
        IssuedToken issued = factory.issue();
        assertNotEquals(EMPTY, issued.rawToken());
        assertEquals(factory.hash(new RawToken(issued.rawToken())), issued.tokenHash());
        assertNotEquals(issued.rawToken(), issued.tokenHash());
    }

    @Test
    void rejects_blank_token_for_hash() {
        SessionTokenFactory factory = new SecureSessionTokenFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.hash(new RawToken(EMPTY)));
    }

    @Test
    void wraps_unknown_hash_algorithm() {
        SecureSessionTokenFactory factory =
                new SecureSessionTokenFactory(
                        new SecureSessionTokenFactory.FactoryConfig(
                                new SecureRandom(), "NotARealDigest"));
        assertThrows(IllegalStateException.class, () -> factory.hash(new RawToken("token")));
    }
}
