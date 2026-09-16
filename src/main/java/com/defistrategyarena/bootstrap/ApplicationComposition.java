package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.shared.messaging.InMemoryDomainEventPublisher;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.defistrategyarena.strategy.application.UpdateStrategy;

public record ApplicationComposition(
        StrategyRepository strategies, DomainEventPublisher events, StrategyRestAdapter strategyHttp) {

    public static ApplicationComposition createDefault() {
        StrategyRepository strategies = new InMemoryStrategyRepository();
        DomainEventPublisher events = new InMemoryDomainEventPublisher();
        CreateStrategy createStrategy =
                new CreateStrategy(new CreateStrategy.CreateStrategyDeps(strategies, events));
        ListStrategies listStrategies =
                new ListStrategies(new ListStrategies.ListStrategiesDeps(strategies));
        GetStrategy getStrategy = new GetStrategy(new GetStrategy.GetStrategyDeps(strategies));
        UpdateStrategy updateStrategy =
                new UpdateStrategy(new UpdateStrategy.UpdateStrategyDeps(strategies, events));
        DeleteStrategy deleteStrategy =
                new DeleteStrategy(new DeleteStrategy.DeleteStrategyDeps(strategies));
        StrategyRestAdapter strategyHttp =
                new StrategyRestAdapter(
                        new StrategyRestAdapter.StrategyRestAdapterDeps(
                                createStrategy,
                                listStrategies,
                                getStrategy,
                                updateStrategy,
                                deleteStrategy));
        return new ApplicationComposition(strategies, events, strategyHttp);
    }
}
