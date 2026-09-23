import { describe, expect, it } from "vitest";
import { websocketUrl } from "../api/sessionSocket";
import { clearAccessToken, readAccessToken, writeAccessToken } from "./tokenStore";

describe("tokenStore", () => {
  it("stores and clears access tokens", () => {
    clearAccessToken();
    expect(readAccessToken()).toBeNull();
    writeAccessToken("abc");
    expect(readAccessToken()).toBe("abc");
    clearAccessToken();
    expect(readAccessToken()).toBeNull();
  });
});

describe("websocketUrl", () => {
  it("uses location host and ws scheme", () => {
    expect(websocketUrl()).toMatch(/^ws:\/\/.+\/ws$/);
  });
});
