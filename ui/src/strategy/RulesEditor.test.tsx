import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import type { StrategyRule } from "../api/types";
import { RulesEditor } from "./RulesEditor";
import { newRule } from "./ruleModel";

const RULES_JSON = "rules-json";

function Harness() {
  const [rules, setRules] = useState<StrategyRule[]>([newRule(1)]);
  return (
    <>
      <RulesEditor rules={rules} disabled={false} onChange={setRules} />
      <output data-testid={RULES_JSON}>{JSON.stringify(rules)}</output>
    </>
  );
}

function currentRules(): StrategyRule[] {
  return JSON.parse(screen.getByTestId(RULES_JSON).textContent ?? "[]") as StrategyRule[];
}

describe("RulesEditor", () => {
  it("builds an AND tree with a price leaf and an indicator leaf", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.selectOptions(screen.getByLabelText("Condition type"), "and");
    await user.click(screen.getByRole("button", { name: "Add indicator condition" }));

    const [rule] = currentRules();
    expect(rule.when.type).toBe("and");
    expect(rule.when.children).toHaveLength(2);
    expect(rule.when.children?.[0].type).toBe("price_compare");
    expect(rule.when.children?.[1].indicator).toBe("sma");
    expect(rule.when.children?.[1].parameters).toEqual({ period: "14" });
  });

  it("edits indicator parameters and removes a nested condition", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.selectOptions(screen.getByLabelText("Condition type"), "or");
    await user.click(screen.getByRole("button", { name: "Add indicator condition" }));

    const leaf = screen.getByRole("group", { name: "Condition 2" });
    await user.selectOptions(within(leaf).getByLabelText("Indicator"), "bollinger_bands");
    await user.clear(within(leaf).getByLabelText("Std dev"));
    await user.type(within(leaf).getByLabelText("Std dev"), "3");
    await user.selectOptions(within(leaf).getByLabelText("Operator"), "lt");

    const [withIndicator] = currentRules();
    expect(withIndicator.when.children?.[1].parameters).toEqual({ period: "14", stdDev: "3" });
    expect(withIndicator.when.children?.[1].operator).toBe("lt");

    await user.click(
      within(screen.getByRole("group", { name: "Condition 1" })).getByRole("button", {
        name: "Remove condition",
      }),
    );
    const [afterRemove] = currentRules();
    expect(afterRemove.when.children).toHaveLength(1);
    expect(afterRemove.when.children?.[0].indicator).toBe("bollinger_bands");
  });

  it("switches the action and captures LP fields", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.selectOptions(screen.getByLabelText("Action type"), "open_lp");
    await user.clear(screen.getByLabelText("Instrument pair"));
    await user.type(screen.getByLabelText("Instrument pair"), "ETH-USDC");
    await user.clear(screen.getByLabelText("Yearly fee percent"));
    await user.type(screen.getByLabelText("Yearly fee percent"), "18");

    const [rule] = currentRules();
    expect(rule.then.type).toBe("open_lp");
    expect(rule.then.instrumentPair).toBe("ETH-USDC");
    expect(rule.then.yearlyFeePercent).toBe("18");
    expect(rule.then.allocationPercent).toBe("25");
  });

  it("adds and removes rules", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(screen.getByRole("button", { name: "Add rule" }));
    expect(currentRules()).toHaveLength(2);
    expect(screen.getByRole("group", { name: "Rule 2" })).toBeInTheDocument();

    await user.click(
      within(screen.getByRole("group", { name: "Rule 1" })).getAllByRole("button", {
        name: "Remove rule",
      })[0],
    );
    const remaining = currentRules();
    expect(remaining).toHaveLength(1);
    expect(remaining[0].id).toBe("r2");
  });
});
