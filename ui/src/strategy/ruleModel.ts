import type {
  ActionType,
  ActionWire,
  CompareOperator,
  ConditionType,
  ConditionWire,
  StrategyRule,
} from "../api/types";
import {
  DEFAULT_INDICATOR_ID,
  indicatorDefaults,
  indicatorParameters,
} from "../indicators/catalog";

export type NodePath = number[];

export const CONDITION_TYPE_OPTIONS: { value: ConditionType; label: string }[] = [
  { value: "and", label: "All of (AND)" },
  { value: "or", label: "Any of (OR)" },
  { value: "price_compare", label: "Price compare" },
  { value: "indicator_compare", label: "Indicator compare" },
];

export const OPERATOR_OPTIONS: { value: CompareOperator; label: string }[] = [
  { value: "lt", label: "is below (lt)" },
  { value: "lte", label: "is at most (lte)" },
  { value: "gt", label: "is above (gt)" },
  { value: "gte", label: "is at least (gte)" },
  { value: "eq", label: "equals (eq)" },
];

export const ACTION_TYPE_OPTIONS: { value: ActionType; label: string }[] = [
  { value: "hold", label: "Hold" },
  { value: "buy", label: "Buy" },
  { value: "sell", label: "Sell" },
  { value: "open_lp", label: "Open LP position" },
];

const DEFAULT_INSTRUMENT = "ETH-USD";
const DEFAULT_INSTRUMENT_PAIR = "ETH-USDC";
const DEFAULT_THRESHOLD = "3000";
const DEFAULT_ALLOCATION_PERCENT = "25";
const DEFAULT_YEARLY_FEE_PERCENT = "12";
const DEFAULT_OPERATOR: CompareOperator = "gt";
const GROUP_TYPES: ConditionType[] = ["and", "or"];

export function isGroup(condition: ConditionWire): boolean {
  return GROUP_TYPES.includes(condition.type);
}

export function childrenOf(condition: ConditionWire): ConditionWire[] {
  return condition.children ?? [];
}

export function newPriceCompare(): ConditionWire {
  return {
    type: "price_compare",
    instrument: DEFAULT_INSTRUMENT,
    operator: DEFAULT_OPERATOR,
    threshold: DEFAULT_THRESHOLD,
  };
}

export function newIndicatorCompare(): ConditionWire {
  return {
    type: "indicator_compare",
    indicator: DEFAULT_INDICATOR_ID,
    parameters: indicatorDefaults(DEFAULT_INDICATOR_ID),
    operator: DEFAULT_OPERATOR,
    threshold: DEFAULT_THRESHOLD,
  };
}

export function newGroup(type: ConditionType): ConditionWire {
  return { type, children: [newPriceCompare()] };
}

export function newAction(type: ActionType): ActionWire {
  if (type === "buy" || type === "sell") {
    return { type, instrument: DEFAULT_INSTRUMENT, allocationPercent: DEFAULT_ALLOCATION_PERCENT };
  }
  if (type === "open_lp") {
    return {
      type,
      instrumentPair: DEFAULT_INSTRUMENT_PAIR,
      allocationPercent: DEFAULT_ALLOCATION_PERCENT,
      yearlyFeePercent: DEFAULT_YEARLY_FEE_PERCENT,
    };
  }
  return { type: "hold" };
}

export function newRule(ruleNumber: number): StrategyRule {
  // biome-ignore lint/suspicious/noThenProperty: `when` / `then` are the wire field names
  return { id: `r${ruleNumber}`, when: newPriceCompare(), then: newAction("hold") };
}

export function nextRuleNumber(rules: StrategyRule[]): number {
  const numbers = rules.map((rule) => Number(rule.id.replace(/\D/g, "")) || 0);
  return Math.max(rules.length, ...numbers) + 1;
}

export function conditionOfType(type: ConditionType): ConditionWire {
  if (type === "and" || type === "or") {
    return newGroup(type);
  }
  if (type === "indicator_compare") {
    return newIndicatorCompare();
  }
  return newPriceCompare();
}

export function withIndicator(condition: ConditionWire, indicator: string): ConditionWire {
  const defaults = indicatorDefaults(indicator);
  const previous = condition.parameters ?? {};
  const parameters: Record<string, string> = {};
  for (const parameter of indicatorParameters(indicator)) {
    parameters[parameter.name] = previous[parameter.name] ?? defaults[parameter.name];
  }
  return { ...condition, indicator, parameters };
}

export function withParameter(
  condition: ConditionWire,
  parameter: { name: string; value: string },
): ConditionWire {
  return {
    ...condition,
    parameters: { ...(condition.parameters ?? {}), [parameter.name]: parameter.value },
  };
}

export function replaceNode(
  root: ConditionWire,
  path: NodePath,
  next: ConditionWire,
): ConditionWire {
  if (path.length === 0) {
    return next;
  }
  const [head, ...rest] = path;
  const children = childrenOf(root).map((child, index) =>
    index === head ? replaceNode(child, rest, next) : child,
  );
  return { ...root, children };
}

export function appendChild(
  root: ConditionWire,
  path: NodePath,
  child: ConditionWire,
): ConditionWire {
  if (path.length === 0) {
    return { ...root, children: [...childrenOf(root), child] };
  }
  const [head, ...rest] = path;
  const children = childrenOf(root).map((node, index) =>
    index === head ? appendChild(node, rest, child) : node,
  );
  return { ...root, children };
}

export function removeNode(root: ConditionWire, path: NodePath): ConditionWire {
  if (path.length === 0) {
    return root;
  }
  const parentPath = path.slice(0, -1);
  const target = path[path.length - 1];
  return mapChildren(root, parentPath, (children) =>
    children.filter((_child, index) => index !== target),
  );
}

function mapChildren(
  root: ConditionWire,
  path: NodePath,
  transform: (children: ConditionWire[]) => ConditionWire[],
): ConditionWire {
  if (path.length === 0) {
    return { ...root, children: transform(childrenOf(root)) };
  }
  const [head, ...rest] = path;
  const children = childrenOf(root).map((child, index) =>
    index === head ? mapChildren(child, rest, transform) : child,
  );
  return { ...root, children };
}

export function describeAction(action: ActionWire): string {
  const option = ACTION_TYPE_OPTIONS.find((entry) => entry.value === action.type);
  return option?.label ?? action.type;
}

const EMPTY_GROUP_MESSAGE = "Every AND/OR group needs at least one condition";
const BLANK_FIELD_MESSAGE = "Fill in every condition and action field";

export function firstRuleProblem(rules: StrategyRule[]): string | null {
  for (const rule of rules) {
    const problem = conditionProblem(rule.when) ?? actionProblem(rule.then);
    if (problem) {
      return problem;
    }
  }
  return null;
}

function conditionProblem(condition: ConditionWire): string | null {
  if (isGroup(condition)) {
    const children = childrenOf(condition);
    if (children.length === 0) {
      return EMPTY_GROUP_MESSAGE;
    }
    for (const child of children) {
      const problem = conditionProblem(child);
      if (problem) {
        return problem;
      }
    }
    return null;
  }
  const values =
    condition.type === "price_compare"
      ? [condition.instrument, condition.threshold]
      : [condition.indicator, condition.threshold, ...Object.values(condition.parameters ?? {})];
  return values.some(isBlank) ? BLANK_FIELD_MESSAGE : null;
}

function actionProblem(action: ActionWire): string | null {
  if (action.type === "buy" || action.type === "sell") {
    return [action.instrument, action.allocationPercent].some(isBlank) ? BLANK_FIELD_MESSAGE : null;
  }
  if (action.type === "open_lp") {
    return [action.instrumentPair, action.allocationPercent, action.yearlyFeePercent].some(isBlank)
      ? BLANK_FIELD_MESSAGE
      : null;
  }
  return null;
}

function isBlank(value: string | undefined): boolean {
  return (value ?? "").trim().length === 0;
}
