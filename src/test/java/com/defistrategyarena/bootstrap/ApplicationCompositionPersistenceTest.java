package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ApplicationCompositionPersistenceTest {

    @Test
    void create_memory_from_config_matches_default() {
        AppConfig config = AppConfig.load(AppConfig.LoadRequest.fromEnvironment(key -> null));
        try (ApplicationComposition composition = ApplicationComposition.create(config)) {
            assertNotNull(composition.strategies());
            assertNotNull(composition.events());
            assertNotNull(composition.strategyHttp());
            assertNotNull(composition.identityHttp());
        }
    }

    @Test
    void create_default_is_memory() {
        try (ApplicationComposition composition = ApplicationComposition.createDefault()) {
            assertSame(composition.strategies(), composition.strategies());
            assertNotNull(composition.identityHttp());
        }
    }
}