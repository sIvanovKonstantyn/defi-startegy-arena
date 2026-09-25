import { describe, expect, it } from "vitest";
import type { ConditionWire } from "../api/types";
import {
  appendChild,
  childrenOf,
  conditionOfType,
  describeAction,
  firstRuleProblem,
  isGroup,
  newAction,
  newIndicatorCompare,
  newPriceCompare,
  newRule,
  nextRuleNumber,
  removeNode,
  replaceNode,
  withIndicator,
  withParameter,
} from "./ruleModel";

function andTree(): ConditionWire {
  return conditionOfType("and");
}

describe("ruleModel", () => {
  it("creates a default rule with a price condition and hold action", () => {
    const rule = newRule(1);
    expect(rule.id).toBe("r1");
    expect(rule.when.type).toBe("price_compare");
    expect(rule.then.type).toBe("hold");
  });

  it("numbers new rules after the highest existing id", () => {
    expect(nextRuleNumber([newRule(1), newRule(4)])).toBe(5);
    expect(nextRuleNumber([])).toBe(1);
  });

  it("builds groups with one starter child", () => {
    const group = andTree();
    expect(isGroup(group)).toBe(true);
    expect(childrenOf(group)).toHaveLength(1);
    expect(isGroup(newPriceCompare())).toBe(false);
  });

  it("appends, replaces and removes nested nodes", () => {
    const withChild = appendChild(andTree(), [], newIndicatorCompare());
    expect(childrenOf(withChild)).toHaveLength(2);

    const nested = appendChild(withChild, [], conditionOfType("or"));
    const deep = appendChild(nested, [2], newPriceCompare());
    expect(childrenOf(childrenOf(deep)[2])).toHaveLength(2);

    const replaced = replaceNode(deep, [2, 1], newIndicatorCompare());
    expect(childrenOf(childrenOf(replaced)[2])[1].type).toBe("indicator_compare");

    const removed = removeNode(replaced, [2, 0]);
    expect(childrenOf(childrenOf(removed)[2])).toHaveLength(1);
    expect(removeNode(replaced, [])).toEqual(replaced);
    expect(replaceNode(replaced, [], newPriceCompare()).type).toBe("price_compare");
  });

  it("keeps known parameters when switching indicator", () => {
    const condition = withParameter(newIndicatorCompare(), { name: "period", value: "30" });
    const switched = withIndicator(condition, "macd");
    expect(switched.indicator).toBe("macd");
    expect(switched.parameters).toEqual({ fast: "12", slow: "26", signal: "9" });
    const back = withIndicator(condition, "ema");
    expect(back.parameters).toEqual({ period: "30" });
  });

  it("describes action options", () => {
    expect(describeAction(newAction("open_lp"))).toBe("Open LP position");
    expect(describeAction({ type: "hold" })).toBe("Hold");
  });

  it("reports empty groups", () => {
    const rule = { ...newRule(1), when: { type: "and", children: [] } as ConditionWire };
    expect(firstRuleProblem([rule])).toBe("Every AND/OR group needs at least one condition");
  });

  it("reports blank leaf fields", () => {
    const rule = { ...newRule(1), when: { ...newPriceCompare(), threshold: " " } };
    expect(firstRuleProblem([rule])).toBe("Fill in every condition and action field");

    const indicatorRule = {
      ...newRule(2),
      when: withParameter(newIndicatorCompare(), { name: "period", value: "" }),
    };
    expect(firstRuleProblem([indicatorRule])).toBe("Fill in every condition and action field");
  });

  it("reports blank action fields", () => {
    // biome-ignore lint/suspicious/noThenProperty: `then` is the rule wire field name
    const buyRule = { ...newRule(1), then: { ...newAction("buy"), instrument: "" } };
    expect(firstRuleProblem([buyRule])).toBe("Fill in every condition and action field");

    // biome-ignore lint/suspicious/noThenProperty: `then` is the rule wire field name
    const lpRule = { ...newRule(2), then: { ...newAction("open_lp"), yearlyFeePercent: "" } };
    expect(firstRuleProblem([lpRule])).toBe("Fill in every condition and action field");
  });

  it("accepts a valid rule set", () => {
    const rule = {
      ...newRule(1),
      when: appendChild(andTree(), [], newIndicatorCompare()),
      // biome-ignore lint/suspicious/noThenProperty: `then` is the rule wire field name
      then: newAction("sell"),
    };
    expect(firstRuleProblem([rule])).toBeNull();
  });
});
