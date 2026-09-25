package com.defistrategyarena.strategy.adapter.persistence;

import static com.defistrategyarena.strategy.adapter.persistence.jooq.tables.Indicators.INDICATORS;
import static com.defistrategyarena.strategy.adapter.persistence.jooq.tables.StrategyRuleConditions.STRATEGY_RULE_CONDITIONS;
import static com.defistrategyarena.strategy.adapter.persistence.jooq.tables.StrategyRules.STRATEGY_RULES;

import com.defistrategyarena.strategy.adapter.dsl.StrategyActionWire;
import com.defistrategyarena.strategy.adapter.dsl.StrategyConditionWire;
import com.defistrategyarena.strategy.adapter.dsl.StrategyDslWireMapper;
import com.defistrategyarena.strategy.adapter.dsl.StrategyDslWireNames;
import com.defistrategyarena.strategy.adapter.dsl.StrategyRuleWire;
import com.defistrategyarena.strategy.adapter.persistence.jooq.tables.records.StrategyRuleConditionsRecord;
import com.defistrategyarena.strategy.adapter.persistence.jooq.tables.records.StrategyRulesRecord;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jooq.DSLContext;

final class StrategyGraphStore {

    private static final String UNKNOWN_INDICATOR = "unknown indicator";
    private static final String RULE_GRAPH_REQUIRED = "strategy must have at least one rule";
    private static final String ROOT_REQUIRED = "strategy rule must have a root condition";
    private static final int FIRST_SORT_ORDER = 0;
    private static final String KEY_INSTRUMENT = "instrument";
    private static final String KEY_INSTRUMENT_PAIR = "instrumentPair";
    private static final String KEY_ALLOCATION = "allocationPercent";
    private static final String KEY_YEARLY_FEE = "yearlyFeePercent";

    private final DSLContext dsl;
    private final StrategyConditionWriter writer;
    private final StrategyConditionReader reader;

    StrategyGraphStore(DSLContext dsl) {
        this.dsl = dsl;
        this.writer = new StrategyConditionWriter(dsl);
        this.reader = new StrategyConditionReader(dsl);
    }

    void replaceRules(Strategy strategy) {
        UUID strategyId = UUID.fromString(strategy.id().value());
        dsl.deleteFrom(STRATEGY_RULES).where(STRATEGY_RULES.STRATEGY_ID.eq(strategyId)).execute();
        List<StrategyDefinition.Rule> rules = strategy.current().definition().rules();
        IntStream.range(FIRST_SORT_ORDER, rules.size())
                .mapToObj(index -> new InsertRuleCommand(strategyId, rules.get(index), index))
                .forEach(writer::insertRule);
    }

    StrategyDefinition loadDefinition(LoadDefinitionCommand command) {
        List<StrategyRulesRecord> ruleRows =
                dsl.selectFrom(STRATEGY_RULES)
                        .where(STRATEGY_RULES.STRATEGY_ID.eq(command.strategyId()))
                        .orderBy(STRATEGY_RULES.SORT_ORDER.asc())
                        .fetch();
        if (ruleRows.isEmpty()) {
            throw new IllegalStateException(RULE_GRAPH_REQUIRED);
        }
        List<StrategyDefinition.Rule> rules =
                ruleRows.stream().map(reader::loadRule).toList();
        return StrategyDefinition.create(
                new StrategyDefinition(command.name(), command.description(), List.copyOf(rules)));
    }

    record LoadDefinitionCommand(UUID strategyId, String name, String description) {}

    private record InsertRuleCommand(UUID strategyId, StrategyDefinition.Rule rule, int sortOrder) {}

    private static final class StrategyConditionWriter {
        private final DSLContext dsl;

        private StrategyConditionWriter(DSLContext dsl) {
            this.dsl = dsl;
        }

        private void insertRule(InsertRuleCommand command) {
            UUID ruleRowId = UUID.randomUUID();
            StrategyRuleWire wire = StrategyDslWireMapper.toWire(command.rule());
            Map<String, String> actionParams = actionParams(wire.then());
            dsl.insertInto(STRATEGY_RULES)
                    .set(STRATEGY_RULES.RULE_ROW_ID, ruleRowId)
                    .set(STRATEGY_RULES.STRATEGY_ID, command.strategyId())
                    .set(STRATEGY_RULES.RULE_KEY, command.rule().id())
                    .set(STRATEGY_RULES.SORT_ORDER, command.sortOrder())
                    .set(STRATEGY_RULES.ACTION_TYPE, wire.then().type())
                    .set(
                            STRATEGY_RULES.ACTION_PARAMS_JSON,
                            StrategyJsonMaps.encode(new StrategyJsonMaps.MapPayload(actionParams)))
                    .execute();
            insertCondition(new InsertConditionCommand(ruleRowId, null, wire.when(), FIRST_SORT_ORDER));
        }

        private void insertCondition(InsertConditionCommand command) {
            UUID conditionId = UUID.randomUUID();
            StrategyConditionWire wire = command.wire();
            UUID indicatorId = resolveIndicatorId(wire);
            dsl.insertInto(STRATEGY_RULE_CONDITIONS)
                    .set(STRATEGY_RULE_CONDITIONS.CONDITION_ID, conditionId)
                    .set(STRATEGY_RULE_CONDITIONS.RULE_ROW_ID, command.ruleRowId())
                    .set(STRATEGY_RULE_CONDITIONS.PARENT_CONDITION_ID, command.parentId())
                    .set(STRATEGY_RULE_CONDITIONS.SORT_ORDER, command.sortOrder())
                    .set(STRATEGY_RULE_CONDITIONS.NODE_TYPE, wire.type())
                    .set(STRATEGY_RULE_CONDITIONS.OPERATOR, blankToNull(new TextValue(wire.operator())))
                    .set(STRATEGY_RULE_CONDITIONS.INSTRUMENT, blankToNull(new TextValue(wire.instrument())))
                    .set(STRATEGY_RULE_CONDITIONS.THRESHOLD, blankToNull(new TextValue(wire.threshold())))
                    .set(STRATEGY_RULE_CONDITIONS.INDICATOR_ID, indicatorId)
                    .set(
                            STRATEGY_RULE_CONDITIONS.PARAMETERS_JSON,
                            StrategyJsonMaps.encode(new StrategyJsonMaps.MapPayload(wire.parameters())))
                    .execute();
            List<StrategyConditionWire> children = wire.children();
            IntStream.range(FIRST_SORT_ORDER, children.size())
                    .mapToObj(
                            index ->
                                    new InsertConditionCommand(
                                            command.ruleRowId(), conditionId, children.get(index), index))
                    .forEach(this::insertCondition);
        }

        private UUID resolveIndicatorId(StrategyConditionWire wire) {
            if (!StrategyDslWireNames.CONDITION_INDICATOR_COMPARE.equals(wire.type())) {
                return null;
            }
            UUID indicatorId =
                    dsl.select(INDICATORS.INDICATOR_ID)
                            .from(INDICATORS)
                            .where(INDICATORS.CODE.eq(wire.indicator()))
                            .fetchOne(INDICATORS.INDICATOR_ID);
            if (indicatorId == null) {
                throw new IllegalArgumentException(UNKNOWN_INDICATOR);
            }
            return indicatorId;
        }

        private static Map<String, String> actionParams(StrategyActionWire action) {
            Map<String, String> params = new HashMap<>();
            params.put(KEY_INSTRUMENT, action.instrument());
            params.put(KEY_INSTRUMENT_PAIR, action.instrumentPair());
            params.put(KEY_ALLOCATION, action.allocationPercent());
            params.put(KEY_YEARLY_FEE, action.yearlyFeePercent());
            return Map.copyOf(params);
        }

        private static String blankToNull(TextValue text) {
            if (text.value().isBlank()) {
                return null;
            }
            return text.value();
        }
    }

    private static final class StrategyConditionReader {
        private final DSLContext dsl;

        private StrategyConditionReader(DSLContext dsl) {
            this.dsl = dsl;
        }

        private StrategyDefinition.Rule loadRule(StrategyRulesRecord ruleRow) {
            ConditionForest forest = loadForest(ruleRow.getRuleRowId());
            StrategyConditionWire when =
                    toConditionWire(new ConditionNodeInput(forest.root(), forest.childrenByParent()));
            StrategyActionWire then =
                    toActionWire(
                            new ActionWireInput(ruleRow.getActionType(), ruleRow.getActionParamsJson()));
            return StrategyDslWireMapper.toRule(
                    new StrategyRuleWire(ruleRow.getRuleKey(), when, then));
        }

        private ConditionForest loadForest(UUID ruleRowId) {
            List<StrategyRuleConditionsRecord> conditionRows =
                    dsl.selectFrom(STRATEGY_RULE_CONDITIONS)
                            .where(STRATEGY_RULE_CONDITIONS.RULE_ROW_ID.eq(ruleRowId))
                            .fetch();
            return indexForest(new ForestRows(conditionRows));
        }

        private static ConditionForest indexForest(ForestRows rows) {
            ForestBuild build = ForestBuild.empty();
            rows.conditionRows().stream().map(ConditionRowInput::new).forEach(build::include);
            return build.requireRoot();
        }

        private StrategyConditionWire toConditionWire(ConditionNodeInput input) {
            StrategyRuleConditionsRecord row = input.row();
            List<StrategyRuleConditionsRecord> sorted = sortedChildren(input);
            List<StrategyConditionWire> children =
                    sorted.stream()
                            .map(child -> toConditionWire(new ConditionNodeInput(child, input.childrenByParent())))
                            .toList();
            Map<String, String> parameters =
                    StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload(row.getParametersJson()));
            return new StrategyConditionWire(
                    row.getNodeType(),
                    children,
                    nullToEmpty(new TextValue(row.getInstrument())),
                    indicatorCode(row.getIndicatorId()),
                    nullToEmpty(new TextValue(row.getOperator())),
                    nullToEmpty(new TextValue(row.getThreshold())),
                    parameters);
        }

        private static List<StrategyRuleConditionsRecord> sortedChildren(ConditionNodeInput input) {
            List<StrategyRuleConditionsRecord> childRows =
                    input.childrenByParent().getOrDefault(input.row().getConditionId(), List.of());
            List<StrategyRuleConditionsRecord> sorted = new ArrayList<>(childRows);
            sorted.sort(Comparator.comparingInt(StrategyRuleConditionsRecord::getSortOrder));
            return sorted;
        }

        private StrategyActionWire toActionWire(ActionWireInput input) {
            Map<String, String> params =
                    StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload(input.paramsJson()));
            return new StrategyActionWire(
                    input.actionType(),
                    params.getOrDefault(KEY_INSTRUMENT, StrategyDslWireNames.EMPTY),
                    params.getOrDefault(KEY_INSTRUMENT_PAIR, StrategyDslWireNames.EMPTY),
                    params.getOrDefault(KEY_ALLOCATION, StrategyDslWireNames.EMPTY),
                    params.getOrDefault(KEY_YEARLY_FEE, StrategyDslWireNames.EMPTY));
        }

        private String indicatorCode(UUID indicatorId) {
            if (indicatorId == null) {
                return StrategyDslWireNames.EMPTY;
            }
            String code =
                    dsl.select(INDICATORS.CODE)
                            .from(INDICATORS)
                            .where(INDICATORS.INDICATOR_ID.eq(indicatorId))
                            .fetchOne(INDICATORS.CODE);
            return code == null ? StrategyDslWireNames.EMPTY : code;
        }

        private static String nullToEmpty(TextValue text) {
            return text.value() == null ? StrategyDslWireNames.EMPTY : text.value();
        }
    }

    private static List<StrategyRuleConditionsRecord> newChildList(UUID ignored) {
        return new ArrayList<>();
    }

    private record InsertConditionCommand(
            UUID ruleRowId, UUID parentId, StrategyConditionWire wire, int sortOrder) {}

    private record ConditionNodeInput(
            StrategyRuleConditionsRecord row,
            Map<UUID, List<StrategyRuleConditionsRecord>> childrenByParent) {}

    private record ActionWireInput(String actionType, String paramsJson) {}

    private record TextValue(String value) {}

    private record ConditionForest(
            StrategyRuleConditionsRecord root,
            Map<UUID, List<StrategyRuleConditionsRecord>> childrenByParent) {}

    private record ForestRows(List<StrategyRuleConditionsRecord> conditionRows) {}

    private static final class ForestBuild {
        private Optional<StrategyRuleConditionsRecord> root = Optional.empty();
        private final Map<UUID, List<StrategyRuleConditionsRecord>> childrenByParent = new HashMap<>();

        private static ForestBuild empty() {
            return new ForestBuild();
        }

        private void include(ConditionRowInput input) {
            StrategyRuleConditionsRecord row = input.row();
            UUID parentId = row.getParentConditionId();
            if (parentId == null) {
                root = Optional.of(row);
                return;
            }
            childrenByParent.computeIfAbsent(parentId, StrategyGraphStore::newChildList).add(row);
        }

        private ConditionForest requireRoot() {
            StrategyRuleConditionsRecord resolved =
                    root.orElseThrow(() -> new IllegalStateException(ROOT_REQUIRED));
            return new ConditionForest(resolved, childrenByParent);
        }
    }

    private record ConditionRowInput(StrategyRuleConditionsRecord row) {}
}
