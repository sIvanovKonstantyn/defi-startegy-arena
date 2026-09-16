package com.defistrategyarena.identity.application;

import java.util.Objects;

public record RegisterWithPasswordCommand(String email, String password, String displayName) {

    private static final String DISPLAY_NAME_REQUIRED = "display name must not be blank";
    private static final String DRAFT_REQUIRED = "register command must not be null";

    public RegisterWithPasswordCommand {
        email = CredentialText.requireEmail(new CredentialText.EmailText(email));
        password = CredentialText.requirePassword(new CredentialText.PasswordText(password));
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(DISPLAY_NAME_REQUIRED);
        }
    }

    public static RegisterWithPasswordCommand create(RegisterWithPasswordCommand draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new RegisterWithPasswordCommand(draft.email(), draft.password(), draft.displayName());
    }
}
