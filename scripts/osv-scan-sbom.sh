#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}"

SBOM="${1:-}"
if [[ -z "${SBOM}" || ! -f "${SBOM}" ]]; then
  echo "Usage: $0 <cyclonedx-sbom.json>" >&2
  exit 2
fi

OSV_BIN="$("${ROOT}/scripts/ensure-osv-scanner.sh")"
REPORT_DIR="${ROOT}/build/reports/osv"
mkdir -p "${REPORT_DIR}"

echo "Scanning SBOM with OSV-Scanner (OSV/GHSA): ${SBOM}"

set +e
"${OSV_BIN}" scan source \
  --experimental-no-default-plugins \
  --experimental-plugins=sbom \
  -L "${SBOM}" \
  --format=table \
  --output-file="${REPORT_DIR}/osv-scanner.txt"
status=$?
set -e

"${OSV_BIN}" scan source \
  --experimental-no-default-plugins \
  --experimental-plugins=sbom \
  -L "${SBOM}" \
  --format=json \
  --output-file="${REPORT_DIR}/osv-scanner.json" >/dev/null 2>&1 || true

if [[ ! -s "${REPORT_DIR}/osv-scanner.txt" ]]; then
  printf 'OSV-Scanner completed with exit code %s\n' "${status}" > "${REPORT_DIR}/osv-scanner.txt"
fi

if [[ "${status}" -ne 0 ]]; then
  echo "OSV-Scanner found vulnerabilities (or failed). See ${REPORT_DIR}/osv-scanner.txt" >&2
  cat "${REPORT_DIR}/osv-scanner.txt" >&2 || true
  exit "${status}"
fi

echo "OSV-Scanner: no known vulnerabilities in SBOM."
