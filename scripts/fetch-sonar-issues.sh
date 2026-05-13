#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# fetch-sonar-issues.sh
#
# Liest die offenen Issues eines SonarCloud-Projekts und gibt sie als
# Markdown nach stdout aus. Der CI-Job (siehe .github/workflows/ci.yml)
# leitet die Ausgabe in den Body einer pinned GitHub-Issue mit dem
# Label "sonar-snapshot".
#
# Optionale Umgebungsvariablen:
#   SONAR_HOST_URL        Default: https://sonarcloud.io
#   SONAR_PROJECT_KEY     Default: sebolber_PortalSecurityScan
#   SONAR_ORGANIZATION    Default: sebolber
#   SONAR_TOKEN           Pflicht. Token mit Read-Rechten auf dem Projekt.
#   WAIT_FOR_SCAN         "1" = poll api/ce/component bis Scan-Status final.
# ---------------------------------------------------------------------------
set -euo pipefail

SONAR_HOST_URL=${SONAR_HOST_URL:-https://sonarcloud.io}
SONAR_PROJECT_KEY=${SONAR_PROJECT_KEY:-sebolber_PortalSecurityScan}
SONAR_ORGANIZATION=${SONAR_ORGANIZATION:-sebolber}
WAIT_FOR_SCAN=${WAIT_FOR_SCAN:-0}

if [ -z "${SONAR_TOKEN:-}" ]; then
  echo "ERROR: SONAR_TOKEN ist nicht gesetzt." >&2
  exit 2
fi

api() {
  curl -fsS -u "$SONAR_TOKEN:" -G "$@"
}

log() {
  echo "[fetch-sonar-issues] $*" >&2
}

# ---------- Optional: warten bis aktueller Scan fertig ist -----------------
if [ "$WAIT_FOR_SCAN" = "1" ]; then
  log "Warte auf Abschluss des Sonar-Scans fuer $SONAR_PROJECT_KEY ..."
  for i in $(seq 1 60); do
    resp=$(api "$SONAR_HOST_URL/api/ce/component" \
      --data-urlencode "component=$SONAR_PROJECT_KEY" \
      || echo '{}')
    queue_len=$(echo "$resp" | jq -r '.queue | length // 0')
    current_status=$(echo "$resp" | jq -r '.current.status // "NONE"')
    if [ "$queue_len" = "0" ]; then
      case "$current_status" in
        SUCCESS|FAILED|CANCELED|NONE)
          log "Scan-Status: $current_status (Versuch $i)"
          break
          ;;
      esac
    fi
    sleep 5
  done
fi

# ---------- Issues paginiert holen ----------------------------------------
tmp_all=$(mktemp)
trap 'rm -f "$tmp_all"' EXIT

page=1
total=0
ps=500
while :; do
  resp=$(api "$SONAR_HOST_URL/api/issues/search" \
    --data-urlencode "componentKeys=$SONAR_PROJECT_KEY" \
    --data-urlencode "organization=$SONAR_ORGANIZATION" \
    --data-urlencode "resolved=false" \
    --data-urlencode "ps=$ps" \
    --data-urlencode "p=$page")
  echo "$resp" | jq -c '.issues[]?' >> "$tmp_all"
  total=$(echo "$resp" | jq -r '.total // 0')
  ps_returned=$(echo "$resp" | jq -r '.ps // 500')
  log "Seite $page geholt, total=$total, ps=$ps_returned"
  if [ "$total" -eq 0 ] || [ $((page * ps_returned)) -ge "$total" ]; then
    break
  fi
  page=$((page + 1))
  # Sonar API begrenzt p*ps <= 10000. Wenn wir das erreichen, abbrechen.
  if [ $((page * ps_returned)) -gt 10000 ]; then
    log "WARNUNG: Sonar-API-Limit (p*ps>10000) erreicht. Snapshot ist gekuerzt."
    break
  fi
done

count=$(wc -l < "$tmp_all" | tr -d ' ')

# ---------- Markdown rendern ----------------------------------------------
project_url="$SONAR_HOST_URL/dashboard?id=$SONAR_PROJECT_KEY"
generated_at=$(date -u +'%Y-%m-%dT%H:%M:%SZ')

cat <<EOF
# Sonar Issue Snapshot - main

Stand: \`$generated_at\` (UTC)

**Projekt**: [\`$SONAR_PROJECT_KEY\`]($project_url)

**Anzahl offener Issues**: $count

EOF

if [ "$count" -eq 0 ]; then
  echo "Keine offenen Sonar-Issues."
  echo
  echo "---"
  echo "_Generiert von \`scripts/fetch-sonar-issues.sh\` nach jedem Push auf \`main\`._"
  exit 0
fi

# Severity-Counts uebersicht
echo "## Verteilung"
echo
echo "| Severity | Anzahl |"
echo "| -------- | -----: |"
for sev in BLOCKER CRITICAL MAJOR MINOR INFO; do
  sev_count=$(jq -s --arg sev "$sev" '[.[] | select(.severity == $sev)] | length' "$tmp_all")
  echo "| $sev | $sev_count |"
done
echo

# Detailansicht nach Severity > Datei
for sev in BLOCKER CRITICAL MAJOR MINOR INFO; do
  sev_count=$(jq -s --arg sev "$sev" '[.[] | select(.severity == $sev)] | length' "$tmp_all")
  if [ "$sev_count" = "0" ]; then
    continue
  fi
  echo "## $sev ($sev_count)"
  echo
  # Komponenten nach Anzahl absteigend
  mapfile -t components < <(jq -s --arg sev "$sev" -r '
    [.[] | select(.severity == $sev)]
    | group_by(.component)
    | sort_by(-length)
    | .[] | .[0].component
  ' "$tmp_all")

  for component in "${components[@]}"; do
    [ -z "$component" ] && continue
    file_path="${component#*:}"
    file_count=$(jq -s --arg sev "$sev" --arg c "$component" \
      '[.[] | select(.severity == $sev and .component == $c)] | length' "$tmp_all")
    echo "<details><summary><code>${file_path:-$component}</code> ($file_count)</summary>"
    echo
    echo "| Line | Rule | Message |"
    echo "| ---: | ---- | ------- |"
    jq -s --arg sev "$sev" --arg c "$component" --arg host "$SONAR_HOST_URL" -r '
      [.[] | select(.severity == $sev and .component == $c)]
      | sort_by(.line // 0)
      | .[]
      | "| \(.line // "-") | [\(.rule)](\($host)/coding_rules?open=\(.rule)) | \((.message // "") | gsub("\\|"; "\\\\|") | gsub("\n"; " ") | gsub("\r"; "")) |"
    ' "$tmp_all"
    echo
    echo "</details>"
    echo
  done
done

echo "---"
echo "_Generiert von \`scripts/fetch-sonar-issues.sh\` nach jedem Push auf \`main\`._"
