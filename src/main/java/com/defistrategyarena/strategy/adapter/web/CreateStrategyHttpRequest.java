package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CreateStrategyHttpRequest(String name, String description, List<RuleBody> rules) {

    private static final String RULES_REQUIRED = "rules must not be null";
    private static final String EMPTY = "";

    public CreateStrategyHttpRequest {
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
        if (description == null) {
            description = EMPTY;
        }
    }

    public static CreateStrategyHttpRequest create(CreateStrategyHttpRequest draft) {
        return new CreateStrategyHttpRequest(draft.name(), draft.description(), draft.rules());
    }

    public record RuleBody(String id, ConditionBody when, ActionBody then) {

        public static RuleBody fromEvent(CreateStrategyRequested.RulePayload payload) {
            return new RuleBody(
                    payload.id(),
                    ConditionBody.fromEvent(payload.when()),
                    ActionBody.fromEvent(payload.then()));
        }

        public CreateStrategyRequested.RulePayload toEvent() {
            return new CreateStrategyRequested.RulePayload(id(), when().toEvent(), then().toEvent());
        }
    }

    public record ConditionBody(
            String type,
            List<ConditionBody> children,
            String instrument,
            String indicator,
            String operator,
            String threshold,
            Map<String, String> parameters) {

        public ConditionBody {
            children = children == null ? List.of() : List.copyOf(children);
            parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
            type = type == null ? EMPTY : type;
            instrument = instrument == null ? EMPTY : instrument;
            indicator = indicator == null ? EMPTY : indicator;
            operator = operator == null ? EMPTY : operator;
            threshold = threshold == null ? EMPTY : threshold;
        }

        static ConditionBody fromEvent(CreateStrategyRequested.ConditionPayload payload) {
            List<ConditionBody> nested = new ArrayList<>();
            for (CreateStrategyRequested.ConditionPayload child : payload.children()) {
                nested.add(fromEvent(child));
            }
            return new ConditionBody(
                    payload.type(),
                    nested,
                    payload.instrument(),
                    payload.indicator(),
                    payload.operator(),
                    payload.threshold(),
                    payload.parameters());
        }

        CreateStrategyRequested.ConditionPayload toEvent() {
            List<CreateStrategyRequested.ConditionPayload> nested = new ArrayList<>();
            for (ConditionBody child : children()) {
                nested.add(child.toEvent());
            }
            return new CreateStrategyRequested.ConditionPayload(
                    type(),
                    nested,
                    instrument(),
                    indicator(),
                    operator(),
                    threshold(),
                    parameters());
        }
    }

    public record ActionBody(
            String type,
            String instrument,
            String instrumentPair,
            String allocationPercent,
            String yearlyFeePercent) {

        public ActionBody {
            type = type == null ? EMPTY : type;
            instrument = instrument == null ? EMPTY : instrument;
            instrumentPair = instrumentPair == null ? EMPTY : instrumentPair;
            allocationPercent = allocationPercent == null ? EMPTY : allocationPercent;
            yearlyFeePercent = yearlyFeePercent == null ? EMPTY : yearlyFeePercent;
        }

        static ActionBody fromEvent(CreateStrategyRequested.ActionPayload payload) {
            return new ActionBody(
                    payload.type(),
                    payload.instrument(),
                    payload.instrumentPair(),
                    payload.allocationPercent(),
                    payload.yearlyFeePercent());
        }

        CreateStrategyRequested.ActionPayload toEvent() {
            return new CreateStrategyRequested.ActionPayload(
                    type(), instrument(), instrumentPair(), allocationPercent(), yearlyFeePercent());
        }
    }
}
