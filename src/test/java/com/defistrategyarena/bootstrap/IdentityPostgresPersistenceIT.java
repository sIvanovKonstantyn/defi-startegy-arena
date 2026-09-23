package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.defistrategyarena.identity.adapter.web.LoginHttpRequest;
import com.defistrategyarena.identity.adapter.web.SessionHttpResponse;
import com.defistrategyarena.identity.adapter.web.SignupHttpRequest;
import com.defistrategyarena.shared.infra.persistence.JdbcSettings;
import com.defistrategyarena.strategy.integration.H2PostgresModeSupport;
import org.junit.jupiter.api.Test;

class IdentityPostgresPersistenceIT {

    private static final int STATUS_CREATED = 201;
    private static final int STATUS_OK = 200;
    private static final String EMAIL = "persist@test.co";
    private static final String PASSWORD = "secret-value";
    private static final String DISPLAY = "Persist";
    private static final String DB_NAME = "identity_persist_it";

    @Test
    void signup_then_login_uses_postgres_identity_tables() {
        JdbcSettings jdbc =
                H2PostgresModeSupport.jdbcSettings(new H2PostgresModeSupport.DatabaseName(DB_NAME));
        AppConfig config =
                AppConfig.load(
                        AppConfig.LoadRequest.fromEnvironment(
                                key ->
                                        switch (key) {
                                            case "DSA_PERSISTENCE_MODE" -> "postgres";
                                            case "DSA_JDBC_DRIVER" -> jdbc.driver();
                                            case "DSA_JDBC_URL" -> jdbc.url();
                                            case "DSA_JDBC_USER" -> jdbc.user();
                                            case "DSA_JDBC_PASSWORD" -> jdbc.password();
                                            default -> null;
                                        }));
        try (ApplicationComposition composition = ApplicationComposition.create(config)) {
            SessionHttpResponse signup =
                    composition
                            .identityHttp()
                            .signup(new SignupHttpRequest(EMAIL, PASSWORD, DISPLAY));
            assertEquals(STATUS_CREATED, signup.status());
            assertFalse(signup.accessToken().isBlank());

            SessionHttpResponse login =
                    composition.identityHttp().login(new LoginHttpRequest(EMAIL, PASSWORD));
            assertEquals(STATUS_OK, login.status());
            assertFalse(login.accessToken().isBlank());
        }
    }
}
