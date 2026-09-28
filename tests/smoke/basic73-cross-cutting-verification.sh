#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/docker-compose.yml"
BASE_URL="${BASE_URL:-http://127.0.0.1:3000}"

(
  cd "$ROOT_DIR/backend"
  mvn verify
)
(
  cd "$ROOT_DIR/frontend"
  npm run typecheck
  npm run test -- --run
  npm run build
)

docker compose -f "$COMPOSE_FILE" up -d --wait

mapfile -t running_services < <(docker compose -f "$COMPOSE_FILE" ps --status running --services)
for required_service in backend frontend database; do
  if ! printf '%s\n' "${running_services[@]}" | grep -Fxq "$required_service"; then
    printf 'FAIL: required service %s is not running\n' "$required_service" >&2
    exit 1
  fi
done

health_body="$(mktemp)"
trap 'rm -f "$health_body"' EXIT
health_status="$(curl --fail-with-body --silent --show-error --output "$health_body" --write-out '%{http_code}' "$BASE_URL/api/health")"
if [[ "$health_status" != "200" ]]; then
  printf 'FAIL: /api/health returned HTTP %s\n' "$health_status" >&2
  exit 1
fi
if ! grep -Fq '"success":true' "$health_body" || ! grep -Fq '"status":"UP"' "$health_body"; then
  printf 'FAIL: /api/health did not return the expected ApiResponse health payload\n' >&2
  exit 1
fi

printf 'BASIC-73 cross-cutting build, regression, compose, and health verification passed.\n'
