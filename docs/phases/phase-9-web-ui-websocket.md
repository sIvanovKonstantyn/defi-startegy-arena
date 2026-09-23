# Phase 9 — React UI + session WebSocket delivery

## Goal

Close the Phase 8 async gap: browsers open one authenticated WebSocket after login, and integration listeners push strategy `*Completed` / `*Failed` envelopes to that user’s sockets. Ship a Vite/React UI in a separate Compose `ui` service (nginx) that proxies `/auth`, `/strategies`, and `/ws` to the app.

## Delivered

- `ownerId` on strategy response events; delivery listeners map them to WS envelopes
- Jetty `/ws` under `shared.infra` with first-frame Bearer auth via `GetCurrentUser`
- `UserSessionHub` + `StrategyResponseDeliveryListeners` in `integration`
- `ui/` React MVP: signup/login, strategy list/create/edit/delete, session WS correlation matching
- Compose `ui` on port `3000`; FE `npm run qualityCheck` (Biome, `tsc`, Vitest, Playwright, `npm audit`)

## Known limits

- Identity sessions remain in-memory — Compose/app restart logs everyone out
- Socket hub is single-node (in-process)
- No marketdata/arena UI; no response HTTP poll API; no outbox background poller (still in-request `drain()`)

## How to run

```bash
docker compose up --build
# open http://localhost:3000
```

Local UI against a host app:

```bash
cd ui && npm run dev   # proxies to localhost:8080
```

## Docs

- Flow: [strategy-response-websocket.md](../flows/strategy-response-websocket.md)
- Frontend gates: [frontend-quality.md](../frontend-quality.md)
