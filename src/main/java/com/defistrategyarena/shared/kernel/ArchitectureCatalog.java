package com.defistrategyarena.shared.kernel;

import java.util.List;

public enum ArchitectureCatalog {
    ;

    public static final String BASE_PACKAGE = "com.defistrategyarena";

    public static final String IDENTITY = "identity";
    public static final String STRATEGY = "strategy";
    public static final String MARKET_DATA = "marketdata";
    public static final String ARENA = "arena";
    public static final String LEADERBOARD = "leaderboard";
    public static final String SHARED = "shared";
    public static final String SHARED_INFRA = "shared.infra";

    public static final List<String> CONTEXT_PACKAGES = List.of(
            IDENTITY,
            STRATEGY,
            MARKET_DATA,
            ARENA,
            LEADERBOARD
    );
}
