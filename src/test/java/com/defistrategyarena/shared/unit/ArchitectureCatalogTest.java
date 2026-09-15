package com.defistrategyarena.shared.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.kernel.ArchitectureCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureCatalogTest {

    @Test
    void exposes_context_package_catalog() {
        assertEquals("com.defistrategyarena", ArchitectureCatalog.BASE_PACKAGE);
        assertEquals(
                List.of(
                        ArchitectureCatalog.IDENTITY,
                        ArchitectureCatalog.STRATEGY,
                        ArchitectureCatalog.MARKET_DATA,
                        ArchitectureCatalog.ARENA,
                        ArchitectureCatalog.LEADERBOARD),
                ArchitectureCatalog.CONTEXT_PACKAGES);
        assertEquals("shared", ArchitectureCatalog.SHARED);
        assertEquals(0, ArchitectureCatalog.values().length);
        assertThrows(IllegalArgumentException.class, () -> ArchitectureCatalog.valueOf("missing"));
    }
}
