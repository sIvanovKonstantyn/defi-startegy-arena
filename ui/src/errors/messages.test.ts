import { describe, expect, it } from "vitest";
import { ApiError, toUserErrorMessage } from "./messages";

describe("toUserErrorMessage", () => {
  it("strips http status prefixes", () => {
    expect(toUserErrorMessage(new Error("HTTP 401"))).toBe(
      "Your session is invalid or expired. Please sign in again.",
    );
    expect(toUserErrorMessage(new ApiError({ status: 401, reason: "HTTP 401" }))).toBe(
      "Your session is invalid or expired. Please sign in again.",
    );
  });

  it("keeps semantic reason codes without status numbers", () => {
    expect(toUserErrorMessage(new ApiError({ status: 409, reason: "DUPLICATE" }))).toBe(
      "DUPLICATE",
    );
  });

  it("maps bare status codes to friendly copy", () => {
    expect(toUserErrorMessage(new ApiError({ status: 500, reason: "500" }))).toBe(
      "Something went wrong on the server. Try again shortly.",
    );
  });
});
