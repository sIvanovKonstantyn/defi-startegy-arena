# Frontend quality

The `ui/` package mirrors backend quality gates with Node tooling.

| Concern | Tool | Backend analogue |
| --- | --- | --- |
| App + bundler | Vite + React + TypeScript | Gradle / Java |
| Lint + format | Biome | PMD + style |
| Types | `tsc -b` | javac |
| Unit / component | Vitest + Testing Library | JUnit |
| E2E | Playwright (hermetic route + WS fakes) | Jetty HTTP e2e |
| Dep CVEs | `npm audit --audit-level=high` | OSV-Scanner |
| Aggregate | `npm run qualityCheck` | `./gradlew qualityCheck` |

## Commands

```bash
cd ui
npm ci
npx playwright install chromium   # once per machine
npm run qualityCheck
```

## Hooks

`scripts/git-hooks/pre-commit` runs `./gradlew qualityCheck`, then when staged paths include `ui/`, runs `cd ui && npm run qualityCheck`.

## Local vs Compose

- **Dev:** `npm run dev` proxies `/auth`, `/strategies`, `/ws` to `localhost:8080`.
- **Compose:** nginx in the `ui` image proxies those paths to `app:8080`; browsers use `http://HOST:3000` only (relative URLs + `window.location` for WS).
