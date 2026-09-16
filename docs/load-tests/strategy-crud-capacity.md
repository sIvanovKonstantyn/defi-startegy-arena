# Strategy CRUD capacity (combined flows)

## Purpose

Measure the highest sustained **combined-flow RPS** (create → get one → list → update) that the Compose stack can hold with:

- peak container CPU ≤ **80% of each service CPU quota** (docker stats %; e.g. app 1 CPU → 80%, db 4 CPU → 320%)
- **zero** HTTP failures / failed checks / flow errors
- containers stay up (**no OOM / crash**)

## Run metadata

- Generated (UTC): `2026-09-16 10:13:52Z`
- Base URL: `http://host.docker.internal:8080`
- Owner: `load-owner`
- Stage duration: `45s`
- RPS ladder: `100 125 150 175 200`
- CPU limit: `80%` of container CPU quota
- Tooling: `scripts/loadtest/run-capacity.sh` + k6 `strategy-crud.js`
- Profile: **2-CPU laptop Compose** (`app` 0.75 / `db` 1.25). For app=1 / db=4 use `COMPOSE_LOADTEST_FILE=docker-compose.loadtest.yml`.

## Verdict

- **Highest graceful combined-flow RPS:** `125`
- First failing step: `150`

## Stage results

| Flow RPS | Pass | Peak app CPU | App budget | Peak db CPU | Db budget | HTTP fail rate | Checks rate | Flow errors | Reason |
| ---: | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 100 | yes | 12.6% | 60.0% | 68.7% | 100.0% | 0.0000 | 1.0000 | 0.0 | graceful |
| 125 | yes | 10.3% | 60.0% | 88.2% | 100.0% | 0.0000 | 1.0000 | 0.0 | graceful |
| 150 | no | 9.6% | 60.0% | 112.4% | 100.0% | 0.0000 | 1.0000 | 0.0 | db peak CPU 112.4% > budget 100.0% (80% of cpus) |

## Combined flow under test

Each counted flow RPS iteration performs:

1. `POST /strategies` (create)
2. `GET /strategies/{id}?ownerId=…` (get one)
3. `GET /strategies?ownerId=…` (list)
4. `PUT /strategies/{id}?ownerId=…` (update)

So HTTP request rate ≈ **4 × combined-flow RPS** at steady state.

## Reproduce

```bash
docker compose up -d --build
./scripts/loadtest/run-capacity.sh
```

Artifacts under `build/loadtest/` (gitignored). This report is written to `docs/load-tests/strategy-crud-capacity.md`.
