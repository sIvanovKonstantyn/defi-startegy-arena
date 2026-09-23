package com.defistrategyarena.identity.adapter.persistence;

import static com.defistrategyarena.identity.adapter.persistence.jooq.tables.UserPasswords.USER_PASSWORDS;
import static com.defistrategyarena.identity.adapter.persistence.jooq.tables.Users.USERS;

import com.defistrategyarena.identity.application.UserRepository;
import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.identity.domain.UserId;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

public final class JooqUserRepository implements UserRepository {

    private static final String DSL_REQUIRED = "dsl context must not be null";

    private final DSLContext dsl;

    public JooqUserRepository(DSLContext dsl) {
        if (dsl == null) {
            throw new IllegalArgumentException(DSL_REQUIRED);
        }
        this.dsl = dsl;
    }

    @Override
    public void saveWithPassword(SaveUserWithPasswordCommand command) {
        dsl.transaction(
                configuration -> {
                    DSLContext tx = DSL.using(configuration);
                    JooqUserWrites.persistUser(new JooqUserWrites.PersistUser(tx, command.user()));
                    JooqUserWrites.persistPassword(
                            new JooqUserWrites.PersistPassword(
                                    tx, command.user().id(), command.passwordHash()));
                });
    }

    @Override
    public void save(User user) {
        JooqUserWrites.persistUser(new JooqUserWrites.PersistUser(dsl, user));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return dsl.selectFrom(USERS)
                .where(USERS.USER_ID.eq(JooqUserWrites.uuid(id)))
                .fetchOptional()
                .map(JooqUserWrites::toUser);
    }

    @Override
    public Optional<User> findByEmail(EmailLookup lookup) {
        return dsl.selectFrom(USERS)
                .where(
                        USERS.EMAIL_NORMALIZED.eq(
                                JooqUserWrites.normalize(
                                        new JooqUserWrites.EmailValue(lookup.email()))))
                .fetchOptional()
                .map(JooqUserWrites::toUser);
    }

    @Override
    public Optional<PasswordHash> findPasswordHash(UserId id) {
        return dsl.selectFrom(USER_PASSWORDS)
                .where(USER_PASSWORDS.USER_ID.eq(JooqUserWrites.uuid(id)))
                .fetchOptional()
                .map(record -> new PasswordHash(record.get(USER_PASSWORDS.PASSWORD_HASH)));
    }

    @Override
    public int size() {
        return dsl.fetchCount(USERS);
    }
}
