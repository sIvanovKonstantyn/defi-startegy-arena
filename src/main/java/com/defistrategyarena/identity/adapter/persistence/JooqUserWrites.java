package com.defistrategyarena.identity.adapter.persistence;

import static com.defistrategyarena.identity.adapter.persistence.jooq.tables.UserPasswords.USER_PASSWORDS;
import static com.defistrategyarena.identity.adapter.persistence.jooq.tables.Users.USERS;

import com.defistrategyarena.identity.application.DuplicateEmailException;
import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.identity.domain.UserId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

enum JooqUserWrites {
    ;

    private static final int EMPTY_COUNT = 0;

    static void persistUser(PersistUser input) {
        UserRow row = UserRow.from(input.user());
        try {
            int updated =
                    input.dsl()
                            .update(USERS)
                            .set(USERS.EMAIL, row.email())
                            .set(USERS.DISPLAY_NAME, row.displayName())
                            .set(USERS.EMAIL_NORMALIZED, row.emailNormalized())
                            .where(USERS.USER_ID.eq(row.userId()))
                            .execute();
            if (updated > EMPTY_COUNT) {
                return;
            }
            OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
            input.dsl()
                    .insertInto(USERS)
                    .columns(
                            USERS.USER_ID,
                            USERS.EMAIL,
                            USERS.EMAIL_NORMALIZED,
                            USERS.DISPLAY_NAME,
                            USERS.CREATED_AT)
                    .values(
                            row.userId(),
                            row.email(),
                            row.emailNormalized(),
                            row.displayName(),
                            createdAt)
                    .execute();
        } catch (DataAccessException ex) {
            throw mappedPersistFailure(ex);
        }
    }

    static void persistPassword(PersistPassword input) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UUID userId = uuid(input.userId());
        String encoded = input.passwordHash().encoded();
        int updated =
                input.dsl()
                        .update(USER_PASSWORDS)
                        .set(USER_PASSWORDS.PASSWORD_HASH, encoded)
                        .set(USER_PASSWORDS.UPDATED_AT, now)
                        .where(USER_PASSWORDS.USER_ID.eq(userId))
                        .execute();
        if (updated > EMPTY_COUNT) {
            return;
        }
        input.dsl()
                .insertInto(USER_PASSWORDS)
                .set(USER_PASSWORDS.USER_ID, userId)
                .set(USER_PASSWORDS.PASSWORD_HASH, encoded)
                .set(USER_PASSWORDS.UPDATED_AT, now)
                .execute();
    }

    static RuntimeException mappedPersistFailure(DataAccessException ex) {
        if (PostgresUniqueConflict.matches(ex)) {
            return new DuplicateEmailException(ex);
        }
        return ex;
    }

    static User toUser(org.jooq.Record record) {
        return User.rehydrate(
                new User.RehydrateUserData(
                        new UserId(record.get(USERS.USER_ID).toString()),
                        record.get(USERS.EMAIL),
                        record.get(USERS.DISPLAY_NAME)));
    }

    static UUID uuid(UserId id) {
        return UUID.fromString(id.value());
    }

    static String normalize(EmailValue email) {
        return email.value().trim().toLowerCase(Locale.ROOT);
    }

    record EmailValue(String value) {}

    record PersistUser(DSLContext dsl, User user) {}

    record PersistPassword(DSLContext dsl, UserId userId, PasswordHash passwordHash) {}

    private record UserRow(UUID userId, String email, String emailNormalized, String displayName) {
        static UserRow from(User user) {
            return new UserRow(
                    uuid(user.id()),
                    user.email(),
                    normalize(new EmailValue(user.email())),
                    user.displayName());
        }
    }
}
