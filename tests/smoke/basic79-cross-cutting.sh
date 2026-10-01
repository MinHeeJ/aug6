#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-infra/docker-compose.yml}"
FRONTEND_BASE_URL="${FRONTEND_BASE_URL:-http://localhost:3000}"
BACKEND_BASE_URL="${BACKEND_BASE_URL:-http://localhost:8080}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  }
}

require_status() {
  local expected="$1"
  local actual="$2"
  local label="$3"
  if [[ "$actual" != "$expected" ]]; then
    printf 'FAIL %s: expected HTTP %s, received %s\n' "$label" "$expected" "$actual" >&2
    exit 1
  fi
  printf 'OK %s: HTTP %s\n' "$label" "$actual"
}

assert_healthy_services() {
  local services
  services="$(docker compose -f "$COMPOSE_FILE" ps --format '{{.Service}} {{.Health}} {{.State}}')"
  printf '%s\n' "$services"
  for service in database backend frontend; do
    if ! grep -Eq "^${service} (healthy|) (running|healthy)$|^${service} healthy" <<<"$services"; then
      printf 'FAIL %s is not healthy/running after compose wait\n' "$service" >&2
      exit 1
    fi
  done
}

assert_health() {
  local base_url="$1"
  local response_file status
  response_file="$(mktemp)"
  trap 'rm -f "$response_file"' RETURN
  status="$(curl --fail-with-body --silent --show-error --output "$response_file" --write-out '%{http_code}' "${base_url}/api/health")"
  require_status "200" "$status" "${base_url}/api/health"
  grep -q '"success":true' "$response_file"
  grep -q '"status":"UP"' "$response_file"
  rm -f "$response_file"
  trap - RETURN
}

require_command docker
require_command curl

docker compose -f "$COMPOSE_FILE" config --quiet
docker compose -f "$COMPOSE_FILE" up -d --wait
assert_healthy_services
assert_health "$BACKEND_BASE_URL"
assert_health "$FRONTEND_BASE_URL"
printf 'BASIC-79 compose, three-service health, and /api/health smoke passed.\n'
