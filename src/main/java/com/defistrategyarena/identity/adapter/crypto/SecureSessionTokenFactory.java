package com.defistrategyarena.identity.adapter.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

public final class SecureSessionTokenFactory
        implements com.defistrategyarena.identity.application.SessionTokenFactory {

    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";
    private static final int TOKEN_BYTES = 32;
    private static final String HASH_FAILED = "token hashing failed";
    private static final String TOKEN_REQUIRED = "token must not be blank";

    private final SecureRandom secureRandom;
    private final String hashAlgorithm;

    public SecureSessionTokenFactory() {
        this(new FactoryConfig(new SecureRandom(), DEFAULT_HASH_ALGORITHM));
    }

    public SecureSessionTokenFactory(FactoryConfig config) {
        this.secureRandom = config.secureRandom();
        this.hashAlgorithm = config.hashAlgorithm();
    }

    @Override
    public IssuedToken issue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(rawToken, hash(new RawToken(rawToken)));
    }

    @Override
    public String hash(RawToken token) {
        Objects.requireNonNull(token);
        String value = requireTokenValue(token);
        try {
            MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HASH_FAILED, exception);
        }
    }

    private static String requireTokenValue(RawToken token) {
        if (token.value() == null || token.value().isBlank()) {
            throw new IllegalArgumentException(TOKEN_REQUIRED);
        }
        return token.value();
    }

    public record FactoryConfig(SecureRandom secureRandom, String hashAlgorithm) {}
}
