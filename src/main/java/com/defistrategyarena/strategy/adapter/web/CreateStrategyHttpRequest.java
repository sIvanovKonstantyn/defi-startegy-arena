package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import java.util.List;

public record CreateStrategyHttpRequest(String name, List<RuleBody> rules) {

    private static final String RULES_REQUIRED = "rules must not be null";

    public CreateStrategyHttpRequest {
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
    }

    public static CreateStrategyHttpRequest create(CreateStrategyHttpRequest draft) {
        return new CreateStrategyHttpRequest(draft.name(), draft.rules());
    }

    public record RuleBody(
            String id,
            String conditionType,
            String actionType,
            String instrument,
            String indicator,
            String threshold,
            String allocationPercent) {

        public static RuleBody fromEvent(CreateStrategyRequested.RulePayload payload) {
            return new RuleBody(
                    payload.id(),
                    payload.conditionType(),
                    payload.actionType(),
                    payload.instrument(),
                    payload.indicator(),
                    payload.threshold(),
                    payload.allocationPercent());
        }

        public CreateStrategyRequested.RulePayload toEvent() {
            return new CreateStrategyRequested.RulePayload(
                    id(),
                    conditionType(),
                    actionType(),
                    instrument(),
                    indicator(),
                    threshold(),
                    allocationPercent());
        }
    }
}
