#!/usr/bin/env python3
"""Render markdown capacity report from load-test stage artifacts."""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path


def load_stages(results_dir: Path) -> list[dict]:
    stages: list[dict] = []
    for path in results_dir.glob("rps-*/stage.json"):
        stages.append(json.loads(path.read_text(encoding="utf-8")))
    stages.sort(key=lambda stage: int(stage.get("rps", 0)))
    return stages


def fmt_pct(value: float) -> str:
    return f"{value:.1f}%"


def fmt_rate(value: float) -> str:
    return f"{value:.4f}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--results-dir", required=True)
    parser.add_argument("--report-file", required=True)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--owner", required=True)
    parser.add_argument("--duration", required=True)
    parser.add_argument("--cpu-limit", required=True, type=float)
    parser.add_argument("--max-ok-rps", required=True, type=int)
    parser.add_argument("--first-fail-rps", default="")
    parser.add_argument("--rps-ladder", required=True)
    args = parser.parse_args()

    results_dir = Path(args.results_dir)
    stages = load_stages(results_dir)
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")
    first_fail = args.first_fail_rps.strip() or "n/a"

    lines: list[str] = [
        "# Strategy CRUD capacity (combined flows)",
        "",
        "## Purpose",
        "",
        "Measure the highest sustained **combined-flow RPS** "
        "(create → get one → list → update) that the Compose stack can hold with:",
        "",
        f"- peak container CPU ≤ **{args.cpu_limit:.0f}% of each service CPU quota** "
        f"(docker stats %; e.g. app 1 CPU → {args.cpu_limit:.0f}%, db 4 CPU → {args.cpu_limit * 4:.0f}%)",
        "- **zero** HTTP failures / failed checks / flow errors",
        "- containers stay up (**no OOM / crash**)",
        "",
        "## Run metadata",
        "",
        f"- Generated (UTC): `{generated_at}`",
        f"- Base URL: `{args.base_url}`",
        f"- Owner: `{args.owner}`",
        f"- Stage duration: `{args.duration}`",
        f"- RPS ladder: `{args.rps_ladder}`",
        f"- CPU limit: `{args.cpu_limit:.0f}%` of container CPU quota",
        f"- Tooling: `scripts/loadtest/run-capacity.sh` + k6 `strategy-crud.js`",
        "",
        "## Verdict",
        "",
        f"- **Highest graceful combined-flow RPS:** `{args.max_ok_rps}`",
        f"- First failing step: `{first_fail}`",
        "",
        "## Stage results",
        "",
        "| Flow RPS | Pass | Peak app CPU | App budget | Peak db CPU | Db budget | HTTP fail rate | Checks rate | Flow errors | Reason |",
        "| ---: | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
    ]

    for stage in stages:
        lines.append(
            "| {rps} | {passed} | {app} | {app_b} | {db} | {db_b} | {http_fail} | {checks} | {flow_err} | {reason} |".format(
                rps=stage.get("rps"),
                passed="yes" if stage.get("passed") else "no",
                app=fmt_pct(float(stage.get("peak_app_cpu_pct", 0))),
                app_b=fmt_pct(float(stage.get("app_cpu_budget_pct", args.cpu_limit))),
                db=fmt_pct(float(stage.get("peak_db_cpu_pct", 0))),
                db_b=fmt_pct(float(stage.get("db_cpu_budget_pct", args.cpu_limit * 4))),
                http_fail=fmt_rate(float(stage.get("http_req_failed_rate", 1))),
                checks=fmt_rate(float(stage.get("checks_rate", 0))),
                flow_err=stage.get("flow_errors", 0),
                reason=str(stage.get("reason", "")).replace("|", "/"),
            )
        )

    if not stages:
        lines.append("| — | — | — | — | — | — | — | — | — | no stages recorded |")

    lines.extend(
        [
            "",
            "## Combined flow under test",
            "",
            "Each counted flow RPS iteration performs:",
            "",
            "1. `POST /strategies` (create)",
            "2. `GET /strategies/{id}?ownerId=…` (get one)",
            "3. `GET /strategies?ownerId=…` (list)",
            "4. `PUT /strategies/{id}?ownerId=…` (update)",
            "",
            "So HTTP request rate ≈ **4 × combined-flow RPS** at steady state.",
            "",
            "## Reproduce",
            "",
            "```bash",
            "docker compose up -d --build",
            "./scripts/loadtest/run-capacity.sh",
            "```",
            "",
            "Artifacts under `build/loadtest/` (gitignored). This report is written to "
            "`docs/load-tests/strategy-crud-capacity.md`.",
            "",
        ]
    )

    report_path = Path(args.report_file)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text("\n".join(lines), encoding="utf-8")

    summary = {
        "generated_at_utc": generated_at,
        "max_ok_rps": args.max_ok_rps,
        "first_fail_rps": first_fail,
        "stages": stages,
    }
    (results_dir / "summary.json").write_text(
        json.dumps(summary, indent=2) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()
