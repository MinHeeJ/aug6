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
  if grep -RInE --exclude-dir=node_modules --exclude-dir=dist --exclude-dir=build --exclude-dir=target --exclude-dir=.git "$pattern" "$path" >/tmp/basic32-grep-hit.txt 2>/dev/null; then
    echo "FAIL $label" >&2
    cat /tmp/basic32-grep-hit.txt >&2
    rm -f /tmp/basic32-grep-hit.txt
    exit 1
  fi
  rm -f /tmp/basic32-grep-hit.txt
  echo "OK $label"
}

smoke_url() {
  local url="$1"
  local label="$2"
  local status
  status="$(curl -fsS -o /tmp/basic32-smoke-response.txt -w '%{http_code}' "$url")" || fail "$label curl failed"
  [ "$status" = "200" ] || fail "$label returned HTTP $status"
  echo "OK $label"
}

basic32_pages=(
  frontend/src/pages/admin/SCR-EVALUATION-ORG-MAPPING.tsx
  frontend/src/pages/admin/SCR-BUSINESS-STATUS-CODE.tsx
  frontend/src/pages/admin/SCR-BUSINESS-STATUS-TRANSITION.tsx
  frontend/src/pages/admin/SCR-REJECTION-REASON.tsx
  frontend/src/pages/admin/SCR-DATA-CHANGE-HISTORY.tsx
  frontend/src/pages/admin/SCR-DELETED-BUSINESS-DATA.tsx
)

basic32_page_tests=(
  frontend/src/pages/admin/SCR-EVALUATION-ORG-MAPPING.test.tsx
  frontend/src/pages/admin/SCR-BUSINESS-STATUS-CODE.test.tsx
  frontend/src/pages/admin/SCR-BUSINESS-STATUS-TRANSITION.test.tsx
  frontend/src/pages/admin/SCR-REJECTION-REASON.test.tsx
  frontend/src/pages/admin/SCR-DATA-CHANGE-HISTORY.test.tsx
  frontend/src/pages/admin/SCR-DELETED-BUSINESS-DATA.test.tsx
)

basic32_api_tests=(
  backend/src/test/java/kr/ac/knue/commonfoundation/basic32/EvaluationOrganizationMappingApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic32/BusinessStatusCodeApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic32/BusinessStatusTransitionApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic32/RejectionReasonApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic32/DataChangeHistoryApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic32/DeletedBusinessDataApiTest.java
)

require_file backend/src/test/java/kr/ac/knue/commonfoundation/basic32/Basic32CommonVerificationTest.java
require_file frontend/src/pages/admin/BASIC32-common-verification.test.tsx
require_file tests/e2e/basic32-common-verification.spec.ts
require_file infra/docker-compose.yml
require_file frontend/Dockerfile
require_file backend/Dockerfile
require_file .gitignore
require_file frontend/.dockerignore
require_file backend/.dockerignore

for file in "${basic32_pages[@]}"; do
  require_file "$file"
  require_grep 'data-screen-id="SCR-' "$file" "T1000 accessibility/smoke screen id exists in $file"
  require_grep 'data-testid="[^"]*page-size-select"' "$file" "T999 page size selector test id exists in $file"
  require_grep '(<label[^>]*>|aria-label=)' "$file" "T1000 accessibility label or aria-label exists in $file"
  require_grep '20.*50.*100|\[20, 50, 100\]' "$file" "T999 20/50/100 options exist in $file"
done

for file in "${basic32_page_tests[@]}"; do
  require_file "$file"
  require_grep '20건' "$file" "T999 page test checks 20건 in $file"
  require_grep '50건' "$file" "T999 page test checks 50건 in $file"
  require_grep '100건' "$file" "T999 page test checks 100건 in $file"
done

for file in "${basic32_api_tests[@]}"; do
  require_file "$file"
  require_grep 'size.*20|20.*size' "$file" "T999 API test checks default size 20 in $file"
done

require_grep 'everyBasic32ListApiDefaultsToTwentyAndAcceptsOnlyCommonUiPageSizesForReq749' backend/src/test/java/kr/ac/knue/commonfoundation/basic32/Basic32CommonVerificationTest.java "T999 all BASIC-32 API page sizes covered by common MockMvc test"
require_grep 'applies default 20 rows and 20/50/100 selectable sizes' frontend/src/pages/admin/BASIC32-common-verification.test.tsx "T999 all BASIC-32 UI page sizes covered by common Vitest test"
require_grep 'new business lists default to 20 and expose 20/50/100 page size choices' tests/e2e/basic32-common-verification.spec.ts "T1000 browser smoke includes list page size check"
require_grep 'tablet and desktop browser smoke viewports' tests/e2e/basic32-common-verification.spec.ts "T1000 browser viewport smoke matrix exists"
require_grep 'without leaking stack details' tests/e2e/basic32-common-verification.spec.ts "T1000 security smoke checks response leakage"

reject_grep '\? IS NULL OR|:param IS NULL OR|#\{[A-Za-z0-9_]+\} IS NULL OR|COALESCE\(' backend/src/main/resources/mapper "T1000 optional filters do not use null-bound SQL predicates"
reject_grep 'http://localhost:8080|https?://127\.0\.0\.1:8080' frontend/src "T1000 frontend API calls remain relative"
reject_grep 'password|secret|token' backend/src/main/java/kr/ac/knue/commonfoundation/basic32 "T1000 BASIC-32 source does not hardcode sensitive literals"

if [ -d frontend/dist ]; then
  bytes="$(du -sb frontend/dist | awk '{print $1}')"
  [ "$bytes" -le 3145728 ] || fail "T1000 frontend dist exceeds 3MB: $bytes bytes"
  echo "OK T1000 frontend dist size <= 3MB ($bytes bytes)"
else
  echo "SKIP T1000 frontend dist size check; frontend/dist is absent because build was not run in this codegen phase"
fi

if [ "${RUN_BASIC32_DOCKER_SMOKE:-0}" = "1" ]; then
  docker compose -f infra/docker-compose.yml up -d --wait
  smoke_url "${BASIC32_BACKEND_HEALTH_URL:-http://localhost:8080/api/health}" "T1000 backend /api/health"
  frontend_base="${BASIC32_FRONTEND_BASE_URL:-http://localhost:3000}"
  for route in \
    /admin/evaluation-organization-mappings \
    /admin/business-status-codes \
    /admin/business-status-transitions \
    /admin/rejection-reasons \
    /admin/data-change-histories \
    /admin/deleted-business-data; do
    smoke_url "${frontend_base}${route}" "T1000 UI route ${route}"
  done
else
  echo "SKIP T1000 docker/browser runtime smoke; set RUN_BASIC32_DOCKER_SMOKE=1 to execute compose/health/UI route checks"
fi

echo "basic32 common verification passed"
