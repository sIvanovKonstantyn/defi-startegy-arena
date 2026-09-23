import { useState } from "react";
import { IconAuthLogin, IconAuthSignup, IconButton, IconSave } from "./icons/IconSet";

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
    <section className="panel auth-panel">
      <div className="mode-toggle">
        <IconButton
          label="Switch to log in"
          className={mode === "login" ? "active" : "button-secondary"}
          onClick={() => setMode("login")}
        >
          <IconAuthLogin />
        </IconButton>
        <IconButton
          label="Switch to sign up"
          className={mode === "signup" ? "active" : "button-secondary"}
          onClick={() => setMode("signup")}
        >
          <IconAuthSignup />
        </IconButton>
      </div>
      <label>
        Email
        <input
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete="email"
        />
      </label>
      <label>
        Password
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete={mode === "login" ? "current-password" : "new-password"}
        />
      </label>
      {mode === "signup" ? (
        <label>
          Display name
          <input
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            autoComplete="nickname"
          />
        </label>
      ) : null}
      <div className="row">
        <IconButton
          label={mode === "login" ? "Log in" : "Create account"}
          disabled={busy}
          onClick={() => void submit()}
        >
          <IconSave />
        </IconButton>
      </div>
    </section>
  );
}
