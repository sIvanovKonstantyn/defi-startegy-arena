package com.defistrategyarena.strategy.adapter.persistence;

import static com.defistrategyarena.strategy.adapter.persistence.jooq.tables.Strategies.STRATEGIES;

import com.defistrategyarena.strategy.domain.Privacy;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import org.jooq.Record;

enum JooqStrategyRowMapper {
    ;

    static Strategy toStrategy(RowInput input) {
        StrategyDefinition definition =
                input.graphStore()
                        .loadDefinition(
                                new StrategyGraphStore.LoadDefinitionCommand(
                                        input.record().get(STRATEGIES.STRATEGY_ID),
                                        input.record().get(STRATEGIES.NAME),
                                        input.record().get(STRATEGIES.DESCRIPTION)));
        return Strategy.rehydrate(
                new Strategy.RehydrateData(
                        new StrategyId(input.record().get(STRATEGIES.STRATEGY_ID).toString()),
                        input.record().get(STRATEGIES.OWNER_ID),
                        Privacy.valueOf(input.record().get(STRATEGIES.PRIVACY)),
                        input.record().get(STRATEGIES.VERSION_NUMBER),
                        definition));
    }

    record RowInput(Record record, StrategyGraphStore graphStore) {}
}
