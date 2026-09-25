export type CompareOperator = "lt" | "lte" | "gt" | "gte" | "eq";

export type ConditionType = "and" | "or" | "price_compare" | "indicator_compare";

export type ActionType = "hold" | "buy" | "sell" | "open_lp";

export type ConditionWire = {
  type: ConditionType;
  children?: ConditionWire[];
  instrument?: string;
  indicator?: string;
  operator?: CompareOperator;
  threshold?: string;
  parameters?: Record<string, string>;
};

export type ActionWire = {
  type: ActionType;
  instrument?: string;
  instrumentPair?: string;
  allocationPercent?: string;
  yearlyFeePercent?: string;
};

export type StrategyRule = {
  id: string;
  when: ConditionWire;
  then: ActionWire;
};

export type StrategySummary = {
  strategyId: string;
  name: string;
  description: string;
  privacy: string;
  versionNumber: number;
};

export type StrategyDetail = StrategySummary & {
  rules: StrategyRule[];
  pnl: string;
  drawdown: string;
};

export type AcceptedResponse = {
  status: number;
  correlationId: string;
};

export type SessionResponse = {
  status: number;
  accessToken: string;
};

export type WsEnvelope = {
  correlationId: string;
  type: string;
  status: "completed" | "failed";
  payload: Record<string, unknown>;
};

export type CreateStrategyBody = {
  name: string;
  description: string;
  rules: StrategyRule[];
};

export type UpdateStrategyBody = {
  description: string;
  rules: StrategyRule[];
};
