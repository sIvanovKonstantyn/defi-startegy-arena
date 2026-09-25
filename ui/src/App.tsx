import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AuthPage } from "./AuthPage";
import {
  createStrategy,
  deleteStrategy,
  getStrategy,
  listStrategies,
  login,
  signup,
  updateStrategy,
} from "./api/http";
import { SessionSocket } from "./api/sessionSocket";
import type {
  ActionType,
  ActionWire,
  CompareOperator,
  ConditionType,
  ConditionWire,
  CreateStrategyBody,
  StrategyDetail,
  StrategyRule,
  StrategySummary,
  UpdateStrategyBody,
  WsEnvelope,
} from "./api/types";
import { ErrorModal } from "./errors/ErrorModal";
import { toUserErrorMessage } from "./errors/messages";
import { BrandMark, IconLogout } from "./icons/IconSet";
import { type SaveStrategyInput, StrategiesPage } from "./StrategiesPage";
import { clearAccessToken, readAccessToken, writeAccessToken } from "./session/tokenStore";
import { Button } from "./ui/Button";

type PendingWaiter = {
  resolve: (envelope: WsEnvelope) => void;
  reject: (error: Error) => void;
};

const CONDITION_TYPES: ConditionType[] = ["and", "or", "price_compare", "indicator_compare"];
const ACTION_TYPES: ActionType[] = ["hold", "buy", "sell", "open_lp"];
const OPERATORS: CompareOperator[] = ["lt", "lte", "gt", "gte", "eq"];
const FALLBACK_CONDITION_TYPE: ConditionType = "price_compare";
const FALLBACK_ACTION_TYPE: ActionType = "hold";

const PAGE_SIZE = 10;
const EMPTY_TOTAL = 0;
const FIRST_PAGE = 0;
const WAIT_TIMEOUT_MS = 15_000;
const RECONNECT_DELAY_MS = 400;
const RESULT_TIMEOUT = "Timed out waiting for strategy result";

function asText(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function asRecord(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === "object" ? (value as Record<string, unknown>) : {};
}

function asParameters(value: unknown): Record<string, string> {
  const source = asRecord(value);
  const parameters: Record<string, string> = {};
  for (const [key, entry] of Object.entries(source)) {
    parameters[key] = asText(entry);
  }
  return parameters;
}

function asConditionType(value: unknown): ConditionType {
  const text = asText(value);
  return CONDITION_TYPES.find((type) => type === text) ?? FALLBACK_CONDITION_TYPE;
}

function asOperator(value: unknown): CompareOperator | undefined {
  const text = asText(value);
  return OPERATORS.find((operator) => operator === text);
}

function asCondition(value: unknown): ConditionWire {
  const row = asRecord(value);
  const childrenRaw = Array.isArray(row.children) ? row.children : [];
  return {
    type: asConditionType(row.type),
    children: childrenRaw.map(asCondition),
    instrument: asText(row.instrument),
    indicator: asText(row.indicator),
    operator: asOperator(row.operator),
    threshold: asText(row.threshold),
    parameters: asParameters(row.parameters),
  };
}

function asAction(value: unknown): ActionWire {
  const row = asRecord(value);
  const text = asText(row.type);
  return {
    type: ACTION_TYPES.find((type) => type === text) ?? FALLBACK_ACTION_TYPE,
    instrument: asText(row.instrument),
    instrumentPair: asText(row.instrumentPair),
    allocationPercent: asText(row.allocationPercent),
    yearlyFeePercent: asText(row.yearlyFeePercent),
  };
}

function asRules(value: unknown): StrategyRule[] {
  const rulesRaw = Array.isArray(value) ? value : [];
  return rulesRaw.map((rule) => {
    const row = asRecord(rule);
    return {
      id: asText(row.id),
      when: asCondition(row.when),
      // biome-ignore lint/suspicious/noThenProperty: `when` / `then` are the wire field names
      then: asAction(row.then),
    };
  });
}

function asSummaries(payload: Record<string, unknown>): StrategySummary[] {
  const items = payload.items;
  if (!Array.isArray(items)) {
    return [];
  }
  return items.map((item) => {
    const row = asRecord(item);
    return {
      strategyId: asText(row.strategyId),
      name: asText(row.name),
      description: asText(row.description),
      privacy: asText(row.privacy),
      versionNumber: Number(row.versionNumber ?? 0),
    };
  });
}

function asTotal(payload: Record<string, unknown>): number {
  return Number(payload.total ?? EMPTY_TOTAL);
}

function asDetail(payload: Record<string, unknown>): StrategyDetail {
  return {
    strategyId: asText(payload.strategyId),
    name: asText(payload.name),
    description: asText(payload.description),
    privacy: asText(payload.privacy),
    versionNumber: Number(payload.versionNumber ?? 0),
    rules: asRules(payload.rules),
    pnl: asText(payload.pnl),
    drawdown: asText(payload.drawdown),
  };
}

export function App() {
  const [token, setToken] = useState<string | null>(() => readAccessToken());
  const [wsReady, setWsReady] = useState(false);
  const [strategies, setStrategies] = useState<StrategySummary[]>([]);
  const [selected, setSelected] = useState<StrategyDetail | null>(null);
  const [page, setPage] = useState(FIRST_PAGE);
  const [total, setTotal] = useState(EMPTY_TOTAL);
  const [error, setError] = useState<string | null>(null);
  const [viewKey, setViewKey] = useState(FIRST_PAGE);
  const pending = useRef(new Map<string, PendingWaiter[]>());
  const buffer = useRef(new Map<string, WsEnvelope>());
  const socketRef = useRef<SessionSocket | null>(null);
  const pageRef = useRef(page);
  pageRef.current = page;

  const reportError = useCallback((err: unknown) => {
    setError(toUserErrorMessage(err));
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const settleEnvelope = useCallback((envelope: WsEnvelope, waiter: PendingWaiter) => {
    if (envelope.status === "failed") {
      const reason =
        typeof envelope.payload.reasonCode === "string" ? envelope.payload.reasonCode : "FAILED";
      waiter.reject(new Error(reason));
      return;
    }
    waiter.resolve(envelope);
  }, []);

  const waitFor = useCallback(
    (correlationId: string) => {
      const buffered = buffer.current.get(correlationId);
      if (buffered) {
        buffer.current.delete(correlationId);
        return new Promise<WsEnvelope>((resolve, reject) => {
          settleEnvelope(buffered, { resolve, reject });
        });
      }
      return new Promise<WsEnvelope>((resolve, reject) => {
        const existing = pending.current.get(correlationId) ?? [];
        const waiter: PendingWaiter = { resolve, reject };
        existing.push(waiter);
        pending.current.set(correlationId, existing);
        window.setTimeout(() => {
          const waiters = pending.current.get(correlationId);
          if (!waiters?.includes(waiter)) {
            return;
          }
          pending.current.set(
            correlationId,
            waiters.filter((entry) => entry !== waiter),
          );
          reject(new Error(RESULT_TIMEOUT));
        }, WAIT_TIMEOUT_MS);
      });
    },
    [settleEnvelope],
  );

  const onEnvelope = useCallback(
    (envelope: WsEnvelope) => {
      const waiters = pending.current.get(envelope.correlationId) ?? [];
      if (waiters.length === 0) {
        buffer.current.set(envelope.correlationId, envelope);
        return;
      }
      pending.current.delete(envelope.correlationId);
      for (const waiter of waiters) {
        settleEnvelope(envelope, waiter);
      }
    },
    [settleEnvelope],
  );

  const onEnvelopeRef = useRef(onEnvelope);
  onEnvelopeRef.current = onEnvelope;
  const tokenRef = useRef(token);
  tokenRef.current = token;

  useEffect(() => {
    if (!token) {
      socketRef.current?.close();
      socketRef.current = null;
      setWsReady(false);
      return;
    }
    let active = true;
    let reconnectTimer: number | undefined;

    const connect = () => {
      if (!active || !tokenRef.current) {
        return;
      }
      const socket = new SessionSocket({
        onEnvelope: (envelope) => {
          if (active) {
            onEnvelopeRef.current(envelope);
          }
        },
        onAuthOk: () => {
          if (active) {
            setWsReady(true);
          }
        },
        onAuthFailed: () => {
          if (!active) {
            return;
          }
          setWsReady(false);
          setError(toUserErrorMessage("WebSocket authentication failed"));
          clearAccessToken();
          setToken(null);
        },
        onClose: () => {
          if (!active) {
            return;
          }
          setWsReady(false);
          reconnectTimer = window.setTimeout(connect, RECONNECT_DELAY_MS);
        },
      });
      socketRef.current = socket;
      socket.connect(tokenRef.current);
    };

    connect();
    return () => {
      active = false;
      window.clearTimeout(reconnectTimer);
      socketRef.current?.close();
      socketRef.current = null;
    };
  }, [token]);

  const refreshList = useCallback(
    async (targetPage = pageRef.current) => {
      const accessToken = tokenRef.current;
      if (!accessToken) {
        return;
      }
      const accepted = await listStrategies(accessToken, {
        page: targetPage,
        size: PAGE_SIZE,
      });
      const envelope = await waitFor(accepted.correlationId);
      setStrategies(asSummaries(envelope.payload));
      setTotal(asTotal(envelope.payload));
      setPage(targetPage);
    },
    [waitFor],
  );

  useEffect(() => {
    if (token && wsReady) {
      void refreshList(FIRST_PAGE).catch((err: unknown) => {
        reportError(err);
      });
    }
  }, [token, wsReady, refreshList, reportError]);
  const handleAuth = useMemo(
    () => ({
      async signup(email: string, password: string, displayName: string) {
        setError(null);
        const session = await signup({ email, password, displayName });
        writeAccessToken(session.accessToken);
        setToken(session.accessToken);
      },
      async login(email: string, password: string) {
        setError(null);
        const session = await login({ email, password });
        writeAccessToken(session.accessToken);
        setToken(session.accessToken);
      },
    }),
    [],
  );

  const handleLogout = () => {
    clearAccessToken();
    setToken(null);
    setStrategies([]);
    setSelected(null);
    setPage(FIRST_PAGE);
    setTotal(EMPTY_TOTAL);
    setWsReady(false);
  };

  const handleCreate = async (body: CreateStrategyBody) => {
    if (!token) {
      return;
    }
    setError(null);
    const accepted = await createStrategy(token, body);
    await waitFor(accepted.correlationId);
    await refreshList(FIRST_PAGE);
  };

  const handleEdit = async (strategyId: string) => {
    if (!token) {
      return;
    }
    setError(null);
    const accepted = await getStrategy(token, strategyId);
    const envelope = await waitFor(accepted.correlationId);
    setSelected(asDetail(envelope.payload));
  };

  const handleSave = async (input: SaveStrategyInput) => {
    if (!token) {
      return;
    }
    setError(null);
    const body: UpdateStrategyBody = { description: input.description, rules: input.rules };
    const accepted = await updateStrategy(token, input.strategyId, body);
    await waitFor(accepted.correlationId);
    setSelected(null);
    await refreshList();
  };

  const handleDelete = async (strategyId: string) => {
    if (!token) {
      return;
    }
    setError(null);
    const accepted = await deleteStrategy(token, strategyId);
    await waitFor(accepted.correlationId);
    if (selected?.strategyId === strategyId) {
      setSelected(null);
    }
    const nextPage =
      strategies.length <= 1 && pageRef.current > FIRST_PAGE
        ? pageRef.current - 1
        : pageRef.current;
    await refreshList(nextPage);
  };

  const goHome = () => {
    setSelected(null);
    setError(null);
    setPage(FIRST_PAGE);
    setViewKey((current) => current + 1);
    if (tokenRef.current && wsReady) {
      void refreshList(FIRST_PAGE).catch((err: unknown) => {
        reportError(err);
      });
    }
  };

  const header = (
    <header className="app-header">
      <a
        className="brand-home"
        href="/"
        aria-label="Home"
        onClick={(event) => {
          event.preventDefault();
          goHome();
          if (window.location.pathname !== "/" || window.location.search || window.location.hash) {
            window.history.pushState({}, "", "/");
          }
        }}
      >
        <BrandMark />
        <span className="brand-home-title">DeFi Arena</span>
      </a>
      {token ? (
        <Button variant="secondary" onClick={handleLogout}>
          <IconLogout />
          Log out
        </Button>
      ) : null}
    </header>
  );

  if (!token) {
    return (
      <div className="app-shell">
        {header}
        <AuthPage onLogin={handleAuth.login} onSignup={handleAuth.signup} onError={reportError} />
        {error ? <ErrorModal message={error} onClose={clearError} /> : null}
      </div>
    );
  }

  return (
    <div className="app-shell">
      {header}
      <StrategiesPage
        key={viewKey}
        strategies={strategies}
        selected={selected}
        page={page}
        pageSize={PAGE_SIZE}
        total={total}
        onPageChange={(nextPage) => {
          void refreshList(nextPage).catch((err: unknown) => {
            reportError(err);
          });
        }}
        onCreate={handleCreate}
        onEdit={handleEdit}
        onSave={handleSave}
        onDelete={handleDelete}
        onCloseEditor={() => setSelected(null)}
        onError={reportError}
      />
      {error ? <ErrorModal message={error} onClose={clearError} /> : null}
    </div>
  );
}
