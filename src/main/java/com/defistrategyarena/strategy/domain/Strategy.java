package com.defistrategyarena.strategy.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import java.util.List;
import java.util.Objects;

public final class Strategy {

    private static final String OWNER_REQUIRED = "owner id must not be blank";
    private static final String DATA_REQUIRED = "create strategy data must not be null";
    private static final String PUBLISH_DATA_REQUIRED = "publish new version data must not be null";
    private static final String REHYDRATE_DATA_REQUIRED = "rehydrate strategy data must not be null";

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
        String ownerId = requireOwnerId(new OwnerIdText(data.ownerId()));
        StrategyDefinition definition = StrategyDefinition.create(data.definition());
        StrategyId id =
                StrategyId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(
                                        List.of(ownerId, definition.name()))));
        return new Strategy(id, ownerId, definition);
    }

    public Strategy publishNewVersion(PublishNewVersionData data) {
        Objects.requireNonNull(data, PUBLISH_DATA_REQUIRED);
        StrategyDefinition currentDefinition = current.definition();
        StrategyDefinition nextDefinition =
                StrategyDefinition.create(
                        new StrategyDefinition(
                                currentDefinition.name(), data.description(), data.rules()));
        return new Strategy(id, ownerId, privacy, current.next(nextDefinition));
    }

    public static Strategy rehydrate(RehydrateData data) {
        Objects.requireNonNull(data, REHYDRATE_DATA_REQUIRED);
        String ownerId = requireOwnerId(new OwnerIdText(data.ownerId()));
        StrategyDefinition definition = StrategyDefinition.create(data.definition());
        StrategyVersion version =
                StrategyVersion.create(new StrategyVersion(data.versionNumber(), definition));
        return new Strategy(data.id(), ownerId, data.privacy(), version);
    }

    private static String requireOwnerId(OwnerIdText ownerId) {
        if (ownerId.value() == null || ownerId.value().isBlank()) {
            throw new IllegalArgumentException(OWNER_REQUIRED);
        }
        return ownerId.value();
    }

    private record OwnerIdText(String value) {}

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

    public record PublishNewVersionData(String description, List<StrategyDefinition.Rule> rules) {
        private static final String DESCRIPTION_REQUIRED = "strategy description must not be null";

        public PublishNewVersionData {
            if (description == null) {
                throw new IllegalArgumentException(DESCRIPTION_REQUIRED);
            }
            rules = NonEmptyRuleList.copyRequired(rules);
        }
    }

    public record RehydrateData(
            StrategyId id,
            String ownerId,
            Privacy privacy,
            int versionNumber,
            StrategyDefinition definition) {}
}
