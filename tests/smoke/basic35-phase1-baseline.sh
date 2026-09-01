#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

fail() {
  echo "FAIL $1" >&2
  exit 1
}

require_file() {
  local file="$1"
  [ -f "$file" ] || fail "missing required file: $file"
  echo "OK file $file"
}

require_grep() {
  local pattern="$1"
  local file="$2"
  local label="$3"
  grep -Eq "$pattern" "$file" || fail "$label"
  echo "OK $label"
}

reject_grep() {
  local pattern="$1"
  local path="$2"
  local label="$3"
  local hit_file
  hit_file="$(mktemp)"
  if python3 - "$pattern" "$path" "$hit_file" <<'PY'
import os
import re
import sys

pattern, root, hit_file = sys.argv[1], sys.argv[2], sys.argv[3]
regex = re.compile(pattern)
skip_dirs = {'node_modules', 'dist', 'build', 'target', '.git'}
hits = []
for current_root, dirs, files in os.walk(root):
    dirs[:] = [name for name in dirs if name not in skip_dirs]
    for name in files:
        file_path = os.path.join(current_root, name)
        try:
            with open(file_path, 'r', encoding='utf-8') as handle:
                for line_no, line in enumerate(handle, start=1):
                    if regex.search(line):
                        hits.append(f"{file_path}:{line_no}:{line.rstrip()}")
        except (UnicodeDecodeError, OSError):
            continue
with open(hit_file, 'w', encoding='utf-8') as handle:
    handle.write('\n'.join(hits))
sys.exit(1 if hits else 0)
PY
  then
    rm -f "$hit_file"
    echo "OK $label"
  else
    echo "FAIL $label" >&2
    cat "$hit_file" >&2
    rm -f "$hit_file"
    exit 1
  fi
}

require_file backend/src/main/resources/db/migration/V2__common_foundation_seed.sql
require_file backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java
require_file backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java
require_file backend/src/main/java/kr/ac/knue/commonfoundation/health/HealthController.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/businessperiod/BusinessPeriodBaselineVerificationTest.java
require_file frontend/src/pages/LoginPage.tsx
require_file frontend/src/pages/LoginPage.test.tsx
require_file frontend/src/api/apiClient.ts
require_file infra/docker-compose.yml
require_file .gitignore
require_file backend/.dockerignore
require_file frontend/.dockerignore

for role in R01 R02 R03 R04 R05 R06 R07 R08 R09; do
  require_grep "'${role}'" backend/src/main/resources/db/migration/V2__common_foundation_seed.sql "T001-T004 existing ${role} role seed is present"
done

require_grep "SELECT user_id, 'R09'" backend/src/main/resources/db/migration/V2__common_foundation_seed.sql "T001 existing admin user reuses R09 role seed"
require_grep "SELECT 'ROLE', 'R09', menu_id, 'ALLOW'" backend/src/main/resources/db/migration/V2__common_foundation_seed.sql "T002 existing R09 menu permission seed is present"
reject_grep "BUSINESS_PERIOD_ADMIN|PERIOD_ADMIN|R10|R11|R12" backend/src/main/resources/db/migration "T003/T004 no new business-period role codes are defined in migrations"

require_grep "SESSION_COOKIE = \"COMMON_FOUNDATION_SESSION\"" backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java "T001-T004 SessionCookie name remains stable"
require_grep "httpOnly\(true\)" backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java "T001-T004 SessionCookie remains HTTP-only"
require_grep "sameSite\(\"Lax\"\)" backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java "T001-T004 SessionCookie SameSite guard remains Lax"
require_grep "AuthController\.SESSION_COOKIE\.equals\(cookie\.getName\(\)\)" backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java "T001-T004 backend filter extracts only the SessionCookie"
require_grep "path\.startsWith\(\"/api/admin/\"\).*path\.startsWith\(\"/api/business/\"\)" backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java "T001-T004 backend menu guard covers admin/business APIs"
require_grep "permissionService\.canAccess\(user\.userId\(\), user\.roles\(\), pathToUiRoute\(path\)\)" backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java "T001-T004 backend menu guard delegates to existing EffectivePermissionService"

require_grep "canAccessAdminRoute" frontend/src/pages/LoginPage.tsx "T001-T004 frontend route guard function exists"
require_grep "return hasMenuUrl\(user\.menus, path\)" frontend/src/pages/LoginPage.tsx "T001-T004 frontend guard uses current user menus"
require_grep "uses menu URLs rather than role codes alone as the frontend route guard" frontend/src/pages/LoginPage.test.tsx "T001-T004 frontend guard regression test exists"

require_grep "@GetMapping\(\"/api/health\"\)" backend/src/main/java/kr/ac/knue/commonfoundation/health/HealthController.java "T005 /api/health endpoint is present"
require_grep "status\", \"UP\"" backend/src/main/java/kr/ac/knue/commonfoundation/health/HealthController.java "T005 /api/health reports UP"
require_grep "apiRequest<HealthStatus>\(\"/api/health\"\)" frontend/src/api/apiClient.ts "T005 frontend health client uses relative /api/health"
require_grep "healthcheck:" infra/docker-compose.yml "T005 compose healthchecks remain configured"
require_grep "http://localhost:8080/api/health" infra/docker-compose.yml "T005 backend container healthcheck targets /api/health internally"
reject_grep "http://localhost:8080|https?://127\.0\.0\.1:8080" frontend/src "T005 frontend API calls remain relative"

if [ "${RUN_BASIC35_COMPOSE_CONFIG:-0}" = "1" ]; then
  docker compose -f infra/docker-compose.yml config >/tmp/basic35-compose-config.yml
  echo "OK T005 docker compose config validation"
else
  echo "SKIP T005 docker compose config validation; set RUN_BASIC35_COMPOSE_CONFIG=1 to run it"
fi

if [ "${RUN_BASIC35_RUNTIME_SMOKE:-0}" = "1" ]; then
  curl -fsS "${BASIC35_BACKEND_HEALTH_URL:-http://localhost:8080/api/health}" >/tmp/basic35-health-response.json
  grep -q '"success"[[:space:]]*:[[:space:]]*true' /tmp/basic35-health-response.json || fail "T005 runtime /api/health did not return ApiResponse success=true"
  grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' /tmp/basic35-health-response.json || fail "T005 runtime /api/health did not return UP"
  echo "OK T005 runtime /api/health smoke"
else
  echo "SKIP T005 runtime /api/health smoke; set RUN_BASIC35_RUNTIME_SMOKE=1 after starting the stack"
fi

echo "basic35 phase1 baseline verification passed"
