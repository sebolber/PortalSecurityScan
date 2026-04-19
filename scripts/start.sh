#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# CVE-Relevance-Manager (CVM) - Start-Skript
#
# Usage:
#   scripts/start.sh [<git-branch>] [optionen]
#
# Beispiele:
#   scripts/start.sh                     # aktueller HEAD, kein git-Update
#   scripts/start.sh main
#   scripts/start.sh claude/continue-next-session-WIofO
#   scripts/start.sh feature/foo --skip-frontend
#   scripts/start.sh main --no-tail
#
# Was es tut:
#   1. Pruefung der Voraussetzungen (git, java, mvnw, docker, node, npm).
#   2. Wenn ein Branch uebergeben wurde: fetch + checkout + ff-pull
#      (mit Schutz vor unsauberem Working-Tree).
#      Ohne Branch-Argument bleibt der aktuelle HEAD unveraendert.
#   3. Docker-Compose-Stack hochfahren (postgres + keycloak + mailhog).
#   4. Backend (Spring Boot) starten und auf /actuator/health warten.
#   5. Frontend (Angular ng serve) starten und auf TCP-Port 4200 warten.
#   6. tail -f auf saemtliche Log-Dateien (Ctrl-C beendet alles sauber).
#
# Umgebungsvariablen (optional):
#   CVM_LOG_DIR                  Default: <repo>/logs/<zeitstempel>
#   CVM_BACKEND_PORT             Default: 8081 (siehe application.yaml)
#   CVM_FRONTEND_PORT            Default: 4200
#   CVM_HEALTH_TIMEOUT_SECONDS   Default: 180 (Backend-Health-Wartezeit)
#   CVM_FRONTEND_TIMEOUT_SECONDS Default: 180
# -----------------------------------------------------------------------------

set -Eeuo pipefail

# --- Pfade --------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
LOG_DIR="${CVM_LOG_DIR:-${REPO_ROOT}/logs/${TIMESTAMP}}"
PID_FILE="${LOG_DIR}/start.pids"

BACKEND_PORT="${CVM_BACKEND_PORT:-8081}"
FRONTEND_PORT="${CVM_FRONTEND_PORT:-4200}"
HEALTH_TIMEOUT="${CVM_HEALTH_TIMEOUT_SECONDS:-180}"
FRONTEND_TIMEOUT="${CVM_FRONTEND_TIMEOUT_SECONDS:-180}"

# --- Default-Optionen ---------------------------------------------------------
SKIP_DOCKER=false
SKIP_BACKEND=false
SKIP_FRONTEND=false
NO_TAIL=false
ALLOW_DIRTY=false
FORCE_FREE_PORTS=false
BRANCH=""

# --- Farben fuer Log-Ausgabe --------------------------------------------------
if [[ -t 1 ]]; then
    C_RED=$'\033[0;31m'; C_GREEN=$'\033[0;32m'; C_YELLOW=$'\033[0;33m'
    C_BLUE=$'\033[0;34m'; C_BOLD=$'\033[1m'; C_RESET=$'\033[0m'
else
    C_RED=''; C_GREEN=''; C_YELLOW=''; C_BLUE=''; C_BOLD=''; C_RESET=''
fi

log()   { printf "%s[%s]%s %s\n" "${C_BLUE}" "$(date +%H:%M:%S)" "${C_RESET}" "$*"; }
info()  { log "${C_BOLD}$*${C_RESET}"; }
ok()    { log "${C_GREEN}OK${C_RESET}    $*"; }
warn()  { log "${C_YELLOW}WARN${C_RESET}  $*"; }
fail()  { log "${C_RED}FAIL${C_RESET}  $*"; }

# --- Hilfe --------------------------------------------------------------------
usage() {
    cat <<EOF
${C_BOLD}CVM Start-Skript${C_RESET}

Usage:
  $(basename "$0") [<git-branch>] [optionen]

Wird ein Branch (z.B. main oder feature/foo) uebergeben, macht das Skript
vorher ein git fetch + checkout + ff-pull. Ohne Argument bleibt der aktuelle
HEAD unveraendert und das Skript startet direkt.

Optionen:
  --skip-docker       Docker-Compose-Stack nicht starten (postgres muss laufen)
  --skip-backend      Spring-Boot-Backend nicht starten
  --skip-frontend     Angular-Frontend nicht starten
  --no-tail           Log am Ende nicht via tail -f anzeigen
  --allow-dirty       Branch-Wechsel auch bei unsauberem Working-Tree erzwingen
  --force-free-ports  Belegte Ports (5432/8080/8025/1025/8081/4200) automatisch
                      freiraeumen: alte cvm-*-Docker-Container entfernen und
                      lokale Listener per SIGTERM beenden (nach 5s SIGKILL).
                      Ohne dieses Flag werden fremde Prozesse nur gemeldet.
  -h | --help         Diese Hilfe anzeigen

Beispiele:
  $(basename "$0")                     # aktueller HEAD, ohne git-Update
  $(basename "$0") main
  $(basename "$0") claude/continue-next-session-WIofO --no-tail
  $(basename "$0") feature/foo --skip-frontend
EOF
}

# --- Argumente parsen ---------------------------------------------------------
while (( "$#" )); do
    case "$1" in
        --skip-docker)       SKIP_DOCKER=true; shift ;;
        --skip-backend)      SKIP_BACKEND=true; shift ;;
        --skip-frontend)     SKIP_FRONTEND=true; shift ;;
        --no-tail)           NO_TAIL=true; shift ;;
        --allow-dirty)       ALLOW_DIRTY=true; shift ;;
        --force-free-ports)  FORCE_FREE_PORTS=true; shift ;;
        -h|--help)           usage; exit 0 ;;
        --*)             fail "Unbekannte Option: $1"; usage; exit 64 ;;
        *)
            if [[ -z "${BRANCH}" ]]; then
                BRANCH="$1"
            else
                fail "Zu viele Argumente. Nur eine Branch-Angabe ist erlaubt."
                usage
                exit 64
            fi
            shift
            ;;
    esac
done

if [[ -z "${BRANCH}" ]]; then
    info "Kein Branch angegeben - starte auf aktuellem HEAD ohne git-Update."
fi

# --- Cleanup-Hook -------------------------------------------------------------
CHILD_PIDS=()

cleanup() {
    local exit_code=$?
    info "Beende laufende Prozesse..."
    if [[ ${#CHILD_PIDS[@]} -gt 0 ]]; then
        for pid in "${CHILD_PIDS[@]}"; do
            if kill -0 "${pid}" 2>/dev/null; then
                # SIGTERM zuerst, dann nach 10s SIGKILL
                kill -TERM "${pid}" 2>/dev/null || true
            fi
        done
        sleep 2
        for pid in "${CHILD_PIDS[@]}"; do
            if kill -0 "${pid}" 2>/dev/null; then
                kill -KILL "${pid}" 2>/dev/null || true
            fi
        done
    fi
    if [[ -f "${PID_FILE}" ]]; then
        rm -f "${PID_FILE}"
    fi
    if [[ ${exit_code} -ne 0 ]]; then
        fail "Start-Skript mit Code ${exit_code} beendet."
    else
        ok "Sauber beendet."
    fi
    exit ${exit_code}
}

trap cleanup EXIT
trap 'exit 130' INT TERM

# --- Port-Management ---------------------------------------------------------
# Wenn ein frueherer Lauf seine Prozesse oder Container nicht sauber
# aufgeraeumt hat, bricht `docker compose up` bzw. Spring Boot spaeter mit
# "Port already allocated" ab. Diese Helfer pruefen vor jedem Start, ob der
# Port frei ist, und raeumen ggf. auf.

# Gibt die PIDs zurueck, die auf dem angegebenen Port lauschen.
pids_on_port() {
    local port="$1"
    if command -v lsof >/dev/null 2>&1; then
        lsof -nP -iTCP:"${port}" -sTCP:LISTEN -t 2>/dev/null || true
    elif command -v ss >/dev/null 2>&1; then
        # Linux-Fallback. ss -tlnp -> "users:(("name",pid=1234,..."
        ss -tlnpH "sport = :${port}" 2>/dev/null \
            | awk -F'pid=' '{for (i=2;i<=NF;i++){split($i,a,",");print a[1]}}' \
            | sort -u || true
    else
        # Ohne lsof/ss geht kein Port-Check; nicht raten.
        echo ""
    fi
}

# Stoppt/entfernt Docker-Container, die den Port blocken. CVM-eigene
# Container (Name beginnt mit "cvm-") werden immer entfernt, weil sie
# eindeutig Altlasten eines frueheren Laufs sind. Fremde Container
# stoppen wir nur, wenn --force-free-ports gesetzt ist; sonst geben
# wir eine klare Anleitung aus und liefern 1 zurueck, damit der Caller
# abbrechen kann.
# $1 Port
# Return: 0 = Port ist jetzt frei von Docker-Containern,
#         1 = ein fremder Container blockiert den Port weiterhin
remove_container_on_port() {
    local port="$1"
    if ! command -v docker >/dev/null 2>&1; then
        return 0
    fi
    local offenders
    offenders="$(docker ps -a --format '{{.Names}}	{{.Ports}}' 2>/dev/null \
        | awk -v p=":${port}->" 'index($0, p) { print $1 }' || true)"
    [[ -z "${offenders}" ]] && return 0

    local rc=0
    local name
    while IFS= read -r name; do
        [[ -z "${name}" ]] && continue
        if [[ "${name}" == cvm-* ]]; then
            warn "Entferne alten CVM-Container '${name}' (blockiert Port ${port})."
            docker rm -f "${name}" >/dev/null 2>&1 || true
        elif "${FORCE_FREE_PORTS}"; then
            warn "Stoppe fremden Container '${name}' auf Port ${port} (--force-free-ports)."
            docker rm -f "${name}" >/dev/null 2>&1 || true
        else
            fail "Fremder Docker-Container '${name}' blockiert Port ${port}."
            fail "Loesung: '--force-free-ports' setzen oder manuell beenden:"
            fail "    docker stop '${name}'   # oder: docker rm -f '${name}'"
            rc=1
        fi
    done <<< "${offenders}"
    return "${rc}"
}

# Stellt sicher, dass der Port frei ist. Verhalten:
#   1. Raeumt einen evtl. vorhandenen cvm-*-Container weg.
#   2. Wenn dann noch ein Listener existiert, entscheidet FORCE_FREE_PORTS:
#      - true  -> SIGTERM, nach 5s SIGKILL, erneuter Check.
#      - false -> Warnung mit Anleitung und Fehler zurueckgeben.
# $1: Port  $2: Servicename fuer die Log-Ausgabe
ensure_port_free() {
    local port="$1"
    local svc="$2"

    if ! remove_container_on_port "${port}"; then
        return 1
    fi

    local pids
    pids="$(pids_on_port "${port}")"
    if [[ -z "${pids}" ]]; then
        return 0
    fi

    if ! "${FORCE_FREE_PORTS}"; then
        fail "Port ${port} (${svc}) wird bereits von PID(s) belegt: ${pids//$'\n'/ }"
        if command -v ps >/dev/null 2>&1; then
            fail "Prozess-Details:"
            # shellcheck disable=SC2086
            ps -o pid=,user=,command= -p ${pids} 2>/dev/null | sed 's/^/        /' >&2 || true
        fi
        fail "Loesung: '--force-free-ports' setzen, oder manuell beenden:"
        fail "    kill ${pids//$'\n'/ }"
        return 1
    fi

    warn "Beende PID(s) auf Port ${port} (${svc}): ${pids//$'\n'/ }"
    # SIGTERM zuerst.
    local pid
    while IFS= read -r pid; do
        [[ -z "${pid}" ]] && continue
        kill -TERM "${pid}" 2>/dev/null || true
    done <<< "${pids}"

    # Bis zu 5s Ruhephase abwarten.
    local waited=0
    while (( waited < 5 )); do
        pids="$(pids_on_port "${port}")"
        [[ -z "${pids}" ]] && break
        sleep 1
        waited=$((waited + 1))
    done

    if [[ -n "${pids}" ]]; then
        warn "SIGTERM reichte nicht - SIGKILL auf ${pids//$'\n'/ }."
        while IFS= read -r pid; do
            [[ -z "${pid}" ]] && continue
            kill -KILL "${pid}" 2>/dev/null || true
        done <<< "${pids}"
        sleep 1
    fi

    pids="$(pids_on_port "${port}")"
    if [[ -n "${pids}" ]]; then
        fail "Port ${port} konnte nicht freigeraeumt werden (noch: ${pids//$'\n'/ })."
        return 1
    fi
    ok "Port ${port} (${svc}) ist jetzt frei."
}

# Sammelt alle Ports, die die jeweils gewaehlten Services brauchen, und
# raeumt sie bei Bedarf frei. Bricht das Skript ab, falls ein Port nicht
# freigemacht werden kann.
prepare_ports() {
    info "Pruefe belegte Ports..."
    local had_error=0

    if ! "${SKIP_DOCKER}"; then
        # Postgres, Keycloak, MailHog (smtp + web).
        ensure_port_free 5432 "postgres" || had_error=1
        ensure_port_free 8080 "keycloak" || had_error=1
        ensure_port_free 1025 "mailhog-smtp" || had_error=1
        ensure_port_free 8025 "mailhog-web" || had_error=1
    fi
    if ! "${SKIP_BACKEND}"; then
        ensure_port_free "${BACKEND_PORT}" "backend" || had_error=1
    fi
    if ! "${SKIP_FRONTEND}"; then
        ensure_port_free "${FRONTEND_PORT}" "frontend" || had_error=1
    fi

    if (( had_error != 0 )); then
        fail "Mindestens ein Port ist belegt. Abbruch."
        exit 80
    fi
    ok "Alle benoetigten Ports sind frei."
}

# --- Voraussetzungen pruefen --------------------------------------------------
require_cmd() {
    local cmd="$1"
    if ! command -v "${cmd}" >/dev/null 2>&1; then
        fail "Kommando '${cmd}' nicht gefunden."
        return 1
    fi
}

check_prereqs() {
    info "Pruefe Voraussetzungen..."
    local missing=0
    require_cmd git || missing=1
    require_cmd java || missing=1
    require_cmd curl || missing=1

    if ! "${SKIP_DOCKER}"; then
        if ! command -v docker >/dev/null 2>&1; then
            fail "Docker nicht gefunden (oder --skip-docker setzen)."
            missing=1
        elif ! docker compose version >/dev/null 2>&1; then
            fail "'docker compose'-Plugin nicht verfuegbar."
            missing=1
        fi
    fi

    if ! "${SKIP_FRONTEND}"; then
        require_cmd node || missing=1
        require_cmd npm || missing=1
    fi

    if [[ ! -x "${REPO_ROOT}/mvnw" ]]; then
        fail "Maven-Wrapper ${REPO_ROOT}/mvnw nicht ausfuehrbar."
        missing=1
    fi

    if [[ ${missing} -ne 0 ]]; then
        fail "Mindestens eine Voraussetzung fehlt - Abbruch."
        exit 69
    fi

    local java_version
    java_version="$(java -version 2>&1 | awk -F\" '/version/ {print $2}' | cut -d. -f1)"
    if [[ -n "${java_version}" && "${java_version}" -lt 21 ]]; then
        warn "Java ${java_version} gefunden - CVM verlangt Java 21."
    fi

    ok "Alle Voraussetzungen erfuellt."
}

# --- Branch-Wechsel -----------------------------------------------------------
checkout_branch() {
    info "Branch wechseln auf: ${BRANCH}"
    cd "${REPO_ROOT}"

    if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
        fail "${REPO_ROOT} ist kein Git-Repository."
        exit 70
    fi

    # Nur Aenderungen an getrackten Dateien blockieren den Branch-Wechsel.
    # Untracked-Files (z.B. das gerade angelegte logs/-Verzeichnis) sind
    # unkritisch, denn git wechselt den Branch trotzdem ohne Datenverlust.
    if [[ -n "$(git status --porcelain --untracked-files=no)" ]] && ! "${ALLOW_DIRTY}"; then
        fail "Working-Tree hat lokale Aenderungen an getrackten Dateien."
        fail "Commit/stash sie oder nutze --allow-dirty."
        git status --short >&2
        exit 71
    fi

    info "Aktualisiere Remote-Refs (git fetch)..."
    if ! git fetch --prune origin 2>&1 | tee -a "${LOG_DIR}/git.log"; then
        warn "git fetch fehlgeschlagen - arbeite mit lokalem Stand weiter."
    fi

    if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
        info "Lokaler Branch '${BRANCH}' existiert."
        git checkout "${BRANCH}" | tee -a "${LOG_DIR}/git.log"
        if git ls-remote --exit-code --heads origin "${BRANCH}" >/dev/null 2>&1; then
            git pull --ff-only origin "${BRANCH}" | tee -a "${LOG_DIR}/git.log" || \
                warn "Fast-Forward-Pull nicht moeglich - Branch divergiert vom Remote."
        fi
    elif git ls-remote --exit-code --heads origin "${BRANCH}" >/dev/null 2>&1; then
        info "Lege lokalen Branch aus origin/${BRANCH} an."
        git checkout -b "${BRANCH}" "origin/${BRANCH}" | tee -a "${LOG_DIR}/git.log"
    else
        fail "Branch '${BRANCH}' existiert weder lokal noch auf origin."
        exit 72
    fi

    ok "Auf Branch $(git rev-parse --abbrev-ref HEAD) (commit $(git rev-parse --short HEAD))."
}

# --- Docker-Compose -----------------------------------------------------------
start_docker() {
    if "${SKIP_DOCKER}"; then
        warn "Docker uebersprungen (--skip-docker)."
        return 0
    fi
    info "Starte Docker-Compose-Stack (postgres + keycloak + mailhog)..."
    cd "${REPO_ROOT}"
    if ! docker compose up -d 2>&1 | tee -a "${LOG_DIR}/docker.log"; then
        fail "docker compose up fehlgeschlagen."
        exit 73
    fi

    info "Warte auf postgres-Healthcheck..."
    local waited=0
    while (( waited < 60 )); do
        local status
        status="$(docker inspect -f '{{.State.Health.Status}}' cvm-postgres 2>/dev/null || echo 'unknown')"
        if [[ "${status}" == "healthy" ]]; then
            ok "postgres ist healthy."
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    fail "postgres nicht innerhalb von 60s healthy geworden."
    exit 74
}

# --- Backend ------------------------------------------------------------------
start_backend() {
    if "${SKIP_BACKEND}"; then
        warn "Backend uebersprungen (--skip-backend)."
        return 0
    fi
    info "Starte Spring-Boot-Backend (Port ${BACKEND_PORT})..."
    cd "${REPO_ROOT}"
    local backend_log="${LOG_DIR}/backend.log"
    local install_log="${LOG_DIR}/backend-install.log"

    # spring-boot:run laeuft sonst auf jedem Projekt im Reaktor (auch
    # cvm-parent ohne Main-Klasse). Daher zwei Schritte:
    #   1. Dependencies in den lokalen Maven-Cache installieren.
    #   2. spring-boot:run NUR auf cvm-app, ohne -am.
    info "Installiere Maven-Module fuer cvm-app (Log: ${install_log})..."
    if ! "${REPO_ROOT}/mvnw" -q -pl cvm-app -am -DskipTests install \
            > "${install_log}" 2>&1; then
        fail "Maven-Install fehlgeschlagen. Letzte Log-Zeilen:"
        tail -n 40 "${install_log}" >&2 || true
        exit 75
    fi
    ok "Maven-Install fertig."

    nohup "${REPO_ROOT}/mvnw" -pl cvm-app \
        -Dspring-boot.run.jvmArguments="-Dserver.port=${BACKEND_PORT}" \
        spring-boot:run \
        > "${backend_log}" 2>&1 &
    local pid=$!
    CHILD_PIDS+=("${pid}")
    echo "backend ${pid}" >> "${PID_FILE}"
    info "Backend-PID ${pid}, Log: ${backend_log}"

    info "Warte auf /actuator/health (max ${HEALTH_TIMEOUT}s)..."
    local waited=0
    local interval=3
    while (( waited < HEALTH_TIMEOUT )); do
        if ! kill -0 "${pid}" 2>/dev/null; then
            fail "Backend-Prozess (${pid}) ist beendet. Letzte Log-Zeilen:"
            tail -n 40 "${backend_log}" >&2 || true
            exit 75
        fi
        local response
        response="$(curl -fsS "http://127.0.0.1:${BACKEND_PORT}/actuator/health" 2>/dev/null || true)"
        if [[ "${response}" == *"\"status\":\"UP\""* ]]; then
            ok "Backend antwortet UP."
            return 0
        fi
        sleep "${interval}"
        waited=$((waited + interval))
    done
    fail "Backend wurde innerhalb von ${HEALTH_TIMEOUT}s nicht UP."
    exit 76
}

# --- Frontend -----------------------------------------------------------------
start_frontend() {
    if "${SKIP_FRONTEND}"; then
        warn "Frontend uebersprungen (--skip-frontend)."
        return 0
    fi
    local fe_dir="${REPO_ROOT}/cvm-frontend"
    if [[ ! -d "${fe_dir}/node_modules" ]]; then
        info "node_modules fehlt - installiere npm-Dependencies..."
        ( cd "${fe_dir}" && npm ci --no-audit --no-fund --loglevel=error ) \
            2>&1 | tee -a "${LOG_DIR}/frontend-install.log" || {
                fail "npm ci fehlgeschlagen."
                exit 77
        }
    fi

    info "Starte Angular-DevServer (Port ${FRONTEND_PORT})..."
    local frontend_log="${LOG_DIR}/frontend.log"
    nohup bash -c "cd '${fe_dir}' && npx ng serve --host 0.0.0.0 --port ${FRONTEND_PORT}" \
        > "${frontend_log}" 2>&1 &
    local pid=$!
    CHILD_PIDS+=("${pid}")
    echo "frontend ${pid}" >> "${PID_FILE}"
    info "Frontend-PID ${pid}, Log: ${frontend_log}"

    info "Warte auf Frontend-Antwort (max ${FRONTEND_TIMEOUT}s)..."
    local waited=0
    local interval=3
    while (( waited < FRONTEND_TIMEOUT )); do
        if ! kill -0 "${pid}" 2>/dev/null; then
            fail "Frontend-Prozess (${pid}) ist beendet. Letzte Log-Zeilen:"
            tail -n 40 "${frontend_log}" >&2 || true
            exit 78
        fi
        if curl -fsS -o /dev/null "http://127.0.0.1:${FRONTEND_PORT}/" 2>/dev/null; then
            ok "Frontend antwortet."
            return 0
        fi
        sleep "${interval}"
        waited=$((waited + interval))
    done
    fail "Frontend wurde innerhalb von ${FRONTEND_TIMEOUT}s nicht erreichbar."
    exit 79
}

# --- Zusammenfassung ----------------------------------------------------------
print_summary() {
    info "============================================================"
    info "${C_BOLD}CVM laeuft.${C_RESET}"
    if ! "${SKIP_BACKEND}"; then
        info "  Backend       : http://localhost:${BACKEND_PORT}"
        info "  Health        : http://localhost:${BACKEND_PORT}/actuator/health"
        info "  OpenAPI/Swagger: http://localhost:${BACKEND_PORT}/swagger-ui.html"
    fi
    if ! "${SKIP_FRONTEND}"; then
        info "  Frontend      : http://localhost:${FRONTEND_PORT}"
    fi
    if ! "${SKIP_DOCKER}"; then
        info "  Keycloak      : http://localhost:8080 (admin/admin)"
        info "  MailHog       : http://localhost:8025"
        info "  Postgres      : localhost:5432 (cvm/cvm)"
    fi
    info "  Logs          : ${LOG_DIR}"
    info "============================================================"
}

# --- Tail-Loop ----------------------------------------------------------------
tail_logs() {
    if "${NO_TAIL}"; then
        info "tail uebersprungen (--no-tail). Prozesse laufen weiter."
        info "Stoppe mit: kill \$(awk '{print \$2}' '${PID_FILE}')"
        # Wir verzichten auf trap-Cleanup, damit die Prozesse weiterlaufen.
        trap - EXIT
        return 0
    fi
    info "Folge Logs (Ctrl-C beendet alles und raeumt auf)..."
    local logs=()
    [[ -f "${LOG_DIR}/backend.log"  ]] && logs+=("${LOG_DIR}/backend.log")
    [[ -f "${LOG_DIR}/frontend.log" ]] && logs+=("${LOG_DIR}/frontend.log")
    [[ -f "${LOG_DIR}/docker.log"   ]] && logs+=("${LOG_DIR}/docker.log")
    if [[ ${#logs[@]} -eq 0 ]]; then
        warn "Keine Log-Dateien zum Tailen gefunden."
        return 0
    fi
    tail -n +1 -F "${logs[@]}"
}

# --- Main ---------------------------------------------------------------------
mkdir -p "${LOG_DIR}"
: > "${PID_FILE}"
info "Log-Verzeichnis: ${LOG_DIR}"

check_prereqs
if [[ -n "${BRANCH}" ]]; then
    checkout_branch
else
    info "Ueberspringe git-Update (kein Branch-Argument)."
fi
prepare_ports
start_docker
start_backend
start_frontend
print_summary
tail_logs
