package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.PasswordHash;
import com.defistrategyarena.identity.domain.User;
import java.util.Objects;
import java.util.Optional;

public final class RegisterWithPassword {

    private static final String COMMAND_REQUIRED = "register command must not be null";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;

    public RegisterWithPassword(RegisterWithPasswordDeps deps) {
        this.users = deps.users();
        this.passwordHasher = deps.passwordHasher();
        this.sessions = deps.sessions();
    }

    public AuthSessionResult execute(RegisterWithPasswordCommand command) {
        Objects.requireNonNull(command, COMMAND_REQUIRED);
        Optional<User> existing =
                users.findByEmail(
                        new UserRepository.EmailLookup(
                                EmailNormalizer.normalize(new EmailNormalizer.EmailText(command.email()))));
        if (existing.isPresent()) {
            throw new DuplicateEmailException();
        }
        User user =
                User.create(new User.CreateUserData(command.email(), command.displayName()));
        PasswordHash hash =
                passwordHasher.hash(new PasswordHasher.PlainPassword(command.password()));
        users.saveWithPassword(new UserRepository.SaveUserWithPasswordCommand(user, hash));
        return sessions.issue(user.id());
    }

    public record RegisterWithPasswordDeps(
            UserRepository users, PasswordHasher passwordHasher, SessionIssuer sessions) {}
}
