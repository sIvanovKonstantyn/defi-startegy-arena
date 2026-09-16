package com.defistrategyarena.shared.infra.persistence;

public record JdbcSettings(String driver, String url, String user, String password) {

    private static final String DRIVER_REQUIRED = "jdbc driver must not be blank";
    private static final String URL_REQUIRED = "jdbc url must not be blank";
    private static final String USER_REQUIRED = "jdbc user must not be blank";
    private static final String PASSWORD_REQUIRED = "jdbc password must not be null";

    public JdbcSettings {
        if (driver == null || driver.isBlank()) {
            throw new IllegalArgumentException(DRIVER_REQUIRED);
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(URL_REQUIRED);
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException(USER_REQUIRED);
        }
        if (password == null) {
            throw new IllegalArgumentException(PASSWORD_REQUIRED);
        }
    }

    public static JdbcSettings create(JdbcSettings draft) {
        return new JdbcSettings(draft.driver(), draft.url(), draft.user(), draft.password());
    }
}
