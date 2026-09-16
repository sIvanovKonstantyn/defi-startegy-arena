# Strategy CRUD combined-flow load test

## Purpose

Probe Compose-backed Postgres persistence under a mixed strategy HTTP workload to find the highest sustained combined-flow RPS that stays within CPU and error budgets.

## Actors

- `scripts/loadtest/run-capacity.sh` (orchestrator)
- k6 (`strategy-crud.js`) via Docker image
- `app` + `db` Compose services
- `docs/load-tests/strategy-crud-capacity.md` (generated report)

## Sequence

```mermaid
sequenceDiagram
  participant Runner as run-capacity.sh
  participant Compose as docker compose
  participant K6 as k6 container
  participant App as app
  participant Db as db
  Runner->>Compose: up --build + health wait
  loop RPS ladder
    Runner->>K6: constant-arrival-rate TARGET_RPS
    par Combined flow
      K6->>App: POST /strategies
      K6->>App: GET /strategies/{id}
      K6->>App: GET /strategies
      K6->>App: PUT /strategies/{id}
    and CPU sample
      Runner->>Compose: docker stats app/db
    end
    App->>Db: jOOQ CRUD
    Runner->>Runner: pass if CPU<=80%, 0 errors, containers up
  end
  Runner->>Runner: write Markdown capacity report
```

## Walkthrough

1. Stack starts with load-test resource limits (2g heap / 2.5g app container; Postgres 2g / 4 CPU).
2. Each ladder step holds a fixed combined-flow arrival rate for `DURATION`.
3. One flow = create + get one + list + update (≈ 4 HTTP calls).
4. Stage fails on any HTTP/check/flow error, container exit (OOM), or peak CPU above **80% of that service’s CPU quota**.
5. Report records per-stage peaks, HTTP/flow latency percentiles (p50/p90/p95/p99), and the highest graceful RPS.

## Errors / edge cases

- API not ready after compose up → runner exits before probing.
- k6 cannot reach host (`host.docker.internal`) → all stages fail; set `BASE_URL` / networking for the environment.
- Duplicate strategy names are avoided with VU/iter/timestamp suffixes.
