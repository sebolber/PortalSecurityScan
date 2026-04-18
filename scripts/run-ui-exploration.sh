#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# run-ui-exploration.sh -- One-Shot-Wrapper fuer die Playwright-Exploration
# (CVM-60, Iteration 23C).
#
# Was das Skript tut:
#   1. Prueft Voraussetzungen (docker, java, mvnw, node, npm).
#   2. Startet docker compose up -d, falls Postgres/Keycloak nicht laufen.
#   3. Startet das Spring-Boot-Backend (Port 8081) im Hintergrund, falls
#      /actuator/health nicht UP meldet. Log unter logs/explore-<ts>/backend.log.
#   4. Startet das Angular-Frontend (Port 4200) im Hintergrund, falls nicht
#      erreichbar. Log unter logs/explore-<ts>/frontend.log.
#   5. Installiert scripts/explore-ui/-Dependencies und Playwright-Chromium,
#      falls noch nicht vorhanden.
#   6. Fuehrt die UI-Exploration aus. Report liegt unter
#      docs/YYYYMMDD/ui-exploration-report.md + Screenshots.
#   7. Optional (--stop-after): stoppt alle im Lauf hochgefahrenen Services
#      und Docker-Container wieder.
#
# Idempotent: wer den Stack bereits manuell gestartet hat, behaelt ihn. Das
# Skript startet nur, was nicht erreichbar ist.
#
# Usage:
#   scripts/run-ui-exploration.sh                         # Standard-Lauf
#   scripts/run-ui-exploration.sh --skip-start            # nur Exploration
#   scripts/run-ui-exploration.sh --stop-after            # am Ende aufraeumen
#   scripts/run-ui-exploration.sh --target=ci             # CI-Host-Konfig
#   scripts/run-ui-exploration.sh --admin-pass=geheim     # Passwort explizit
#
# Umgebungsvariablen (optional):
#   CVM_TEST_ADMIN_PASS     Default: admin (Passwort von a.admin@ahs.test)
#   CVM_BACKEND_PORT        Default: 8081
#   CVM_FRONTEND_PORT       Default: 4200
#   CVM_HEALTH_TIMEOUT      Default: 180 (Sekunden, Backend-Health)
#   CVM_FRONTEND_TIMEOUT    Default: 180 (Sekunden, Frontend-Port)
# -----------------------------------------------------------------------------

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
LOG_DIR="${REPO_ROOT}/logs/explore-${TIMESTAMP}"
mkdir -p "${LOG_DIR}"

BACKEND_PORT="${CVM_BACKEND_PORT:-8081}"
FRONTEND_PORT="${CVM_FRONTEND_PORT:-4200}"
HEALTH_TIMEOUT="${CVM_HEALTH_TIMEOUT:-180}"
FRONTEND_TIMEOUT="${CVM_FRONTEND_TIMEOUT:-180}"
ADMIN_PASS="${CVM_TEST_ADMIN_PASS:-admin}"
TARGET="local"
SKIP_START=false
STOP_AFTER=false

# Farben nur bei TTY
if [[ -t 1 ]]; then
    C_RED=$'\033[0;31m'; C_GREEN=$'\033[0;32m'; C_YELLOW=$'\033[0;33m'
    C_BLUE=$'\033[0;34m'; C_BOLD=$'\033[1m'; C_RESET=$'\033[0m'
else
    C_RED=''; C_GREEN=''; C_YELLOW=''; C_BLUE=''; C_BOLD=''; C_RESET=''
fi
log()  { printf "%s[%s]%s %s\n" "${C_BLUE}" "$(date +%H:%M:%S)" "${C_RESET}" "$*"; }
info() { log "${C_BOLD}$*${C_RESET}"; }
ok()   { log "${C_GREEN}OK${C_RESET}    $*"; }
warn() { log "${C_YELLOW}WARN${C_RESET}  $*"; }
fail() { log "${C_RED}FAIL${C_RESET}  $*"; }

usage() {
    cat <<EOF
${C_BOLD}UI-Exploration ausfuehren${C_RESET}

Usage:
  $(basename "$0") [optionen]

Optionen:
  --skip-start             Stack bereits gestartet; nur Exploration laufen lassen
  --stop-after             Nach Exploration: Frontend, Backend und Docker stoppen
  --target=local|ci        Host-Konfig fuer Playwright (Default: local)
  --admin-pass=<pw>        Passwort fuer a.admin@ahs.test (Default: admin)
  -h | --help              Diese Hilfe anzeigen

Ausgaben:
  docs/YYYYMMDD/ui-exploration-report.md
  docs/YYYYMMDD/ui-exploration.json
  docs/YYYYMMDD/ui-exploration/screenshots/<route>.png

Beispiele:
  $(basename "$0")
  $(basename "$0") --stop-after
  $(basename "$0") --skip-start
EOF
}

while (( $# )); do
    case "$1" in
        --skip-start)     SKIP_START=true; shift ;;
        --stop-after)     STOP_AFTER=true; shift ;;
        --target=*)       TARGET="${1#*=}"; shift ;;
        --admin-pass=*)   ADMIN_PASS="${1#*=}"; shift ;;
        -h|--help)        usage; exit 0 ;;
        *)                fail "Unbekannte Option: $1"; usage; exit 64 ;;
    esac
done

# --- Cleanup-State ------------------------------------------------------------
# PIDs, die das Skript selbst gestartet hat. Bei --stop-after werden nur diese
# wieder beendet; manuell gestartete Prozesse bleiben unangetastet.
BACKEND_PID_FILE="${LOG_DIR}/backend.pid"
FRONTEND_PID_FILE="${LOG_DIR}/frontend.pid"
DOCKER_STARTED=false

cleanup_on_signal() {
    warn "Abbruch erkannt, raeume gestartete Prozesse auf..."
    stop_spawned_services
    exit 130
}
trap cleanup_on_signal INT TERM

# --- Helfer -------------------------------------------------------------------
need_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        fail "Befehl '$1' nicht gefunden."
        exit 1
    fi
}

port_in_use() {
    local port="$1"
    if command -v nc >/dev/null 2>&1; then
        nc -z localhost "${port}" 2>/dev/null
    else
        (exec 3<>"/dev/tcp/127.0.0.1/${port}") 2>/dev/null
    fi
}

wait_for_http() {
    local url="$1" timeout="$2" label="$3"
    local waited=0
    info "Warte auf ${label} (${url}, Timeout ${timeout}s)..."
    while (( waited < timeout )); do
        if curl -fsS --max-time 3 "${url}" >/dev/null 2>&1; then
            ok "${label} ist erreichbar."
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    fail "${label} nach ${timeout}s nicht erreichbar."
    return 1
}

wait_for_port() {
    local port="$1" timeout="$2" label="$3"
    local waited=0
    info "Warte auf ${label} (Port ${port}, Timeout ${timeout}s)..."
    while (( waited < timeout )); do
        if port_in_use "${port}"; then
            ok "${label} antwortet auf Port ${port}."
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    fail "${label} auf Port ${port} nach ${timeout}s nicht erreichbar."
    return 1
}

stop_spawned_services() {
    if [[ -f "${FRONTEND_PID_FILE}" ]]; then
        local pid
        pid="$(cat "${FRONTEND_PID_FILE}")"
        if kill -0 "${pid}" 2>/dev/null; then
            info "Stoppe Frontend (PID ${pid})..."
            kill "${pid}" 2>/dev/null || true
            # Kinder ebenfalls beenden (ng serve startet sub-prozesse)
            pkill -TERM -P "${pid}" 2>/dev/null || true
        fi
    fi
    if [[ -f "${BACKEND_PID_FILE}" ]]; then
        local pid
        pid="$(cat "${BACKEND_PID_FILE}")"
        if kill -0 "${pid}" 2>/dev/null; then
            info "Stoppe Backend (PID ${pid})..."
            kill "${pid}" 2>/dev/null || true
            pkill -TERM -P "${pid}" 2>/dev/null || true
        fi
    fi
    if [[ "${DOCKER_STARTED}" == "true" ]]; then
        info "Stoppe docker compose..."
        (cd "${REPO_ROOT}" && docker compose down 2>/dev/null || true)
    fi
}

# --- Voraussetzungen ----------------------------------------------------------
info "Pruefe Voraussetzungen..."
need_cmd docker
need_cmd curl
need_cmd node
need_cmd npm
ok "Voraussetzungen vorhanden."

# --- Stack hochfahren ---------------------------------------------------------
if [[ "${SKIP_START}" == "true" ]]; then
    info "--skip-start: Ueberspringe Start, erwarte laufende Services."
else
    info "Pruefe docker compose..."
    if ! docker info >/dev/null 2>&1; then
        fail "Docker-Daemon antwortet nicht. 'docker info' fehlgeschlagen."
        exit 1
    fi

    # Postgres-Port 5432 als Proxy-Check: wenn nicht offen -> Stack hochfahren
    if port_in_use 5432 && port_in_use 8080; then
        ok "Postgres + Keycloak laufen bereits."
    else
        info "Starte docker compose up -d ..."
        (cd "${REPO_ROOT}" && docker compose up -d) | tee "${LOG_DIR}/docker.log"
        DOCKER_STARTED=true
        wait_for_port 5432 60 "Postgres"
        wait_for_http "http://localhost:8080/realms/cvm-local" 120 "Keycloak-Realm cvm-local"
    fi

    # Backend
    if curl -fsS --max-time 3 \
        "http://localhost:${BACKEND_PORT}/actuator/health" 2>/dev/null \
        | grep -q '"status":"UP"'; then
        ok "Backend laeuft bereits auf Port ${BACKEND_PORT}."
    else
        info "Starte Backend (./mvnw spring-boot:run -pl cvm-app)..."
        need_cmd java
        (
            cd "${REPO_ROOT}"
            nohup ./mvnw spring-boot:run -pl cvm-app \
                > "${LOG_DIR}/backend.log" 2>&1 &
            echo $! > "${BACKEND_PID_FILE}"
        )
        wait_for_http \
            "http://localhost:${BACKEND_PORT}/actuator/health" \
            "${HEALTH_TIMEOUT}" \
            "Backend-Health"
    fi

    # Frontend
    if port_in_use "${FRONTEND_PORT}"; then
        ok "Frontend laeuft bereits auf Port ${FRONTEND_PORT}."
    else
        info "Starte Frontend (npm start)..."
        (
            cd "${REPO_ROOT}/cvm-frontend"
            if [[ ! -d node_modules ]]; then
                info "npm install (Frontend) - erstmalig..."
                npm install --no-audit --no-fund \
                    > "${LOG_DIR}/frontend-install.log" 2>&1
            fi
            nohup npm start \
                > "${LOG_DIR}/frontend.log" 2>&1 &
            echo $! > "${FRONTEND_PID_FILE}"
        )
        wait_for_port "${FRONTEND_PORT}" "${FRONTEND_TIMEOUT}" "Frontend"
    fi
fi

# --- Explore-UI-Paket vorbereiten --------------------------------------------
EXPLORE_DIR="${REPO_ROOT}/scripts/explore-ui"
if [[ ! -d "${EXPLORE_DIR}/node_modules" ]]; then
    info "npm install (scripts/explore-ui) - erstmalig..."
    (cd "${EXPLORE_DIR}" && npm install --no-audit --no-fund) \
        > "${LOG_DIR}/explore-install.log" 2>&1
    ok "Explore-UI-Dependencies installiert."
else
    ok "Explore-UI-Dependencies bereits vorhanden."
fi

# Chromium fuer Playwright sicherstellen
PLAYWRIGHT_DIR="${HOME}/.cache/ms-playwright"
if [[ ! -d "${PLAYWRIGHT_DIR}" ]] \
   || [[ -z "$(ls "${PLAYWRIGHT_DIR}" 2>/dev/null | grep -i chromium || true)" ]]; then
    info "Installiere Playwright-Chromium (einmalig)..."
    (cd "${EXPLORE_DIR}" && npx playwright install chromium) \
        | tee "${LOG_DIR}/playwright-install.log"
else
    ok "Playwright-Chromium bereits vorhanden."
fi

# --- Exploration ausfuehren ---------------------------------------------------
info "Starte UI-Exploration (target=${TARGET}) als a.admin@ahs.test ..."
EXPLORE_LOG="${LOG_DIR}/explore.log"
set +e
(
    cd "${EXPLORE_DIR}"
    CVM_TEST_ADMIN_PASS="${ADMIN_PASS}" \
        npm run explore -- --target="${TARGET}"
) 2>&1 | tee "${EXPLORE_LOG}"
EXPLORE_EXIT="${PIPESTATUS[0]}"
set -e

DATE_DIR="${REPO_ROOT}/docs/$(date +%Y%m%d)"
REPORT_MD="${DATE_DIR}/ui-exploration-report.md"
REPORT_JSON="${DATE_DIR}/ui-exploration.json"
SCREENSHOTS_DIR="${DATE_DIR}/ui-exploration/screenshots"

echo
info "Ergebnis:"
if [[ -f "${REPORT_MD}" ]]; then
    ok "Report: ${REPORT_MD}"
else
    warn "Markdown-Report wurde nicht erzeugt (siehe ${EXPLORE_LOG})."
fi
if [[ -f "${REPORT_JSON}" ]]; then
    ok "JSON:   ${REPORT_JSON}"
fi
if [[ -d "${SCREENSHOTS_DIR}" ]]; then
    COUNT="$(ls "${SCREENSHOTS_DIR}" 2>/dev/null | wc -l)"
    ok "Screenshots: ${COUNT} Dateien unter ${SCREENSHOTS_DIR}"
fi

if [[ "${STOP_AFTER}" == "true" ]]; then
    stop_spawned_services
    ok "Aufraeumen abgeschlossen."
else
    if [[ -f "${BACKEND_PID_FILE}" ]] || [[ -f "${FRONTEND_PID_FILE}" ]] \
        || [[ "${DOCKER_STARTED}" == "true" ]]; then
        info "Stack bleibt laufen. Zum Aufraeumen:"
        info "  docker compose down"
        [[ -f "${BACKEND_PID_FILE}" ]] && \
            info "  kill \$(cat ${BACKEND_PID_FILE})"
        [[ -f "${FRONTEND_PID_FILE}" ]] && \
            info "  kill \$(cat ${FRONTEND_PID_FILE})"
    fi
fi

if (( EXPLORE_EXIT != 0 )); then
    warn "Exploration meldete Exit-Code ${EXPLORE_EXIT} (Routen mit FEHLER/NICHT_ERREICHBAR)."
    warn "Das ist ein diagnostisches Signal, keine Abbruchsbedingung des Skripts."
fi

# Exit-Code weiterreichen, damit der User scharf erkennt ob Findings da sind
exit "${EXPLORE_EXIT}"
