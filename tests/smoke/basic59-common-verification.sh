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
  if grep -RInE --exclude-dir=node_modules --exclude-dir=dist --exclude-dir=build --exclude-dir=target --exclude-dir=.git "$pattern" "$path" >/tmp/basic59-grep-hit.txt 2>/dev/null; then
    echo "FAIL $label" >&2
    cat /tmp/basic59-grep-hit.txt >&2
    rm -f /tmp/basic59-grep-hit.txt
    exit 1
  fi
  rm -f /tmp/basic59-grep-hit.txt
  echo "OK $label"
}

smoke_url() {
  local url="$1"
  local label="$2"
  local status
  status="$(curl -fsS -o /tmp/basic59-smoke-response.txt -w '%{http_code}' "$url")" || fail "$label curl failed"
  [ "$status" = "200" ] || fail "$label returned HTTP $status"
  echo "OK $label"
}

basic59_pages=(
  frontend/src/pages/admin/SCR-EVALUATION-ELEMENT-MGMT-ITEMS.tsx
  frontend/src/pages/admin/SCR-PARTICIPATION-RATE-OPERATION.tsx
  frontend/src/pages/admin/SCR-MANAGEMENT-ITEM-EVAL-SCORES.tsx
  frontend/src/pages/admin/SCR-COURSE-AREA-GROUP-GRADES.tsx
)

basic59_page_tests=(
  frontend/src/pages/admin/SCR-EVALUATION-ELEMENT-MGMT-ITEMS.test.tsx
  frontend/src/pages/admin/SCR-PARTICIPATION-RATE-OPERATION.test.tsx
  frontend/src/pages/admin/SCR-MANAGEMENT-ITEM-EVAL-SCORES.test.tsx
  frontend/src/pages/admin/SCR-COURSE-AREA-GROUP-GRADES.test.tsx
)

basic59_api_tests=(
  backend/src/test/java/kr/ac/knue/commonfoundation/basic59/EvaluationElementManagementItemApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic59/ParticipationRateOperationSettingApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic59/ManagementItemEvaluationScoreApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic59/CourseAreaGroupGradeApiTest.java
  backend/src/test/java/kr/ac/knue/commonfoundation/basic59/Basic59CommonVerificationApiTest.java
)

for file in "${basic59_pages[@]}"; do
  require_file "$file"
  require_grep 'data-screen-id="SCR-' "$file" "BASIC-59 screen id exists in $file"
  require_grep 'data-testid="[^"]*page-size-select"' "$file" "REQ-1753 page size selector exists in $file"
  require_grep '20.*50.*100|\[20, 50, 100\]' "$file" "REQ-1753 20/50/100 page sizes exist in $file"
  require_grep 'downloadCsv|엑셀 내려받기' "$file" "REQ-1754 existing CSV/Excel download policy exists in $file"
done

for file in "${basic59_page_tests[@]}"; do
  require_file "$file"
  require_grep '20건' "$file" "REQ-1753 page test checks 20건 in $file"
  require_grep '50건' "$file" "REQ-1753 page test checks 50건 in $file"
  require_grep '100건' "$file" "REQ-1753 page test checks 100건 in $file"
  require_grep '엑셀 내려받기' "$file" "REQ-1754 page test checks Excel action in $file"
done

for file in "${basic59_api_tests[@]}"; do
  require_file "$file"
done

require_grep 'everyBasic59SaveApiReturnsApiErrorFieldsForRequiredServerValidationForReq1758' backend/src/test/java/kr/ac/knue/commonfoundation/basic59/Basic59CommonVerificationApiTest.java "REQ-1758 server validation/common ApiError.fields test exists"
require_grep 'everyBasic59ListApiDefaultsToTwentyAcceptsOnlyCommonPageSizesAndPropagatesRequestIdsForReq1753Req1759' backend/src/test/java/kr/ac/knue/commonfoundation/basic59/Basic59CommonVerificationApiTest.java "REQ-1753/REQ-1759 common page/request id test exists"
require_grep 'meta.requestId|traceId' backend/src/test/java/kr/ac/knue/commonfoundation/basic59/Basic59CommonVerificationApiTest.java "REQ-1759 request id and trace assertions exist"

reject_grep '\? IS NULL OR|:param IS NULL OR|#\{[A-Za-z0-9_]+\} IS NULL OR|COALESCE\(' backend/src/main/resources/mapper/basic59 "optional filters do not use null-bound SQL predicates"
reject_grep 'http://localhost:8080|https?://127\.0\.0\.1:8080' frontend/src "frontend API calls remain relative"

if [ "${RUN_BASIC59_DOCKER_SMOKE:-0}" = "1" ]; then
  docker compose -f infra/docker-compose.yml up -d --wait
  docker compose -f infra/docker-compose.yml ps
  smoke_url "${BASIC59_HEALTH_URL:-http://localhost:3000/api/health}" "REQ-1795 /api/health"
else
  echo "SKIP REQ-1794/REQ-1795 docker runtime smoke; set RUN_BASIC59_DOCKER_SMOKE=1 to execute compose/health checks"
fi

echo "basic59 common verification passed"
