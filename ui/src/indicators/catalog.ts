/**
 * Mirror of the backend catalog in
 * `src/main/java/com/defistrategyarena/shared/indicators/IndicatorCatalog.java`.
 * Ids, parameter names and defaults must stay identical; `label` is UI-only.
 */

export type IndicatorParameterType = "integer" | "decimal";

export type IndicatorParameterSpec = {
  name: string;
  label: string;
  type: IndicatorParameterType;
  required: boolean;
  defaultValue: string;
};

export type IndicatorDefinition = {
  id: string;
  displayName: string;
  parameters: IndicatorParameterSpec[];
};

const PERIOD: IndicatorParameterSpec = {
  name: "period",
  label: "Period",
  type: "integer",
  required: true,
  defaultValue: "14",
};

const STD_DEV: IndicatorParameterSpec = {
  name: "stdDev",
  label: "Std dev",
  type: "decimal",
  required: true,
  defaultValue: "2",
};

const FAST: IndicatorParameterSpec = {
  name: "fast",
  label: "Fast",
  type: "integer",
  required: true,
  defaultValue: "12",
};

const SLOW: IndicatorParameterSpec = {
  name: "slow",
  label: "Slow",
  type: "integer",
  required: true,
  defaultValue: "26",
};

const SIGNAL: IndicatorParameterSpec = {
  name: "signal",
  label: "Signal",
  type: "integer",
  required: true,
  defaultValue: "9",
};

export const INDICATOR_CATALOG: IndicatorDefinition[] = [
  { id: "sma", displayName: "SMA", parameters: [PERIOD] },
  { id: "ema", displayName: "EMA", parameters: [PERIOD] },
  { id: "rsi", displayName: "RSI", parameters: [PERIOD] },
  { id: "bollinger_bands", displayName: "Bollinger Bands", parameters: [PERIOD, STD_DEV] },
  { id: "macd", displayName: "MACD", parameters: [FAST, SLOW, SIGNAL] },
];

export const DEFAULT_INDICATOR_ID = INDICATOR_CATALOG[0].id;

export function findIndicator(id: string): IndicatorDefinition | undefined {
  return INDICATOR_CATALOG.find((indicator) => indicator.id === id);
}

export function indicatorParameters(id: string): IndicatorParameterSpec[] {
  return findIndicator(id)?.parameters ?? [];
}

export function indicatorDefaults(id: string): Record<string, string> {
  const defaults: Record<string, string> = {};
  for (const parameter of indicatorParameters(id)) {
    defaults[parameter.name] = parameter.defaultValue;
  }
  return defaults;
}

export function indicatorLabel(id: string): string {
  return findIndicator(id)?.displayName ?? id;
}
