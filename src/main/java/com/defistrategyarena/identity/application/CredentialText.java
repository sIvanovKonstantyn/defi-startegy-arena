package com.defistrategyarena.identity.application;

enum CredentialText {
    ;

    private static final String EMAIL_REQUIRED = "email must not be blank";
    private static final String PASSWORD_REQUIRED = "password must not be blank";

    static String requireEmail(EmailText text) {
        return requireNonBlank(new CredentialField(text.value(), EMAIL_REQUIRED));
    }

    static String requirePassword(PasswordText text) {
        return requireNonBlank(new CredentialField(text.value(), PASSWORD_REQUIRED));
    }

    private static String requireNonBlank(CredentialField field) {
        if (field.value() == null || field.value().isBlank()) {
            throw new IllegalArgumentException(field.message());
        }
        return field.value();
    }

    record EmailText(String value) {}

    record PasswordText(String value) {}

    private record CredentialField(String value, String message) {}
}
