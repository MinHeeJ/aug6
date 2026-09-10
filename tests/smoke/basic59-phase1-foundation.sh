#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MIGRATION_DIR="$ROOT_DIR/backend/src/main/resources/db/migration"
COMPOSE_FILE="$ROOT_DIR/infra/docker-compose.yml"
CONTRACT_CLASS="$ROOT_DIR/backend/src/main/java/kr/ac/knue/commonfoundation/basic59/Basic59FoundationContract.java"
AUTH_FILTER="$ROOT_DIR/backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java"
MIGRATION_FILE="$MIGRATION_DIR/V56__basic59_phase1_foundation_reservations.sql"

failures=()
require_path() {
  local path="$1"
  if [[ ! -e "$ROOT_DIR/$path" ]]; then
    failures+=("missing required path: $path")
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
reject_text() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if [[ -f "$file" ]] && grep -Eiq "$pattern" "$file"; then
    failures+=("forbidden $label in ${file#$ROOT_DIR/}")
  fi
}

require_path "backend"
require_path "frontend"
require_path "infra/docker-compose.yml"
require_path "backend/src/main/resources/db/migration"
require_path "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java"
require_path "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java"
require_path "backend/src/main/java/kr/ac/knue/commonfoundation/auth/CurrentUser.java"
require_path "backend/src/main/java/kr/ac/knue/commonfoundation/permissions/EffectivePermissionService.java"
require_path "backend/src/main/resources/db/migration/V55__basic54_report_management_foundation.sql"

require_text "$COMPOSE_FILE" '^services:$' "single compose services root"
require_text "$COMPOSE_FILE" '^  backend:$' "backend app service"
require_text "$COMPOSE_FILE" '^  frontend:$' "frontend app service"
require_text "$COMPOSE_FILE" 'postgres:16' "PostgreSQL 16 runtime"
require_text "$COMPOSE_FILE" 'context: ../backend' "static backend context"
require_text "$COMPOSE_FILE" 'context: ../frontend' "static frontend context"

require_text "$CONTRACT_CLASS" 'RESERVED_MENU_IDS = Map\.of\(' "BASIC-59 reserved menu id map"
require_text "$CONTRACT_CLASS" '"SCR-EVALUATION-ELEMENT-MGMT-ITEMS", 565L' "FR-018 menu id 565 reservation"
require_text "$CONTRACT_CLASS" '"SCR-PARTICIPATION-RATE-OPERATION", 566L' "FR-019 menu id 566 reservation"
require_text "$CONTRACT_CLASS" '"SCR-MANAGEMENT-ITEM-EVAL-SCORES", 567L' "FR-020 menu id 567 reservation"
require_text "$CONTRACT_CLASS" '"SCR-COURSE-AREA-GROUP-GRADES", 568L' "FR-024 menu id 568 reservation"
require_text "$CONTRACT_CLASS" 'V56__basic59_phase1_foundation_reservations\.sql' "reserved migration filename"
require_text "$CONTRACT_CLASS" 'maxExistingMenuId=564' "existing menu max id evidence"
require_text "$CONTRACT_CLASS" 'SessionCookie|COMMON_FOUNDATION_SESSION|CurrentUser|EffectivePermissionService' "auth reuse checklist"
require_text "$CONTRACT_CLASS" 'no-new-auth|no-new-user-table|no-new-org-table|no-new-role-table|no-second-compose' "review checklist prohibitions"

require_text "$AUTH_FILTER" 'Basic59FoundationContract' "AuthenticationFilter uses BASIC-59 route map"
require_text "$MIGRATION_FILE" 'CREATE TABLE IF NOT EXISTS basic59_phase1_foundation_reservations' "phase 1 reservation registry table"
require_text "$MIGRATION_FILE" 'COMMENT ON TABLE basic59_phase1_foundation_reservations' "registry table comment"
require_text "$MIGRATION_FILE" "'SCR-EVALUATION-ELEMENT-MGMT-ITEMS'.*565" "FR-018 reservation row"
require_text "$MIGRATION_FILE" "'SCR-PARTICIPATION-RATE-OPERATION'.*566" "FR-019 reservation row"
require_text "$MIGRATION_FILE" "'SCR-MANAGEMENT-ITEM-EVAL-SCORES'.*567" "FR-020 reservation row"
require_text "$MIGRATION_FILE" "'SCR-COURSE-AREA-GROUP-GRADES'.*568" "FR-024 reservation row"
require_text "$MIGRATION_FILE" 'ON CONFLICT \(screen_id\) DO UPDATE' "idempotent reservation upsert"

reject_text "$MIGRATION_FILE" 'CREATE TABLE IF NOT EXISTS (users|roles|organizations|user_roles|auth|sessions)' "new auth/user/org/role/common table creation"
reject_text "$MIGRATION_FILE" 'INSERT INTO (users|roles|organizations|user_roles)' "new auth/user/org/role seed"
reject_text "$MIGRATION_FILE" 'CREATE TABLE IF NOT EXISTS (evaluation_element_management_item_settings|participation_rate_operation_settings|management_item_score_settings|course_area_group_evaluation_grades)' "later phase BASIC-59 business table"

if (( ${#failures[@]} > 0 )); then
  printf 'BASIC-59 phase 1 foundation failed:\n' >&2
  printf ' - %s\n' "${failures[@]}" >&2
  exit 1
fi

printf 'BASIC-59 phase 1 foundation ready.\n'
