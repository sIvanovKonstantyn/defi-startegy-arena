import type { WsEnvelope } from "./types";

export type SessionSocketHandlers = {
  onEnvelope: (envelope: WsEnvelope) => void;
  onAuthOk: () => void;
  onAuthFailed: () => void;
  onClose: () => void;
};

export function websocketUrl(): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws`;
}

export class SessionSocket {
  private socket: WebSocket | null = null;
  private readonly handlers: SessionSocketHandlers;
  private generation = 0;

  constructor(handlers: SessionSocketHandlers) {
    this.handlers = handlers;
  }

  connect(accessToken: string): void {
    this.close();
    const generation = this.generation + 1;
    this.generation = generation;
    const socket = new WebSocket(websocketUrl());
    this.socket = socket;
    socket.addEventListener("open", () => {
      if (this.generation !== generation) {
        return;
      }
      socket.send(JSON.stringify({ type: "auth", accessToken }));
    });
    socket.addEventListener("message", (event) => {
      if (this.generation !== generation) {
        return;
      }
      this.onMessage(String(event.data));
    });
    socket.addEventListener("close", () => {
      if (this.generation !== generation) {
        return;
      }
      this.handlers.onClose();
    });
  }

  close(): void {
    this.generation += 1;
    if (this.socket !== null) {
      const closing = this.socket;
      this.socket = null;
      closing.close();
    }
  }

  private onMessage(raw: string): void {
    let parsed: Record<string, unknown>;
    try {
      parsed = JSON.parse(raw) as Record<string, unknown>;
    } catch {
      return;
    }
    if (parsed.type === "auth") {
      if (parsed.status === "ok") {
        this.handlers.onAuthOk();
      } else {
        this.handlers.onAuthFailed();
      }
      return;
    }
    if (
      typeof parsed.correlationId === "string" &&
      typeof parsed.type === "string" &&
      (parsed.status === "completed" || parsed.status === "failed") &&
      typeof parsed.payload === "object" &&
      parsed.payload !== null
    ) {
      this.handlers.onEnvelope(parsed as WsEnvelope);
    }
  }
}
