package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.identity.domain.UserId;
import java.util.Optional;

public interface UserRepository {

    void saveWithPassword(SaveUserWithPasswordCommand command);

    void save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(EmailLookup lookup);

    Optional<PasswordHash> findPasswordHash(UserId id);

    int size();

    record SaveUserWithPasswordCommand(User user, PasswordHash passwordHash) {}

    record EmailLookup(String email) {}
}
