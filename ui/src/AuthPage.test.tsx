import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AuthPage } from "./AuthPage";

describe("AuthPage", () => {
  it("submits login credentials", async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn().mockResolvedValue(undefined);
    const onSignup = vi.fn();
    const onError = vi.fn();
    render(<AuthPage onLogin={onLogin} onSignup={onSignup} onError={onError} />);
    await user.type(screen.getByLabelText("Email"), "a@b.com");
    await user.type(screen.getByLabelText("Password"), "secret-value");
    await user.click(screen.getByRole("button", { name: /^Log in$/ }));
    expect(onLogin).toHaveBeenCalledWith("a@b.com", "secret-value");
  });
});
