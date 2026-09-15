package com.defistrategyarena.shared.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.kernel.ArchitectureCatalog;
import com.defistrategyarena.shared.kernel.PackageName;
import org.junit.jupiter.api.Test;

class PackageNameTest {

    @Test
    void creates_package_name_and_qualifies_under_base() {
        PackageName base = new PackageName(ArchitectureCatalog.BASE_PACKAGE);
        PackageName name = new PackageName(ArchitectureCatalog.STRATEGY);
        assertEquals(ArchitectureCatalog.STRATEGY, name.value());
        assertEquals(
                ArchitectureCatalog.BASE_PACKAGE + '.' + ArchitectureCatalog.STRATEGY,
                name.qualifiedUnder(base));
    }

    @Test
    void rejects_null_package_name() {
        assertThrows(IllegalArgumentException.class, () -> new PackageName(null));
    }

    @Test
    void rejects_blank_package_name() {
        assertThrows(IllegalArgumentException.class, () -> new PackageName(" "));
    }
}
