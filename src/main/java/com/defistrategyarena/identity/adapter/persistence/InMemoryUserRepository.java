package com.defistrategyarena.identity.adapter.persistence;

import com.defistrategyarena.identity.application.DuplicateEmailException;
import com.defistrategyarena.identity.application.UserRepository;
import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.identity.domain.UserId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> byId = new ConcurrentHashMap<>();
    private final Map<String, String> emailIndex = new ConcurrentHashMap<>();
    private final Map<String, PasswordHash> passwords = new ConcurrentHashMap<>();

    @Override
    public void saveWithPassword(SaveUserWithPasswordCommand command) {
        save(command.user());
        passwords.put(command.user().id().value(), command.passwordHash());
    }

    @Override
    public void save(User user) {
        String emailKey = normalize(new EmailValue(user.email()));
        String existingId = emailIndex.putIfAbsent(emailKey, user.id().value());
        if (existingId != null && !existingId.equals(user.id().value())) {
            throw new DuplicateEmailException();
        }
        emailIndex.put(emailKey, user.id().value());
        byId.put(user.id().value(), user);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(byId.get(id.value()));
    }

    @Override
    public Optional<User> findByEmail(EmailLookup lookup) {
        String userId = emailIndex.get(normalize(new EmailValue(lookup.email())));
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(userId));
    }

    @Override
    public Optional<PasswordHash> findPasswordHash(UserId id) {
        return Optional.ofNullable(passwords.get(id.value()));
    }

    @Override
    public int size() {
        return byId.size();
    }

    private static String normalize(EmailValue email) {
        return email.value().trim().toLowerCase(Locale.ROOT);
    }

    private record EmailValue(String value) {}
}
