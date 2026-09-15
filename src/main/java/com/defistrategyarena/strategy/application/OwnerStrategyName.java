package com.defistrategyarena.strategy.application;

public record OwnerStrategyName(String ownerId, String name) {

    public static OwnerStrategyName create(OwnerStrategyName draft) {
        return new OwnerStrategyName(draft.ownerId(), draft.name());
    }
}
