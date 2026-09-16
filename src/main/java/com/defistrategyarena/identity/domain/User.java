package com.defistrategyarena.identity.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class User {

    private static final String DATA_REQUIRED = "create user data must not be null";
    private static final String REHYDRATE_REQUIRED = "rehydrate user data must not be null";
    private static final String EMAIL_REQUIRED = "email must not be blank";
    private static final String DISPLAY_NAME_REQUIRED = "display name must not be blank";

    private final UserId id;
    private final String email;
    private final String displayName;

    private User(UserId id, String email, String displayName) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
    }

    public static User create(CreateUserData data) {
        Objects.requireNonNull(data, DATA_REQUIRED);
        ProfileFields fields = profileFields(new ProfileInput(data.email(), data.displayName()));
        UserId id =
                UserId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(
                                        List.of(normalizeEmail(new EmailValue(fields.email()))))));
        return new User(id, fields.email(), fields.displayName());
    }

    public static User rehydrate(RehydrateUserData data) {
        Objects.requireNonNull(data, REHYDRATE_REQUIRED);
        ProfileFields fields = profileFields(new ProfileInput(data.email(), data.displayName()));
        return new User(data.id(), fields.email(), fields.displayName());
    }

    public UserId id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public String normalizedEmail() {
        return normalizeEmail(new EmailValue(email));
    }

    private static ProfileFields profileFields(ProfileInput input) {
        return new ProfileFields(
                NonBlankText.require(new NonBlankText.TextRequirement(input.email(), EMAIL_REQUIRED)),
                NonBlankText.require(
                        new NonBlankText.TextRequirement(
                                input.displayName(), DISPLAY_NAME_REQUIRED)));
    }

    private static String normalizeEmail(EmailValue email) {
        return email.value().trim().toLowerCase(Locale.ROOT);
    }

    private record EmailValue(String value) {}

    private record ProfileInput(String email, String displayName) {}

    private record ProfileFields(String email, String displayName) {}

    public record CreateUserData(String email, String displayName) {}

    public record RehydrateUserData(UserId id, String email, String displayName) {}
}
