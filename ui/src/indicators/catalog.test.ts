import { describe, expect, it } from "vitest";
import {
  DEFAULT_INDICATOR_ID,
  findIndicator,
  INDICATOR_CATALOG,
  indicatorDefaults,
  indicatorLabel,
  indicatorParameters,
} from "./catalog";

describe("indicator catalog", () => {
  it("mirrors the backend catalog ids", () => {
    expect(INDICATOR_CATALOG.map((indicator) => indicator.id)).toEqual([
      "sma",
      "ema",
      "rsi",
      "bollinger_bands",
      "macd",
    ]);
    expect(DEFAULT_INDICATOR_ID).toBe("sma");
  });

  it("exposes parameter names per indicator", () => {
    expect(indicatorParameters("rsi").map((parameter) => parameter.name)).toEqual(["period"]);
    expect(indicatorParameters("bollinger_bands").map((parameter) => parameter.name)).toEqual([
      "period",
      "stdDev",
    ]);
    expect(indicatorParameters("macd").map((parameter) => parameter.name)).toEqual([
      "fast",
      "slow",
      "signal",
    ]);
  });

  it("returns defaults matching the backend", () => {
    expect(indicatorDefaults("sma")).toEqual({ period: "14" });
    expect(indicatorDefaults("bollinger_bands")).toEqual({ period: "14", stdDev: "2" });
    expect(indicatorDefaults("macd")).toEqual({ fast: "12", slow: "26", signal: "9" });
  });

  it("handles unknown indicators", () => {
    expect(findIndicator("nope")).toBeUndefined();
    expect(indicatorParameters("nope")).toEqual([]);
    expect(indicatorDefaults("nope")).toEqual({});
    expect(indicatorLabel("nope")).toBe("nope");
    expect(indicatorLabel("ema")).toBe("EMA");
  });
});
