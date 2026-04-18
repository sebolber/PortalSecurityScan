#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# list-frontend-calls.sh -- Audit-Helfer fuer Frontend-API-Aufrufe und
# Routen (Iteration 23C, CVM-60).
#
# Produziert zwei Abschnitte:
#   1. Alle api.get/post/put/delete/patch-Aufrufe aus cvm-frontend/src/app
#      (URL + Quelldatei:Zeile)
#   2. Alle Angular-Router-Pfade aus app.routes.ts inkl. Komponenten-
#      Klassennamen
#
# Hemdsaermelig, grep-basiert. Falsch-positive Treffer (z.B. string-
# Interpolationen) sind in Kauf zu nehmen; sie fallen beim manuellen
# Abgleich in der Coverage-Matrix auf.
# ---------------------------------------------------------------------------
set -u
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
FE_ROOT="${REPO_ROOT}/cvm-frontend/src/app"

if [ ! -d "${FE_ROOT}" ]; then
  echo "Frontend-Verzeichnis nicht gefunden: ${FE_ROOT}" >&2
  exit 1
fi

print_section() {
  printf '\n===== %s =====\n' "$1"
}

print_section "HTTP-Aufrufe (api.{get,post,put,delete,patch})"
printf '%-8s %-60s %s\n' "METHOD" "URL" "SOURCE:LINE"
printf -- '-%.0s' {1..110}; echo

# Such-Pattern: this.api.get< .. >('/api/..') oder this.api.post<..>('/api/..', ..)
grep -rn --include='*.ts' \
     -E 'this\.api\.(get|post|put|delete|patch)' \
     "${FE_ROOT}" 2>/dev/null \
  | while IFS=: read -r file lineno rest; do
      method="$(printf '%s' "${rest}" \
        | sed -nE 's/.*this\.api\.(get|post|put|delete|patch).*/\1/p' \
        | head -1 | tr '[:lower:]' '[:upper:]')"
      # URL: erstes String-Literal in Backticks, Double- oder Single-Quotes
      url="$(printf '%s' "${rest}" \
        | sed -nE "s/.*\`([^\`]*)\`.*/\\1/p; s/.*'([^']*)'.*/\\1/p; s/.*\"([^\"]*)\".*/\\1/p" \
        | head -1)"
      printf '%-8s %-60s %s:%s\n' \
        "${method:-?}" \
        "${url:-?}" \
        "${file#${REPO_ROOT}/}" \
        "${lineno}"
    done \
  | sort -k2,2 -u

print_section "Angular-Routen (app.routes.ts)"
printf '%-35s %s\n' "PATH" "COMPONENT"
printf -- '-%.0s' {1..85}; echo

ROUTES_FILE="${FE_ROOT}/app.routes.ts"
if [ -f "${ROUTES_FILE}" ]; then
  # Einfacher Zustandsautomat: path merken, bei erstem
  # loadComponent/component den Namen holen und ausgeben.
  awk '
    function extract_between(s, a, b,   i, j) {
      i = index(s, a); if (i == 0) return ""; s = substr(s, i + length(a));
      j = index(s, b); if (j == 0) return ""; return substr(s, 1, j - 1);
    }
    /path:[[:space:]]*['\''"]/ {
      line = $0;
      sub(/.*path:[[:space:]]*/, "", line);
      if (substr(line, 1, 1) == "\x27") q = "\x27"; else q = "\"";
      current_path = extract_between(line, q, q);
    }
    /loadComponent:/ {
      loader_pending = 1;
      next;
    }
    loader_pending && /m\.[A-Za-z_]+/ {
      s = $0;
      sub(/.*m\./, "", s);
      sub(/[^A-Za-z_].*/, "", s);
      if (current_path != "" && s != "") {
        printf("%-35s %s\n", current_path, s);
        current_path = ""; loader_pending = 0;
      }
    }
    /component:[[:space:]]*[A-Z][A-Za-z_]+/ {
      s = $0;
      sub(/.*component:[[:space:]]*/, "", s);
      sub(/[^A-Za-z_].*/, "", s);
      if (current_path != "" && s != "") {
        printf("%-35s %s\n", current_path, s);
        current_path = "";
      }
    }
  ' "${ROUTES_FILE}" \
    | sort -u
else
  echo "app.routes.ts nicht gefunden: ${ROUTES_FILE}" >&2
fi
