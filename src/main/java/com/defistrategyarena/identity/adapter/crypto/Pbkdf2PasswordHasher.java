package com.defistrategyarena.identity.adapter.crypto;

import com.defistrategyarena.identity.application.PasswordHasher;
import com.defistrategyarena.identity.domain.PasswordHash;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String DEFAULT_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int ITERATIONS = 210_000;
    private static final String PART_SEPARATOR = ":";
    private static final int EXPECTED_PARTS = 3;
    private static final int INDEX_ITERATIONS = 0;
    private static final int INDEX_SALT = 1;
    private static final int INDEX_HASH = 2;
    private static final int HASH_COMPARE_EQUAL = 0;
    private static final int INDEX_START = 0;
    private static final String PASSWORD_REQUIRED = "password must not be blank";
    private static final String REQUEST_REQUIRED = "password match request must not be null";
    private static final String HASH_FAILED = "password hashing failed";

    private final SecureRandom secureRandom;
    private final String algorithm;

    public Pbkdf2PasswordHasher() {
        this(new HasherConfig(new SecureRandom(), DEFAULT_ALGORITHM));
    }

    public Pbkdf2PasswordHasher(HasherConfig config) {
        this.secureRandom = config.secureRandom();
        this.algorithm = config.algorithm();
    }

    @Override
    public PasswordHash hash(PlainPassword password) {
        Objects.requireNonNull(password);
        requirePassword(new PasswordText(password.value()));
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(new DeriveInput(password.value().toCharArray(), salt, ITERATIONS));
        String encoded =
                ITERATIONS
                        + PART_SEPARATOR
                        + Base64.getEncoder().encodeToString(salt)
                        + PART_SEPARATOR
                        + Base64.getEncoder().encodeToString(derived);
        return PasswordHash.create(new PasswordHash(encoded));
    }

    @Override
    public boolean matches(PasswordMatchRequest request) {
        Objects.requireNonNull(request, REQUEST_REQUIRED);
        requirePassword(new PasswordText(request.password().value()));
        EncodedParts parts = parse(new EncodedHash(request.hash().encoded()));
        byte[] actual =
                derive(
                        new DeriveInput(
                                request.password().value().toCharArray(),
                                parts.salt(),
                                parts.iterations()));
        return constantTimeEquals(new BytePair(parts.hash(), actual));
    }

    private static void requirePassword(PasswordText password) {
        if (password.value() == null || password.value().isBlank()) {
            throw new IllegalArgumentException(PASSWORD_REQUIRED);
        }
    }

    private byte[] derive(DeriveInput input) {
        try {
            KeySpec spec =
                    new PBEKeySpec(input.password(), input.salt(), input.iterations(), KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(HASH_FAILED, exception);
        }
    }

    private static EncodedParts parse(EncodedHash encoded) {
        String[] parts = encoded.value().split(PART_SEPARATOR);
        if (parts.length != EXPECTED_PARTS) {
            throw new IllegalArgumentException(HASH_FAILED);
        }
        int iterations = Integer.parseInt(parts[INDEX_ITERATIONS]);
        byte[] salt = Base64.getDecoder().decode(parts[INDEX_SALT]);
        byte[] hash = Base64.getDecoder().decode(parts[INDEX_HASH]);
        return new EncodedParts(iterations, salt, hash);
    }

    private static boolean constantTimeEquals(BytePair pair) {
        if (pair.left().length != pair.right().length) {
            return false;
        }
        int result = HASH_COMPARE_EQUAL;
        for (int index = INDEX_START; index < pair.left().length; index++) {
            result |= pair.left()[index] ^ pair.right()[index];
        }
        return result == HASH_COMPARE_EQUAL;
    }

    public record HasherConfig(SecureRandom secureRandom, String algorithm) {}

    private record PasswordText(String value) {}

    private record DeriveInput(char[] password, byte[] salt, int iterations) {}

    private record EncodedHash(String value) {}

    private record EncodedParts(int iterations, byte[] salt, byte[] hash) {}

    private record BytePair(byte[] left, byte[] right) {}
}
