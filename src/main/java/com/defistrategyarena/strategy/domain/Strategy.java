package com.defistrategyarena.strategy.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import java.util.List;
import java.util.Objects;

public final class Strategy {

    private static final String OWNER_REQUIRED = "owner id must not be blank";
    private static final String DATA_REQUIRED = "create strategy data must not be null";
    private static final String PUBLISH_DATA_REQUIRED = "publish new version data must not be null";

    private final StrategyId id;
    private final String ownerId;
    private final Privacy privacy;
    private final StrategyVersion current;

    private Strategy(StrategyId id, String ownerId, StrategyDefinition definition) {
        this(id, ownerId, Privacy.PRIVATE, StrategyVersion.initial(definition));
    }

    private Strategy(StrategyId id, String ownerId, Privacy privacy, StrategyVersion current) {
        this.id = id;
        this.ownerId = ownerId;
        this.privacy = privacy;
        this.current = current;
    }

    public static Strategy create(CreateStrategyData data) {
        Objects.requireNonNull(data, DATA_REQUIRED);
        if (data.ownerId() == null || data.ownerId().isBlank()) {
            throw new IllegalArgumentException(OWNER_REQUIRED);
        }
        StrategyDefinition definition = StrategyDefinition.create(data.definition());
        StrategyId id =
                StrategyId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(
                                        List.of(data.ownerId(), definition.name()))));
        return new Strategy(id, data.ownerId(), definition);
    }

    public Strategy publishNewVersion(PublishNewVersionData data) {
        Objects.requireNonNull(data, PUBLISH_DATA_REQUIRED);
        StrategyDefinition nextDefinition =
                StrategyDefinition.create(
                        new StrategyDefinition(current.definition().name(), data.rules()));
        return new Strategy(id, ownerId, privacy, current.next(nextDefinition));
    }

    public StrategyId id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public Privacy privacy() {
        return privacy;
    }

    public StrategyVersion current() {
        return current;
    }

    public record CreateStrategyData(String ownerId, StrategyDefinition definition) {}

    public record PublishNewVersionData(List<StrategyDefinition.Rule> rules) {
        public PublishNewVersionData {
            rules = NonEmptyRuleList.copyRequired(rules);
        }
    }
}
