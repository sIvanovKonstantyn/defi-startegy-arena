package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.http.HttpServerBootstrap;
import com.defistrategyarena.shared.infra.http.HttpServerConfig;

public record ApplicationStartCommand(HttpServerBootstrap bootstrap, HttpServerConfig config) {

    public static ApplicationStartCommand create(ApplicationStartCommand draft) {
        return new ApplicationStartCommand(draft.bootstrap(), draft.config());
    }
}
