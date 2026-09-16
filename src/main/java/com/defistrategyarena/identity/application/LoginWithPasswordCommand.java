package com.defistrategyarena.identity.application;

import java.util.Objects;

public record LoginWithPasswordCommand(String email, String password) {

    private static final String DRAFT_REQUIRED = "login command must not be null";

    public LoginWithPasswordCommand {
        email = CredentialText.requireEmail(new CredentialText.EmailText(email));
        password = CredentialText.requirePassword(new CredentialText.PasswordText(password));
    }

    public static LoginWithPasswordCommand create(LoginWithPasswordCommand draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new LoginWithPasswordCommand(draft.email(), draft.password());
    }
}
