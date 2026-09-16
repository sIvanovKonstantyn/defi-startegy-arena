package com.defistrategyarena.identity.application;

import java.util.Locale;

enum EmailNormalizer {
    ;

    static String normalize(EmailText email) {
        return email.value().trim().toLowerCase(Locale.ROOT);
    }

    record EmailText(String value) {}
}
