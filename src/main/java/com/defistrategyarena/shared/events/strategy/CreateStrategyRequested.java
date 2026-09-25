package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.List;
import java.util.Map;

public record CreateStrategyRequested(
        String correlationId,
        String ownerId,
        String name,
        String description,
        List<RulePayload> rules)
        implements DomainEvent {

    public CreateStrategyRequested {
        rules = List.copyOf(rules);
        if (description == null) {
            description = "";
        }
    }

    public record RulePayload(String id, ConditionPayload when, ActionPayload then) {}

    public record ConditionPayload(
            String type,
            List<ConditionPayload> children,
            String instrument,
            String indicator,
            String operator,
            String threshold,
            Map<String, String> parameters) {

        public ConditionPayload {
            children = children == null ? List.of() : List.copyOf(children);
            parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
            instrument = instrument == null ? "" : instrument;
            indicator = indicator == null ? "" : indicator;
            operator = operator == null ? "" : operator;
            threshold = threshold == null ? "" : threshold;
            type = type == null ? "" : type;
        }
    }

    public record ActionPayload(
            String type,
            String instrument,
            String instrumentPair,
            String allocationPercent,
            String yearlyFeePercent) {

        public ActionPayload {
            type = type == null ? "" : type;
            instrument = instrument == null ? "" : instrument;
            instrumentPair = instrumentPair == null ? "" : instrumentPair;
            allocationPercent = allocationPercent == null ? "" : allocationPercent;
            yearlyFeePercent = yearlyFeePercent == null ? "" : yearlyFeePercent;
        }
    }
}
