package com.defistrategyarena.integration;

import com.defistrategyarena.shared.events.strategy.CreateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.CreateStrategyFailed;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyFailed;
import com.defistrategyarena.shared.events.strategy.GetStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.GetStrategyFailed;
import com.defistrategyarena.shared.events.strategy.ListStrategiesCompleted;
import com.defistrategyarena.shared.events.strategy.ListStrategiesFailed;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyFailed;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class StrategyResponseEnvelopes {

    static final String KEY_CORRELATION = "correlationId";
    static final String KEY_TYPE = "type";
    static final String KEY_STATUS = "status";
    static final String KEY_PAYLOAD = "payload";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String TYPE_CREATE = "strategy.create";
    private static final String TYPE_LIST = "strategy.list";
    private static final String TYPE_GET = "strategy.get";
    private static final String TYPE_UPDATE = "strategy.update";
    private static final String TYPE_DELETE = "strategy.delete";
    private static final String KEY_STRATEGY_ID = "strategyId";
    private static final String KEY_REASON = "reasonCode";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_NAME = "name";
    private static final String KEY_PRIVACY = "privacy";
    private static final String KEY_VERSION = "versionNumber";
    private static final String KEY_RULES = "rules";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PNL = "pnl";
    private static final String KEY_DRAWDOWN = "drawdown";

    PushEnvelope createCompleted(CreateStrategyCompleted event) {
        return completed(
                new CompletedParts(
                        event.ownerId(),
                        event.correlationId(),
                        TYPE_CREATE,
                        Map.of(KEY_STRATEGY_ID, event.strategyId())));
    }

    PushEnvelope createFailed(CreateStrategyFailed event) {
        return failed(new FailedParts(event.ownerId(), event.correlationId(), TYPE_CREATE, event.reasonCode()));
    }

    PushEnvelope listCompleted(ListStrategiesCompleted event) {
        return completed(
                new CompletedParts(
                        event.ownerId(),
                        event.correlationId(),
                        TYPE_LIST,
                        Map.of(KEY_ITEMS, event.items(), KEY_TOTAL, event.total())));
    }

    PushEnvelope listFailed(ListStrategiesFailed event) {
        return failed(new FailedParts(event.ownerId(), event.correlationId(), TYPE_LIST, event.reasonCode()));
    }

    PushEnvelope getCompleted(GetStrategyCompleted event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(KEY_STRATEGY_ID, event.strategyId());
        payload.put(KEY_NAME, event.name());
        payload.put(KEY_DESCRIPTION, event.description());
        payload.put(KEY_PRIVACY, event.privacy());
        payload.put(KEY_VERSION, event.versionNumber());
        payload.put(KEY_RULES, event.rules());
        payload.put(KEY_PNL, event.pnl());
        payload.put(KEY_DRAWDOWN, event.drawdown());
        return completed(
                new CompletedParts(event.ownerId(), event.correlationId(), TYPE_GET, Map.copyOf(payload)));
    }

    PushEnvelope getFailed(GetStrategyFailed event) {
        return failed(new FailedParts(event.ownerId(), event.correlationId(), TYPE_GET, event.reasonCode()));
    }

    PushEnvelope updateCompleted(UpdateStrategyCompleted event) {
        return completed(
                new CompletedParts(
                        event.ownerId(),
                        event.correlationId(),
                        TYPE_UPDATE,
                        Map.of(KEY_STRATEGY_ID, event.strategyId(), KEY_VERSION, event.versionNumber())));
    }

    PushEnvelope updateFailed(UpdateStrategyFailed event) {
        return failed(new FailedParts(event.ownerId(), event.correlationId(), TYPE_UPDATE, event.reasonCode()));
    }

    PushEnvelope deleteCompleted(DeleteStrategyCompleted event) {
        return completed(
                new CompletedParts(
                        event.ownerId(),
                        event.correlationId(),
                        TYPE_DELETE,
                        Map.of(KEY_STRATEGY_ID, event.strategyId())));
    }

    PushEnvelope deleteFailed(DeleteStrategyFailed event) {
        return failed(new FailedParts(event.ownerId(), event.correlationId(), TYPE_DELETE, event.reasonCode()));
    }

    private static PushEnvelope completed(CompletedParts parts) {
        return new PushEnvelope(
                parts.ownerId(),
                parts.correlationId(),
                parts.type(),
                STATUS_COMPLETED,
                parts.payload());
    }

    private static PushEnvelope failed(FailedParts parts) {
        return new PushEnvelope(
                parts.ownerId(),
                parts.correlationId(),
                parts.type(),
                STATUS_FAILED,
                Map.of(KEY_REASON, parts.reasonCode()));
    }

    record PushEnvelope(
            String ownerId,
            String correlationId,
            String type,
            String status,
            Map<String, Object> payload) {}

    private record CompletedParts(
            String ownerId, String correlationId, String type, Map<String, Object> payload) {}

    private record FailedParts(String ownerId, String correlationId, String type, String reasonCode) {}
}
