#!/bin/bash

set -euo pipefail

SONAR_URL="${SONAR_URL:-http://localhost:9000}"
SONAR_TOKEN="${SONAR_TOKEN:?SONAR_TOKEN is required}"

GATE_NAME="SafeZone Quality Gate"
PROJECT_KEY="safe-zone"

echo "Configuring SonarQube Quality Gate..."

get_gate_id() {
    curl -sSf \
        -u "${SONAR_TOKEN}:" \
        "${SONAR_URL}/api/qualitygates/list" |
        jq -r --arg NAME "$GATE_NAME" \
        '.qualitygates[] | select(.name == $NAME) | .id' |
        head -n 1
}

GATE_ID="$(get_gate_id)"

if [ -z "$GATE_ID" ] || [ "$GATE_ID" = "null" ]; then
    echo "Creating Quality Gate: ${GATE_NAME}"

    curl -sSf \
        -u "${SONAR_TOKEN}:" \
        -X POST \
        --data-urlencode "name=${GATE_NAME}" \
        "${SONAR_URL}/api/qualitygates/create" > /dev/null

    GATE_ID="$(get_gate_id)"
fi

if [ -z "$GATE_ID" ] || [ "$GATE_ID" = "null" ]; then
    echo "ERROR: Could not determine Quality Gate ID."
    exit 1
fi

echo "Quality Gate ID: ${GATE_ID}"

echo "Removing existing conditions..."

curl -sSf \
    -u "${SONAR_TOKEN}:" \
    "${SONAR_URL}/api/qualitygates/show?name=$(printf '%s' "$GATE_NAME" | jq -sRr @uri)" |
    jq -r '.conditions[]?.id' |
    while read -r condition_id; do
        curl -sSf \
            -u "${SONAR_TOKEN}:" \
            -X POST \
            --data-urlencode "id=${condition_id}" \
            "${SONAR_URL}/api/qualitygates/delete_condition" > /dev/null
    done

echo "Adding conditions..."

add_condition() {
    local metric="$1"
    local operator="$2"
    local threshold="$3"

    echo "  ${metric} ${operator} ${threshold}"

    curl -sSf \
        -u "${SONAR_TOKEN}:" \
        -X POST \
        --data-urlencode "gateName=${GATE_NAME}" \
        --data-urlencode "metric=${metric}" \
        --data-urlencode "op=${operator}" \
        --data-urlencode "error=${threshold}" \
        "${SONAR_URL}/api/qualitygates/create_condition" > /dev/null
}

add_condition "new_violations" "GT" "0"
add_condition "new_security_hotspots_reviewed" "LT" "100"
add_condition "new_coverage" "LT" "80"
add_condition "new_duplicated_lines_density" "GT" "3"

echo "Assigning Quality Gate to ${PROJECT_KEY}..."

curl -sSf \
    -u "${SONAR_TOKEN}:" \
    -X POST \
    --data-urlencode "gateName=${GATE_NAME}" \
    --data-urlencode "projectKey=${PROJECT_KEY}" \
    "${SONAR_URL}/api/qualitygates/select" > /dev/null

echo "Quality Gate configured successfully."