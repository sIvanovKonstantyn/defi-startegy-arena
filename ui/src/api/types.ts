export type StrategyRule = {
  id: string;
  conditionType: string;
  actionType: string;
  instrument: string;
  indicator: string;
  threshold: string;
  allocationPercent: string;
};

export type StrategySummary = {
  strategyId: string;
  name: string;
  privacy: string;
  versionNumber: number;
};

export type StrategyDetail = StrategySummary & {
  rules: StrategyRule[];
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
  rules: StrategyRule[];
};

export type UpdateStrategyBody = {
  rules: StrategyRule[];
};
