#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# cvm-dev-token.sh - Holt einen Access-Token aus dem lokalen Dev-Keycloak
# und gibt ihn auf stdout aus.
#
# NUR fuer DEV-Umgebungen (`infra/keycloak/dev-realm.json`). In Prod wird
# der Token ueber den SSO-Flow des Frontends oder einen Service-Account
# eingeholt - dieses Skript ist ausdruecklich kein Produkt-Feature.
#
# Usage:
#   ./scripts/cvm-dev-token.sh                      # Default a.admin@ahs.test
#   ./scripts/cvm-dev-token.sh t.tester@ahs.test test
#   ./scripts/cvm-dev-token.sh j.meyer@ahs.test meyer
#
# Einsatz im Upload-Skript:
#   ./scripts/cvm-upload.sh \
#       --cvm-url http://localhost:8081 \
#       --product-version-id 2d6abe21-bef2-43de-9e6a-f5e29555c76a \
#       --token "$(./scripts/cvm-dev-token.sh)"
#
# Umgebungsvariablen (optional):
#   CVM_KEYCLOAK_URL     Default http://localhost:8080
#   CVM_KEYCLOAK_REALM   Default cvm-local
#   CVM_KEYCLOAK_CLIENT  Default cvm-local
# -----------------------------------------------------------------------------
set -Eeuo pipefail

KC_URL="${CVM_KEYCLOAK_URL:-http://localhost:8080}"
KC_REALM="${CVM_KEYCLOAK_REALM:-cvm-local}"
KC_CLIENT="${CVM_KEYCLOAK_CLIENT:-cvm-local}"

USER="${1:-a.admin@ahs.test}"
# Default-Passwort wird aus dem Benutzernamen abgeleitet, damit man die
# drei Dev-Accounts aus dev-realm.json ohne zweites Argument nutzen kann.
case "${USER}" in
    a.admin@ahs.test)   DEFAULT_PW="admin" ;;
    t.tester@ahs.test)  DEFAULT_PW="test"  ;;
    j.meyer@ahs.test)   DEFAULT_PW="meyer" ;;
    *)                  DEFAULT_PW=""      ;;
esac
PASS="${2:-${DEFAULT_PW}}"

if [[ -z "${PASS}" ]]; then
    echo "Passwort fuer '${USER}' nicht bekannt. Usage:" >&2
    echo "  $0 <user> <password>" >&2
    exit 64
fi

response="$(curl -sS -X POST \
    "${KC_URL}/realms/${KC_REALM}/protocol/openid-connect/token" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d "grant_type=password" \
    -d "client_id=${KC_CLIENT}" \
    -d "username=${USER}" \
    --data-urlencode "password=${PASS}")"

if ! echo "${response}" | grep -q '"access_token"'; then
    echo "Keycloak-Login fehlgeschlagen:" >&2
    echo "${response}" >&2
    exit 1
fi

# Extrahiere access_token ohne jq-Abhaengigkeit.
echo "${response}" \
    | awk -F'"access_token":"' 'NF>1 {print $2}' \
    | cut -d'"' -f1
