package com.defistrategyarena.shared.indicators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class IndicatorCatalog {

    private static final String UNKNOWN_INDICATOR = "unknown indicator";
    private static final String PERIOD = "period";
    private static final String STD_DEV = "stdDev";
    private static final String FAST = "fast";
    private static final String SLOW = "slow";
    private static final String SIGNAL = "signal";
    private static final String DEFAULT_PERIOD = "14";
    private static final String DEFAULT_STD_DEV = "2";
    private static final String DEFAULT_FAST = "12";
    private static final String DEFAULT_SLOW = "26";
    private static final String DEFAULT_SIGNAL = "9";
    private static final String CODE_SMA = "sma";
    private static final String CODE_EMA = "ema";
    private static final String CODE_RSI = "rsi";
    private static final String CODE_BOLLINGER = "bollinger_bands";
    private static final String CODE_MACD = "macd";
    private static final String NAME_SMA = "SMA";
    private static final String NAME_EMA = "EMA";
    private static final String NAME_RSI = "RSI";
    private static final String NAME_BOLLINGER = "Bollinger Bands";
    private static final String NAME_MACD = "MACD";
    private static final String MISSING_PARAM_PREFIX = "missing required indicator parameter: ";
    private static final String PARAMETERS_REQUIRED = "parameters must not be null";

    private static final Map<String, IndicatorDefinition> BY_CODE = buildCatalog();

    private IndicatorCatalog() {}

    public static List<IndicatorDefinition> all() {
        return List.copyOf(BY_CODE.values());
    }

    public static IndicatorDefinition require(IndicatorId id) {
        IndicatorDefinition definition = BY_CODE.get(id.value());
        if (definition == null) {
            throw new IllegalArgumentException(UNKNOWN_INDICATOR);
        }
        return definition;
    }

    public static Optional<IndicatorDefinition> find(IndicatorId id) {
        return Optional.ofNullable(BY_CODE.get(id.value()));
    }

    public static void validateParameters(ValidateParametersCommand command) {
        IndicatorDefinition definition = require(command.indicatorId());
        definition.parameters().stream()
                .map(spec -> new SpecParams(spec, command.parameters()))
                .map(IndicatorCatalog::checkFor)
                .forEach(IndicatorCatalog::requirePresent);
    }

    private static RequiredParamCheck checkFor(SpecParams input) {
        return new RequiredParamCheck(input.spec(), input.parameters());
    }

    private static void requirePresent(RequiredParamCheck check) {
        if (isMissingRequired(check)) {
            throw new IllegalArgumentException(MISSING_PARAM_PREFIX + check.spec().name());
        }
    }

    private static boolean isMissingRequired(RequiredParamCheck check) {
        String value = check.parameters().get(check.spec().name());
        return value == null || value.isBlank();
    }

    public record ValidateParametersCommand(IndicatorId indicatorId, Map<String, String> parameters) {
        public ValidateParametersCommand {
            if (parameters == null) {
                throw new IllegalArgumentException(PARAMETERS_REQUIRED);
            }
            parameters = Map.copyOf(parameters);
        }
    }

    private static Map<String, IndicatorDefinition> buildCatalog() {
        Map<String, IndicatorDefinition> catalog = new LinkedHashMap<>();
        put(new CatalogPut(catalog, periodIndicator(new PeriodIndicatorDraft(CODE_SMA, NAME_SMA))));
        put(new CatalogPut(catalog, periodIndicator(new PeriodIndicatorDraft(CODE_EMA, NAME_EMA))));
        put(new CatalogPut(catalog, periodIndicator(new PeriodIndicatorDraft(CODE_RSI, NAME_RSI))));
        put(
                new CatalogPut(
                        catalog,
                        IndicatorDefinition.create(
                                new IndicatorDefinition(
                                        new IndicatorId(CODE_BOLLINGER),
                                        NAME_BOLLINGER,
                                        List.of(
                                                typedParam(
                                                        new TypedParamDraft(
                                                                PERIOD,
                                                                IndicatorParameterSpec.ParameterType.INTEGER,
                                                                DEFAULT_PERIOD)),
                                                typedParam(
                                                        new TypedParamDraft(
                                                                STD_DEV,
                                                                IndicatorParameterSpec.ParameterType.DECIMAL,
                                                                DEFAULT_STD_DEV)))))));
        put(
                new CatalogPut(
                        catalog,
                        IndicatorDefinition.create(
                                new IndicatorDefinition(
                                        new IndicatorId(CODE_MACD),
                                        NAME_MACD,
                                        List.of(
                                                typedParam(
                                                        new TypedParamDraft(
                                                                FAST,
                                                                IndicatorParameterSpec.ParameterType.INTEGER,
                                                                DEFAULT_FAST)),
                                                typedParam(
                                                        new TypedParamDraft(
                                                                SLOW,
                                                                IndicatorParameterSpec.ParameterType.INTEGER,
                                                                DEFAULT_SLOW)),
                                                typedParam(
                                                        new TypedParamDraft(
                                                                SIGNAL,
                                                                IndicatorParameterSpec.ParameterType.INTEGER,
                                                                DEFAULT_SIGNAL)))))));
        return Map.copyOf(catalog);
    }

    private static void put(CatalogPut command) {
        command.catalog().put(command.definition().id().value(), command.definition());
    }

    private static IndicatorDefinition periodIndicator(PeriodIndicatorDraft draft) {
        return IndicatorDefinition.create(
                new IndicatorDefinition(
                        new IndicatorId(draft.code()),
                        draft.displayName(),
                        List.of(
                                typedParam(
                                        new TypedParamDraft(
                                                PERIOD,
                                                IndicatorParameterSpec.ParameterType.INTEGER,
                                                DEFAULT_PERIOD)))));
    }

    private static IndicatorParameterSpec typedParam(TypedParamDraft draft) {
        return IndicatorParameterSpec.create(
                new IndicatorParameterSpec(draft.name(), draft.type(), true, draft.defaultValue()));
    }

    private record RequiredParamCheck(IndicatorParameterSpec spec, Map<String, String> parameters) {}

    private record SpecParams(IndicatorParameterSpec spec, Map<String, String> parameters) {}

    private record CatalogPut(Map<String, IndicatorDefinition> catalog, IndicatorDefinition definition) {}

    private record PeriodIndicatorDraft(String code, String displayName) {}

    private record TypedParamDraft(
            String name, IndicatorParameterSpec.ParameterType type, String defaultValue) {}
}
