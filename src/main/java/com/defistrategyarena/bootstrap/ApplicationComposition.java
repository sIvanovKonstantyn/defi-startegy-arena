package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.shared.messaging.InMemoryDomainEventPublisher;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.StrategyRepository;

public record ApplicationComposition(
        StrategyRepository strategies, DomainEventPublisher events, StrategyRestAdapter strategyHttp) {

    public static ApplicationComposition createDefault() {
        StrategyRepository strategies = new InMemoryStrategyRepository();
        DomainEventPublisher events = new InMemoryDomainEventPublisher();
        CreateStrategy createStrategy =
                new CreateStrategy(new CreateStrategy.CreateStrategyDeps(strategies, events));
        StrategyRestAdapter strategyHttp =
                new StrategyRestAdapter(new StrategyRestAdapter.StrategyRestAdapterDeps(createStrategy));
        return new ApplicationComposition(strategies, events, strategyHttp);
    }
}
