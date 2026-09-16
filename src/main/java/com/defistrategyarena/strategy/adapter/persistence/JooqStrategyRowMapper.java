package com.defistrategyarena.strategy.adapter.persistence;

import static com.defistrategyarena.strategy.adapter.persistence.jooq.tables.Strategies.STRATEGIES;

import com.defistrategyarena.strategy.domain.Privacy;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import org.jooq.Record;

enum JooqStrategyRowMapper {
    ;

    static Strategy toStrategy(Record record) {
        StrategyDefinition definition =
                StrategyDefinitionJsonCodec.decode(
                        new StrategyDefinitionJsonCodec.JsonPayload(
                                record.get(STRATEGIES.DEFINITION_JSON)));
        return Strategy.rehydrate(
                new Strategy.RehydrateData(
                        new StrategyId(record.get(STRATEGIES.STRATEGY_ID).toString()),
                        record.get(STRATEGIES.OWNER_ID),
                        Privacy.valueOf(record.get(STRATEGIES.PRIVACY)),
                        record.get(STRATEGIES.VERSION_NUMBER),
                        definition));
    }
}
