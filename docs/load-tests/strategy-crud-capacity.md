# Strategy CRUD capacity (combined flows)

## Purpose

Measure the highest sustained **combined-flow RPS** (create → get one → list → update) that the Compose stack can hold with:

- peak container CPU ≤ **80% of each service CPU quota** (docker stats %; e.g. app 1 CPU → 80%, db 4 CPU → 320%)
- **zero** HTTP failures / failed checks / flow errors
- containers stay up (**no OOM / crash**)
- latency percentiles recorded for HTTP requests and full combined flows

## Run metadata

- Generated (UTC): `2026-09-16 11:39:32Z`
- Base URL: `http://host.docker.internal:8080`
- Owner: `load-owner`
- Stage duration: `45s`
- RPS ladder: `100 125 150`
- CPU limit: `80%` of container CPU quota
- Tooling: `scripts/loadtest/run-capacity.sh` + k6 `strategy-crud.js`
- Profile: fresh DB (`FRESH_DB=1`), warm-up 20 RPS / 20s, laptop Compose (`app` 0.75 / `db` 1.25)

## Verdict

- **Highest graceful combined-flow RPS:** `150`
- First failing step: `n/a`

## Stage results

| Flow RPS | Pass | Peak app CPU | App budget | Peak db CPU | Db budget | HTTP fail rate | Checks rate | Flow errors | Reason |
| ---: | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 100 | yes | 15.5% | 60.0% | 17.5% | 100.0% | 0.0000 | 1.0000 | 0.0 | graceful |
| 125 | yes | 12.0% | 60.0% | 24.6% | 100.0% | 0.0000 | 1.0000 | 0.0 | graceful |
| 150 | yes | 12.1% | 60.0% | 58.1% | 100.0% | 0.0000 | 1.0000 | 0.0 | graceful |

## Latency (HTTP request duration, ms)

Per-request latency across create/get/list/update calls (`http_req_duration`).

| Flow RPS | avg | p50 | p90 | p95 | p99 | max |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 0.76 | 0.59 | 1.30 | 1.59 | 3.75 | 29.34 |
| 125 | 0.79 | 0.44 | 1.85 | 2.10 | 3.24 | 42.89 |
| 150 | 1.03 | 0.39 | 2.60 | 3.36 | 5.67 | 52.53 |

## Latency (combined flow duration, ms)

End-to-end time for one combined iteration (create → get one → list → update) (`flow_duration`).

| Flow RPS | avg | p50 | p90 | p95 | p99 | max |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 3.40 | 3.00 | 5.00 | 6.00 | 13.00 | 62.00 |
| 125 | 3.45 | 3.00 | 4.00 | 5.00 | 10.00 | 62.00 |
| 150 | 4.38 | 3.00 | 6.00 | 7.00 | 20.00 | 95.00 |

## Combined flow under test

Each counted flow RPS iteration performs:

1. `POST /strategies` (create)
2. `GET /strategies/{id}?ownerId=…` (get one)
3. `GET /strategies?ownerId=…` (list)
4. `PUT /strategies/{id}?ownerId=…` (update)

So HTTP request rate ≈ **4 × combined-flow RPS** at steady state.

## Reproduce

```bash
FRESH_DB=1 ./scripts/loadtest/run-capacity.sh
```

Artifacts under `build/loadtest/` (gitignored). This report is written to `docs/load-tests/strategy-crud-capacity.md`.
