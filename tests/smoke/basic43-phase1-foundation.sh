#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MIGRATION_DIR="$ROOT_DIR/backend/src/main/resources/db/migration"
OPENAPI_FILE="$ROOT_DIR/backend/src/test/resources/contracts/openapi.yaml"
COMPOSE_FILE="$ROOT_DIR/infra/docker-compose.yml"

failures=()
require_path() {
  local path="$1"
  if [[ ! -e "$ROOT_DIR/$path" ]]; then
    failures+=("missing contract: $path")
  fi
}
require_text() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if [[ ! -f "$file" ]] || ! grep -Eq "$pattern" "$file"; then
    failures+=("missing $label in ${file#$ROOT_DIR/}")
  fi
}
require_migration_text() {
  local pattern="$1"
  local label="$2"
  if ! grep -REq "$pattern" "$MIGRATION_DIR"; then
    failures+=("missing migration contract: $label")
  fi
}

require_path "backend"
require_path "frontend"
require_path "infra/docker-compose.yml"
require_path "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationPort.java"
require_path "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java"
require_path "backend/src/main/resources/db/migration"

require_text "$COMPOSE_FILE" '^services:$' "compose services root"
require_text "$COMPOSE_FILE" '^  backend:$' "backend app service"
require_text "$COMPOSE_FILE" '^  frontend:$' "frontend app service"
require_text "$COMPOSE_FILE" 'postgres:16' "PostgreSQL 16 image"
require_text "$COMPOSE_FILE" 'context: ../backend' "static backend build context"
require_text "$COMPOSE_FILE" 'context: ../frontend' "static frontend build context"

require_text "$OPENAPI_FILE" 'SessionCookie' "SessionCookie security scheme"
require_text "$OPENAPI_FILE" 'operationId: getCurrentUser' "getCurrentUser operation"
require_text "$OPENAPI_FILE" 'operationId: getHealth' "getHealth operation"
require_text "$OPENAPI_FILE" 'operationId: listBusinessStatusTransitions' "business status transition API"
require_text "$OPENAPI_FILE" 'operationId: listRejectionReasons' "rejection reason API"
require_text "$OPENAPI_FILE" 'operationId: listDepartmentChairConfirmPeriods' "department chair confirm period API"
require_text "$OPENAPI_FILE" 'operationId: listAppealPeriods' "appeal period API"

require_migration_text "'R09'" "existing R09 seed/reference"
require_migration_text '^CREATE TABLE IF NOT EXISTS business_status_transitions' "business_status_transitions table"
require_migration_text '^CREATE TABLE IF NOT EXISTS rejection_reasons' "rejection_reasons table"
require_migration_text '^CREATE TABLE IF NOT EXISTS department_chair_confirm_period_settings' "department_chair_confirm_period_settings table"
require_migration_text '^CREATE TABLE IF NOT EXISTS appeal_period_settings' "appeal_period_settings table"

if (( ${#failures[@]} > 0 )); then
  printf 'BASIC-43 phase 1 foundation precondition failed:\n' >&2
  printf ' - %s\n' "${failures[@]}" >&2
  exit 1
fi

printf 'BASIC-43 phase 1 foundation precondition ready.\n'
