# DeFi Strategy Arena UI

React + Vite client for auth and strategy CRUD. Talks to the API via same-origin paths (`/auth`, `/strategies`, `/ws`) — nginx or Vite proxy reaches the Jetty app.

## Scripts

```bash
npm ci
npx playwright install chromium
npm run dev            # proxy to localhost:8080
npm run qualityCheck   # biome + tsc + vitest + build + playwright + audit
```

See [`docs/frontend-quality.md`](../docs/frontend-quality.md) and [`docs/phases/phase-9-web-ui-websocket.md`](../docs/phases/phase-9-web-ui-websocket.md).
