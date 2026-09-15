#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS_DIR="${ROOT}/.tools"
VERSION="${OSV_SCANNER_VERSION:-v2.6.0}"
BIN="${TOOLS_DIR}/osv-scanner"

if [[ -x "${BIN}" && "${OSV_SCANNER_FORCE_DOWNLOAD:-}" != "1" ]]; then
  echo "${BIN}"
  exit 0
fi

os_name="$(uname -s | tr '[:upper:]' '[:lower:]')"
arch_name="$(uname -m)"
case "${arch_name}" in
  x86_64|amd64) arch_name="amd64" ;;
  aarch64|arm64) arch_name="arm64" ;;
  *)
    echo "Unsupported architecture: ${arch_name}" >&2
    exit 1
    ;;
esac

asset="osv-scanner_${os_name}_${arch_name}"
url="https://github.com/google/osv-scanner/releases/download/${VERSION}/${asset}"

mkdir -p "${TOOLS_DIR}"
tmp="$(mktemp)"
echo "Downloading OSV-Scanner ${VERSION} (${asset})..." >&2
curl -fsSL -o "${tmp}" "${url}"
chmod +x "${tmp}"
mv "${tmp}" "${BIN}"
echo "${BIN}"
