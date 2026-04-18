#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# list-endpoints.sh -- Audit-Helfer fuer Backend-Endpunkte (Iteration 23C,
# CVM-60).
#
# Scannt alle @RestController-Klassen unter cvm-api/, cvm-application/
# und cvm-ai-services/ und gibt pro Method-Mapping eine Zeile aus:
#   METHOD  PATH                                ROLE           SOURCE:LINE
#
# Bewusst hemdsaermelig (grep + sed). Dient als Basis fuer den manuellen
# Abgleich in der Coverage-Matrix, nicht als finales Urteil.
# ---------------------------------------------------------------------------
set -u
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

SOURCE_DIRS=(
  "${REPO_ROOT}/cvm-api/src/main/java"
  "${REPO_ROOT}/cvm-application/src/main/java"
  "${REPO_ROOT}/cvm-ai-services/src/main/java"
)

TMP_FILE="$(mktemp)"
trap 'rm -f "${TMP_FILE}"' EXIT

for base in "${SOURCE_DIRS[@]}"; do
  [ -d "${base}" ] || continue
  controllers="$(grep -rlE '@RestController\b' "${base}" 2>/dev/null || true)"
  [ -z "${controllers}" ] && continue

  while IFS= read -r file; do
    [ -z "${file}" ] && continue

    class_mapping=""
    raw="$(grep -E '^\s*@RequestMapping\(' "${file}" 2>/dev/null | head -1 || true)"
    if [ -n "${raw}" ]; then
      class_mapping="$(printf '%s' "${raw}" \
        | sed -nE 's/.*@RequestMapping\(\s*"([^"]*)".*/\1/p' \
        | head -1)"
    fi

    grep -nE '@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)' "${file}" 2>/dev/null \
      | while IFS=: read -r lineno content; do
          verb="$(printf '%s' "${content}" \
            | sed -nE 's/.*@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping).*/\1/p' \
            | head -1 \
            | sed -E 's/Mapping$//' \
            | tr '[:lower:]' '[:upper:]')"
          suffix="$(printf '%s' "${content}" \
            | sed -nE 's/.*(value[[:space:]]*=[[:space:]]*)?"([^"]*)".*/\2/p' \
            | head -1)"
          [ -z "${suffix:-}" ] && suffix=""

          end_line=$((lineno + 5))
          role="$(sed -n "${lineno},${end_line}p" "${file}" 2>/dev/null \
            | grep -E '@PreAuthorize' \
            | head -1 \
            | sed -nE "s/.*hasAuthority\(\s*['\"]([^'\"]+)['\"].*/\1/p; s/.*hasRole\(\s*['\"]([^'\"]+)['\"].*/\1/p" \
            | head -1 || true)"
          [ -z "${role:-}" ] && role="?"

          full_path="${class_mapping}${suffix}"
          [ -z "${full_path}" ] && full_path="/"

          rel_path="${file#${REPO_ROOT}/}"
          printf '%-6s %-55s %-25s %s:%s\n' \
            "${verb:-?}" \
            "${full_path}" \
            "${role}" \
            "${rel_path}" \
            "${lineno}" >> "${TMP_FILE}"
        done
  done <<< "${controllers}"
done

printf '%-6s %-55s %-25s %s\n' "METHOD" "PATH" "ROLE" "SOURCE:LINE"
printf -- '-%.0s' {1..130}
echo
if [ -s "${TMP_FILE}" ]; then
  sort -k2,2 -k1,1 "${TMP_FILE}"
else
  echo "(keine Endpunkte gefunden)"
fi
