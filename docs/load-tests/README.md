# Strategy CRUD load tests

## Goal

Find the highest **combined-flow RPS** (create → get one → list → update) that Compose can sustain with:

- peak `app` / `db` CPU ≤ **80% of each service CPU quota** (docker stats; app≈80%, db≈320% on 4 CPUs)
- **no** HTTP/check/flow errors
- containers stay up (**no OOM**)

## Prerequisites

- Docker / Rancher Desktop
- `curl`, `jq`, `python3`
- Compose memory limits: app heap 2g / container 2.5g, Postgres 2g, Hikari 10
- CPU: default compose is laptop-safe (`app` 0.75 / `db` 1.25). On a dedicated load-test host (≥5 CPUs) use `docker-compose.loadtest.yml` (`app` 1.0 / `db` 4.0).

## Run

```bash
# Full ladder (default stages ~45s each)
./scripts/loadtest/run-capacity.sh

# Dedicated host CPU profile (app 1 / db 4)
COMPOSE_LOADTEST_FILE=docker-compose.loadtest.yml ./scripts/loadtest/run-capacity.sh

# Faster probe
DURATION=20s RPS_LADDER="5 10 15 20 25 30" ./scripts/loadtest/run-capacity.sh
```

The script:

1. `docker compose up -d --build` and waits for HTTP readiness
2. Runs k6 (`grafana/k6`) at each RPS on the ladder
3. Samples `docker stats` CPU for `app` / `db`
4. Stops at the first failing stage
5. Writes **`docs/load-tests/strategy-crud-capacity.md`**

Raw artifacts land in `build/loadtest/` (gitignored).

## Report

Commit the generated Markdown under `docs/load-tests/` after a representative run on the target machine.
