package com.defistrategyarena.identity.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class Session {

    private static final String DATA_REQUIRED = "create session data must not be null";
    private static final String REHYDRATE_REQUIRED = "rehydrate session data must not be null";
    private static final String TOKEN_HASH_REQUIRED = "token hash must not be blank";
    private static final String EXPIRES_REQUIRED = "expires at must not be null";

    private final SessionId id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;

    private Session(SessionId id, UserId userId, String tokenHash, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static Session create(CreateSessionData data) {
        Objects.requireNonNull(data, DATA_REQUIRED);
        String tokenHash = requireTokenHash(new TokenHashText(data.tokenHash()));
        Instant expiresAt = Objects.requireNonNull(data.expiresAt(), EXPIRES_REQUIRED);
        SessionId id =
                SessionId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(List.of(tokenHash))));
        return new Session(id, data.userId(), tokenHash, expiresAt);
    }

    public static Session rehydrate(RehydrateSessionData data) {
        Objects.requireNonNull(data, REHYDRATE_REQUIRED);
        String tokenHash = requireTokenHash(new TokenHashText(data.tokenHash()));
        Instant expiresAt = Objects.requireNonNull(data.expiresAt(), EXPIRES_REQUIRED);
        return new Session(data.id(), data.userId(), tokenHash, expiresAt);
    }

    public boolean isExpiredAt(InstantReference reference) {
        Objects.requireNonNull(reference);
        return !reference.instant().isBefore(expiresAt);
    }

    public SessionId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    private static String requireTokenHash(TokenHashText tokenHash) {
        if (tokenHash.value() == null || tokenHash.value().isBlank()) {
            throw new IllegalArgumentException(TOKEN_HASH_REQUIRED);
        }
        return tokenHash.value();
    }

    private record TokenHashText(String value) {}

    public record InstantReference(Instant instant) {}

    public record CreateSessionData(UserId userId, String tokenHash, Instant expiresAt) {}

    public record RehydrateSessionData(
            SessionId id, UserId userId, String tokenHash, Instant expiresAt) {}
}
