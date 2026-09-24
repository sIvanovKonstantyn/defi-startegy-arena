import { useState } from "react";
import { Button } from "./ui/Button";
import { InputField } from "./ui/InputField";

type AuthPageProps = {
  onLogin: (email: string, password: string) => Promise<void>;
  onSignup: (email: string, password: string, displayName: string) => Promise<void>;
  onError: (error: unknown) => void;
};

export function AuthPage(props: AuthPageProps) {
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    try {
      if (mode === "login") {
        await props.onLogin(email, password);
      } else {
        await props.onSignup(email, password, displayName);
      }
    } catch (err) {
      props.onError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="panel auth-panel" aria-label="Authentication">
      <div className="mode-toggle" role="tablist" aria-label="Authentication mode">
        <button
          type="button"
          role="tab"
          className={`btn btn-secondary${mode === "login" ? " active" : ""}`}
          aria-selected={mode === "login"}
          onClick={() => setMode("login")}
        >
          Sign in
        </button>
        <button
          type="button"
          role="tab"
          className={`btn btn-secondary${mode === "signup" ? " active" : ""}`}
          aria-selected={mode === "signup"}
          onClick={() => setMode("signup")}
        >
          Create account
        </button>
      </div>
      <InputField
        id="auth-email"
        label="Email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        autoComplete="email"
      />
      <InputField
        id="auth-password"
        label="Password"
        type="password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        autoComplete={mode === "login" ? "current-password" : "new-password"}
      />
      {mode === "signup" ? (
        <InputField
          id="auth-display-name"
          label="Display name"
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          autoComplete="nickname"
        />
      ) : null}
      <div className="row">
        <Button variant="primary" disabled={busy} onClick={() => void submit()}>
          {mode === "login" ? "Sign in" : "Create account"}
        </Button>
      </div>
    </section>
  );
}
