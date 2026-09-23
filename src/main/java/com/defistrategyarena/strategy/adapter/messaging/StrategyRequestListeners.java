package com.defistrategyarena.strategy.adapter.messaging;

import com.defistrategyarena.shared.events.strategy.CreateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.CreateStrategyFailed;
import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyFailed;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyRequested;
import com.defistrategyarena.shared.events.strategy.GetStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.GetStrategyFailed;
import com.defistrategyarena.shared.events.strategy.GetStrategyRequested;
import com.defistrategyarena.shared.events.strategy.ListStrategiesCompleted;
import com.defistrategyarena.shared.events.strategy.ListStrategiesFailed;
import com.defistrategyarena.shared.events.strategy.ListStrategiesRequested;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyFailed;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyRequested;
import com.defistrategyarena.shared.messaging.DomainEventListener;
import com.defistrategyarena.shared.messaging.DomainEventListenerRegistry;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.adapter.web.StrategyHttpViews;
import com.defistrategyarena.strategy.application.CreateStrategyCommand;
import com.defistrategyarena.strategy.application.DeleteStrategyCommand;
import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.application.StrategyUseCases;
import com.defistrategyarena.strategy.application.UpdateStrategyCommand;
import com.defistrategyarena.strategy.application.UpdateStrategyResult;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StrategyRequestListeners {

    private static final String DEPS_REQUIRED = "strategy request listeners deps must not be null";
    private static final String REASON_DUPLICATE = "DUPLICATE";
    private static final String REASON_BAD_REQUEST = "BAD_REQUEST";
    private static final String REASON_NOT_FOUND = "NOT_FOUND";

    private final StrategyUseCases useCases;
    private final DomainEventPublisher events;

    public StrategyRequestListeners(StrategyRequestListenersDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.useCases = deps.useCases();
        this.events = deps.events();
    }

    public void register(DomainEventListenerRegistry registry) {
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        CreateStrategyRequested.class, createListener()));
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        ListStrategiesRequested.class, listListener()));
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        GetStrategyRequested.class, getListener()));
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        UpdateStrategyRequested.class, updateListener()));
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        DeleteStrategyRequested.class, deleteListener()));
    }

    private DomainEventListener<CreateStrategyRequested> createListener() {
        return event -> {
            try {
                StrategyId id =
                        useCases.createStrategy()
                                .execute(
                                        CreateStrategyCommand.create(
                                                new CreateStrategyCommand(
                                                        event.ownerId(),
                                                        StrategyDefinition.create(
                                                                new StrategyDefinition(
                                                                        event.name(),
                                                                        StrategyEventRuleMapper.toRules(
                                                                                new StrategyEventRuleMapper
                                                                                        .RulePayloadList(
                                                                                        event.rules())))))));
                events.publish(
                        new CreateStrategyCompleted(
                                event.correlationId(), event.ownerId(), id.value()));
            } catch (DuplicateStrategyException exception) {
                events.publish(
                        new CreateStrategyFailed(
                                event.correlationId(), event.ownerId(), REASON_DUPLICATE));
            } catch (IllegalArgumentException exception) {
                events.publish(
                        new CreateStrategyFailed(
                                event.correlationId(), event.ownerId(), REASON_BAD_REQUEST));
            }
        };
    }

    private DomainEventListener<ListStrategiesRequested> listListener() {
        return event -> {
            try {
                StrategyPage page =
                        useCases.listStrategies()
                                .execute(
                                        new ListStrategiesQuery(
                                                event.ownerId(),
                                                event.page(),
                                                event.size(),
                                                event.sort(),
                                                event.order()));
                List<ListStrategiesCompleted.StrategySummaryPayload> items = new ArrayList<>();
                for (Strategy strategy : page.items()) {
                    items.add(StrategyHttpViews.toEventSummary(strategy));
                }
                events.publish(
                        new ListStrategiesCompleted(
                                event.correlationId(),
                                event.ownerId(),
                                List.copyOf(items),
                                page.totalElements()));
            } catch (IllegalArgumentException exception) {
                events.publish(
                        new ListStrategiesFailed(
                                event.correlationId(), event.ownerId(), REASON_BAD_REQUEST));
            }
        };
    }

    private DomainEventListener<GetStrategyRequested> getListener() {
        return event -> {
            try {
                Optional<Strategy> found =
                        useCases.getStrategy()
                                .execute(
                                        new GetStrategyQuery(
                                                event.ownerId(), new StrategyId(event.strategyId())));
                if (found.isEmpty()) {
                    events.publish(
                            new GetStrategyFailed(
                                    event.correlationId(), event.ownerId(), REASON_NOT_FOUND));
                    return;
                }
                Strategy strategy = found.get();
                StrategyHttpViews.StrategyProjection projection =
                        StrategyHttpViews.projection(strategy);
                events.publish(
                        new GetStrategyCompleted(
                                event.correlationId(),
                                event.ownerId(),
                                projection.strategyId(),
                                projection.name(),
                                projection.privacy(),
                                projection.versionNumber(),
                                StrategyEventRuleMapper.toPayloads(
                                        new StrategyEventRuleMapper.RuleDomainList(
                                                strategy.current().definition().rules()))));
            } catch (IllegalArgumentException exception) {
                events.publish(
                        new GetStrategyFailed(
                                event.correlationId(), event.ownerId(), REASON_BAD_REQUEST));
            }
        };
    }

    private DomainEventListener<UpdateStrategyRequested> updateListener() {
        return event -> {
            try {
                Optional<UpdateStrategyResult> result =
                        useCases.updateStrategy()
                                .execute(
                                        new UpdateStrategyCommand(
                                                event.ownerId(),
                                                new StrategyId(event.strategyId()),
                                                StrategyEventRuleMapper.toRules(
                                                        new StrategyEventRuleMapper.RulePayloadList(
                                                                event.rules()))));
                if (result.isEmpty()) {
                    events.publish(
                            new UpdateStrategyFailed(
                                    event.correlationId(), event.ownerId(), REASON_NOT_FOUND));
                    return;
                }
                UpdateStrategyResult updated = result.get();
                events.publish(
                        new UpdateStrategyCompleted(
                                event.correlationId(),
                                event.ownerId(),
                                updated.strategyId().value(),
                                updated.versionNumber()));
            } catch (IllegalArgumentException exception) {
                events.publish(
                        new UpdateStrategyFailed(
                                event.correlationId(), event.ownerId(), REASON_BAD_REQUEST));
            }
        };
    }

    private DomainEventListener<DeleteStrategyRequested> deleteListener() {
        return event -> {
            try {
                Optional<StrategyId> deleted =
                        useCases.deleteStrategy()
                                .execute(
                                        new DeleteStrategyCommand(
                                                event.ownerId(), new StrategyId(event.strategyId())));
                if (deleted.isEmpty()) {
                    events.publish(
                            new DeleteStrategyFailed(
                                    event.correlationId(), event.ownerId(), REASON_NOT_FOUND));
                    return;
                }
                events.publish(
                        new DeleteStrategyCompleted(
                                event.correlationId(), event.ownerId(), deleted.get().value()));
            } catch (IllegalArgumentException exception) {
                events.publish(
                        new DeleteStrategyFailed(
                                event.correlationId(), event.ownerId(), REASON_BAD_REQUEST));
            }
        };
    }

    public record StrategyRequestListenersDeps(StrategyUseCases useCases, DomainEventPublisher events) {}
}
