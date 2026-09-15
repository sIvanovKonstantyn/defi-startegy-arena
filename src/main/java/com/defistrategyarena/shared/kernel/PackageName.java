package com.defistrategyarena.shared.kernel;

public record PackageName(String value) {

    private static final String MUST_NOT_BE_BLANK = "package name must not be blank";
    private static final char PACKAGE_SEPARATOR = '.';

    public PackageName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MUST_NOT_BE_BLANK);
        }
    }

    public String qualifiedUnder(PackageName basePackage) {
        return basePackage.value() + PACKAGE_SEPARATOR + value;
    }
}
