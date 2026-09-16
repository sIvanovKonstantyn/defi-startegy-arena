package com.defistrategyarena.identity.domain;

enum NonBlankText {
    ;

    static String require(TextRequirement requirement) {
        if (requirement.value() == null || requirement.value().isBlank()) {
            throw new IllegalArgumentException(requirement.message());
        }
        return requirement.value().trim();
    }

    record TextRequirement(String value, String message) {}
}
