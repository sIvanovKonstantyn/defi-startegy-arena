#!/usr/bin/env bash
# Capacity probe for combined strategy CRUD flows (create → get one → list → update).
# Finds the highest sustained flow RPS with: zero HTTP/check errors, containers alive
# (no OOM/exit), and docker CPU for app+db each <= CPU_LIMIT_PCT (default 80).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

BASE_URL="${BASE_URL:-http://host.docker.internal:8080}"
PROBE_URL="${PROBE_URL:-http://localhost:8080}"
OWNER="${OWNER:-load-owner}"
DURATION="${DURATION:-45s}"
CPU_LIMIT_PCT="${CPU_LIMIT_PCT:-80}"
K6_IMAGE="${K6_IMAGE:-grafana/k6:0.54.0}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
# Optional second file, e.g. "docker-compose.loadtest.yml" for app=1 / db=4 CPU profile.
COMPOSE_LOADTEST_FILE="${COMPOSE_LOADTEST_FILE:-}"
REPORT_DIR="${REPORT_DIR:-docs/load-tests}"
REPORT_FILE="${REPORT_FILE:-${REPORT_DIR}/strategy-crud-capacity.md}"
RESULTS_DIR="${RESULTS_DIR:-build/loadtest}"
RPS_LADDER="${RPS_LADDER:-5 10 15 20 25 30 40 50 60 75 100}"
SKIP_COMPOSE_BUILD="${SKIP_COMPOSE_BUILD:-0}"
FRESH_DB="${FRESH_DB:-0}"
WARMUP_RPS="${WARMUP_RPS:-20}"
WARMUP_DURATION="${WARMUP_DURATION:-20s}"
# Ignore early docker-stats samples per stage (JVM / connection warm-up).
CPU_WARMUP_SAMPLES="${CPU_WARMUP_SAMPLES:-5}"

APP_SERVICE=app
DB_SERVICE=db

RESULTS_DIR="$(mkdir -p "$RESULTS_DIR" && cd "$RESULTS_DIR" && pwd)"
REPORT_DIR="$(mkdir -p "$REPORT_DIR" && cd "$REPORT_DIR" && pwd)"
if [[ "$REPORT_FILE" != /* ]]; then
  REPORT_FILE="${ROOT_DIR}/${REPORT_FILE}"
fi

log() { printf '[loadtest] %s\n' "$*"; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "missing required command: $1" >&2
    exit 1
  }
}

require_cmd docker
require_cmd python3
require_cmd jq
require_cmd curl

compose() {
  if [[ -n "$COMPOSE_LOADTEST_FILE" ]]; then
    docker compose -f "$COMPOSE_FILE" -f "$COMPOSE_LOADTEST_FILE" "$@"
  else
    docker compose -f "$COMPOSE_FILE" "$@"
  fi
}

container_id() {
  compose ps -q "$1"
}

container_running() {
  local id
  id="$(container_id "$1")"
  [[ -n "$id" ]] || return 1
  [[ "$(docker inspect -f '{{.State.Running}}' "$id")" == "true" ]]
}

sample_cpu_pct() {
  local id="$1"
  docker stats --no-stream --format '{{.CPUPerc}}' "$id" | tr -d '%'
}

# docker stats CPU% is relative to host cores; scale the 80% budget by container CPU quota.
container_cpu_budget_pct() {
  local id="$1"
  python3 - "$id" "$CPU_LIMIT_PCT" <<'PY'
import json, subprocess, sys
container_id, limit_pct = sys.argv[1], float(sys.argv[2])
inspect = json.loads(
    subprocess.check_output(["docker", "inspect", container_id], text=True)
)[0]
host = inspect["HostConfig"]
nano = host.get("NanoCpus") or 0
if nano:
    cpus = nano / 1_000_000_000
else:
    quota = host.get("CpuQuota") or 0
    period = host.get("CpuPeriod") or 100000
    cpus = (quota / period) if quota > 0 else 1.0
print(limit_pct * cpus)
PY
}

wait_http_ready() {
  local url="$1"
  local attempts=90
  local i
  for ((i = 1; i <= attempts; i++)); do
    if curl -fsS "${url}/strategies?ownerId=${OWNER}&page=0&size=1" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "API not ready at ${url}" >&2
  return 1
}

ensure_stack() {
  if [[ "$FRESH_DB" == "1" ]]; then
    log "resetting compose stack for fresh database"
    compose down -v --remove-orphans
  fi
  log "starting compose stack"
  if [[ "$SKIP_COMPOSE_BUILD" == "1" ]]; then
    compose up -d
  else
    compose up -d --build
  fi
  wait_http_ready "$PROBE_URL"
  container_running "$APP_SERVICE"
  container_running "$DB_SERVICE"
}

peak_cpu_column() {
  local cpu_log="$1"
  local column="$2"
  local skip_samples="${3:-0}"
  python3 - "$cpu_log" "$column" "$skip_samples" <<'PY'
import sys
path, col, skip = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
vals = []
for index, line in enumerate(open(path, encoding="utf-8")):
    if index < skip:
        continue
    parts = line.strip().split("\t")
    if len(parts) == 3:
        vals.append(float(parts[col]))
print(max(vals) if vals else 0.0)
PY
}

extract_k6_summary() {
  local raw="$1"
  local out="$2"
  if [[ -s "$out" ]]; then
    return 0
  fi
  # Fallback: last complete JSON object in mixed stdout (legacy)
  python3 - "$raw" "$out" <<'PY'
import sys
raw = open(sys.argv[1], encoding="utf-8").read()
decoder = __import__("json").JSONDecoder()
best = None
for i, ch in enumerate(raw):
    if ch != "{":
        continue
    try:
        obj, _ = decoder.raw_decode(raw, i)
    except Exception:
        continue
    if isinstance(obj, dict) and "metrics" in obj:
        best = obj
if best is None:
    open(sys.argv[2], "w", encoding="utf-8").write("{}\n")
else:
    open(sys.argv[2], "w", encoding="utf-8").write(
        __import__("json").dumps(best) + "\n"
    )
PY
}

export_stage() {
  local rps="$1"
  local passed_bool="$2"
  local reason="$3"
  local k6_rc="$4"
  local app_alive_bool="$5"
  local db_alive_bool="$6"
  local peak_app="$7"
  local peak_db="$8"
  local http_fail="$9"
  local checks="${10}"
  local flow_errors="${11}"
  local out_path="${12}"
  local k6_summary_path="${13}"
  STAGE_REASON="$reason" STAGE_PASSED="$passed_bool" STAGE_APP_ALIVE="$app_alive_bool" \
    STAGE_DB_ALIVE="$db_alive_bool" python3 - "$out_path" "$k6_summary_path" "$rps" "$k6_rc" \
    "$peak_app" "$peak_db" "$http_fail" "$checks" "$flow_errors" <<'PY'
import json, os, sys

def trend_ms(metrics, name):
    values = metrics.get(name, {}).get("values", {})
    return {
        "avg_ms": float(values.get("avg", 0) or 0),
        "p50_ms": float(values.get("med", 0) or 0),
        "p90_ms": float(values.get("p(90)", 0) or 0),
        "p95_ms": float(values.get("p(95)", 0) or 0),
        "p99_ms": float(values.get("p(99)", 0) or 0),
        "max_ms": float(values.get("max", 0) or 0),
    }

path, k6_path, rps, k6_rc, peak_app, peak_db, http_fail, checks, flow_errors = sys.argv[1:10]
metrics = {}
try:
    metrics = json.loads(open(k6_path, encoding="utf-8").read()).get("metrics", {})
except Exception:
    metrics = {}

doc = {
  "rps": int(rps),
  "passed": os.environ["STAGE_PASSED"] == "true",
  "reason": os.environ["STAGE_REASON"],
  "k6_exit_code": int(k6_rc),
  "app_alive": os.environ["STAGE_APP_ALIVE"] == "true",
  "db_alive": os.environ["STAGE_DB_ALIVE"] == "true",
  "peak_app_cpu_pct": float(peak_app),
  "peak_db_cpu_pct": float(peak_db),
  "app_cpu_budget_pct": float(os.environ.get("STAGE_APP_BUDGET", "80")),
  "db_cpu_budget_pct": float(os.environ.get("STAGE_DB_BUDGET", "80")),
  "http_req_failed_rate": float(http_fail),
  "checks_rate": float(checks),
  "flow_errors": float(flow_errors),
  "duration": os.environ.get("STAGE_DURATION", os.environ.get("DURATION", "")),
  "cpu_limit_pct": float(os.environ.get("STAGE_CPU_LIMIT_PCT", os.environ.get("CPU_LIMIT_PCT", "80"))),
  "http_req_duration": trend_ms(metrics, "http_req_duration"),
  "flow_duration": trend_ms(metrics, "flow_duration"),
}
open(path, "w", encoding="utf-8").write(json.dumps(doc, indent=2) + "\n")
print(json.dumps(doc))
PY
}

run_stage() {
  local rps="$1"
  local stage_dir
  stage_dir="$(cd "$RESULTS_DIR" && pwd)/rps-${rps}"
  mkdir -p "$stage_dir"
  local app_id db_id
  app_id="$(container_id "$APP_SERVICE")"
  db_id="$(container_id "$DB_SERVICE")"

  local cpu_log="${stage_dir}/cpu.tsv"
  : >"$cpu_log"
  local sampler_pid
  (
    while true; do
      local app_cpu db_cpu ts
      ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
      app_cpu="$(sample_cpu_pct "$app_id")"
      db_cpu="$(sample_cpu_pct "$db_id")"
      printf '%s\t%s\t%s\n' "$ts" "$app_cpu" "$db_cpu" >>"$cpu_log"
      sleep 2
    done
  ) &
  sampler_pid=$!

  local k6_rc=0
  local k6_out="${stage_dir}/k6-summary.json"
  rm -f "$k6_out"
  set +e
  docker run --rm \
    --add-host=host.docker.internal:host-gateway \
    -v "${ROOT_DIR}/scripts/loadtest:/scripts:ro" \
    -v "${stage_dir}:/out" \
    -e BASE_URL="$BASE_URL" \
    -e OWNER="$OWNER" \
    -e TARGET_RPS="$rps" \
    -e DURATION="$DURATION" \
    -e SUMMARY_PATH=/out/k6-summary.json \
    "$K6_IMAGE" run /scripts/strategy-crud.js \
    >"${stage_dir}/k6-raw.json" 2>"${stage_dir}/k6-stderr.log"
  k6_rc=$?
  set -e

  kill "$sampler_pid" 2>/dev/null || true
  wait "$sampler_pid" 2>/dev/null || true

  extract_k6_summary "${stage_dir}/k6-raw.json" "$k6_out"

  local app_alive=true
  local db_alive=true
  container_running "$APP_SERVICE" || app_alive=false
  container_running "$DB_SERVICE" || db_alive=false

  local peak_app peak_db
  peak_app="$(peak_cpu_column "$cpu_log" 1 "$CPU_WARMUP_SAMPLES")"
  peak_db="$(peak_cpu_column "$cpu_log" 2 "$CPU_WARMUP_SAMPLES")"
  local app_budget db_budget
  app_budget="$(container_cpu_budget_pct "$app_id")"
  db_budget="$(container_cpu_budget_pct "$db_id")"

  local http_fail_rate checks_rate flow_error_count
  http_fail_rate="$(jq -r '.metrics.http_req_failed.values.rate // 1' "$k6_out")"
  checks_rate="$(jq -r '.metrics.checks.values.rate // 0' "$k6_out")"
  flow_error_count="$(jq -r '.metrics.flow_errors.values.count // 0' "$k6_out")"

  local reasons=()
  local passed=true

  if [[ "$k6_rc" -ne 0 ]]; then
    passed=false
    reasons+=("k6 exited with code ${k6_rc}")
  fi
  if [[ "$app_alive" != true ]]; then
    passed=false
    reasons+=("app container not running (possible OOM/crash)")
  fi
  if [[ "$db_alive" != true ]]; then
    passed=false
    reasons+=("db container not running")
  fi
  if ! python3 -c "raise SystemExit(0 if float('${http_fail_rate}') == 0 else 1)"; then
    passed=false
    reasons+=("http_req_failed rate=${http_fail_rate}")
  fi
  if ! python3 -c "raise SystemExit(0 if float('${checks_rate}') == 1 else 1)"; then
    passed=false
    reasons+=("checks rate=${checks_rate}")
  fi
  if ! python3 -c "raise SystemExit(0 if float('${flow_error_count}') == 0 else 1)"; then
    passed=false
    reasons+=("flow_errors=${flow_error_count}")
  fi
  if python3 -c "raise SystemExit(0 if float('${peak_app}') > float('${app_budget}') else 1)"; then
    passed=false
    reasons+=("app peak CPU ${peak_app}% > budget ${app_budget}% (${CPU_LIMIT_PCT}% of cpus)")
  fi
  if python3 -c "raise SystemExit(0 if float('${peak_db}') > float('${db_budget}') else 1)"; then
    passed=false
    reasons+=("db peak CPU ${peak_db}% > budget ${db_budget}% (${CPU_LIMIT_PCT}% of cpus)")
  fi

  local reason
  if [[ "$passed" == true ]]; then
    reason="graceful"
  else
    local IFS='; '
    reason="${reasons[*]}"
  fi

  STAGE_DURATION="$DURATION" STAGE_CPU_LIMIT_PCT="$CPU_LIMIT_PCT" \
  STAGE_APP_BUDGET="$app_budget" STAGE_DB_BUDGET="$db_budget" \
    export_stage "$rps" "$passed" "$reason" "$k6_rc" "$app_alive" "$db_alive" \
    "$peak_app" "$peak_db" "$http_fail_rate" "$checks_rate" "$flow_error_count" \
    "${stage_dir}/stage.json" "$k6_out" >/dev/null

  [[ "$passed" == true ]]
}

ensure_stack

# Clear previous stage folders for a clean report
rm -rf "${RESULTS_DIR}/rps-"*
mkdir -p "$RESULTS_DIR"

if [[ "$WARMUP_RPS" != "0" ]]; then
  log "warm-up at combined-flow RPS=${WARMUP_RPS} for ${WARMUP_DURATION} (not scored)"
  _saved_duration="$DURATION"
  DURATION="$WARMUP_DURATION"
  run_stage "$WARMUP_RPS" || true
  DURATION="$_saved_duration"
  unset _saved_duration
  rm -rf "${RESULTS_DIR}/rps-${WARMUP_RPS}"
fi

max_ok=0
first_fail=""

for rps in $RPS_LADDER; do
  log "probing combined-flow RPS=${rps} for ${DURATION}"
  if run_stage "$rps"; then
    max_ok="$rps"
    log "PASS rps=${rps}"
  else
    first_fail="$rps"
    log "FAIL rps=${rps}"
    break
  fi
done

python3 "${ROOT_DIR}/scripts/loadtest/render_report.py" \
  --results-dir "$RESULTS_DIR" \
  --report-file "$REPORT_FILE" \
  --base-url "$BASE_URL" \
  --owner "$OWNER" \
  --duration "$DURATION" \
  --cpu-limit "$CPU_LIMIT_PCT" \
  --max-ok-rps "$max_ok" \
  --first-fail-rps "${first_fail}" \
  --rps-ladder "$RPS_LADDER"

log "report written to ${REPORT_FILE}"
log "highest graceful combined-flow RPS: ${max_ok}"
