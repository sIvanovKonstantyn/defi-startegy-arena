package com.defistrategyarena.strategy.adapter.web;

public record StrategySummaryHttpResponse(
        String strategyId, String name, String description, String privacy, int versionNumber) {

    public static StrategySummaryHttpResponse create(StrategySummaryHttpResponse draft) {
        return new StrategySummaryHttpResponse(
                draft.strategyId(),
                draft.name(),
                draft.description(),
                draft.privacy(),
                draft.versionNumber());
    }
}
