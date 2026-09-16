package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.User;
import java.util.Objects;
import java.util.Optional;

public final class LoginWithPassword {

    private static final String COMMAND_REQUIRED = "login command must not be null";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;

    public LoginWithPassword(LoginWithPasswordDeps deps) {
        this.users = deps.users();
        this.passwordHasher = deps.passwordHasher();
        this.sessions = deps.sessions();
    }

    public AuthSessionResult execute(LoginWithPasswordCommand command) {
        Objects.requireNonNull(command, COMMAND_REQUIRED);
        User user = requireUser(command);
        requireMatchingPassword(new PasswordCheck(command.password(), user));
        return sessions.issue(user.id());
    }

    private User requireUser(LoginWithPasswordCommand command) {
        Optional<User> user =
                users.findByEmail(
                        new UserRepository.EmailLookup(
                                EmailNormalizer.normalize(
                                        new EmailNormalizer.EmailText(command.email()))));
        if (user.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        return user.get();
    }

    private void requireMatchingPassword(PasswordCheck check) {
        PasswordHash hash = requirePasswordHash(check.user());
        boolean matches =
                passwordHasher.matches(
                        new PasswordHasher.PasswordMatchRequest(
                                new PasswordHasher.PlainPassword(check.password()), hash));
        if (!matches) {
            throw new InvalidCredentialsException();
        }
    }

    private PasswordHash requirePasswordHash(User user) {
        Optional<PasswordHash> hash = users.findPasswordHash(user.id());
        if (hash.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        return hash.get();
    }

    private record PasswordCheck(String password, User user) {}

    public record LoginWithPasswordDeps(
            UserRepository users, PasswordHasher passwordHasher, SessionIssuer sessions) {}
}
