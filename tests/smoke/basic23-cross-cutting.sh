#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "FAIL $1" >&2
  exit 1
}

require_file() {
  local file="$1"
  [ -f "$file" ] || fail "missing required file: $file"
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
  if grep -RInE --exclude-dir=node_modules --exclude-dir=dist --exclude-dir=build --exclude-dir=target --exclude-dir=.git "$pattern" "$path" >/tmp/basic23-grep-hit.txt 2>/dev/null; then
    echo "FAIL $label" >&2
    cat /tmp/basic23-grep-hit.txt >&2
    rm -f /tmp/basic23-grep-hit.txt
    exit 1
  fi
  rm -f /tmp/basic23-grep-hit.txt
  echo "OK $label"
}

require_file backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchCrossCuttingGuardApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchDefinitionManagementApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchExecutionManagementApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchRetryManagementApiTest.java
require_file frontend/src/pages/admin/SCR-BATCH-DEFINITION-MGMT.test.tsx
require_file frontend/src/pages/admin/SCR-BATCH-EXECUTION-MGMT.test.tsx
require_file frontend/src/pages/admin/SCR-BATCH-RESULT-MGMT.test.tsx
require_file frontend/src/pages/admin/SCR-BATCH-RETRY-MGMT.test.tsx
require_file frontend/.dockerignore
require_file backend/.dockerignore

require_grep "batchListsDefaultToTwentyItemsAcrossFourScreens" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchCrossCuttingGuardApiTest.java "T026 backend list APIs default to 20"
require_grep "20, 50, 100|TwentyFiftyHundred" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchCrossCuttingGuardApiTest.java "T026 backend list APIs cover 20/50/100"
require_grep "pageSizeOptions.*20, 50, 100|20/50/100" frontend/src/pages/admin/SCR-BATCH-DEFINITION-MGMT.test.tsx "T026 definition UI page-size options covered"
require_grep "pageSizeOptions.*20, 50, 100|20/50/100" frontend/src/pages/admin/SCR-BATCH-EXECUTION-MGMT.test.tsx "T026 execution UI page-size options covered"
require_grep "pageSizeOptions.*20, 50, 100|20/50/100" frontend/src/pages/admin/SCR-BATCH-RESULT-MGMT.test.tsx "T026 result UI page-size options covered"
require_grep "pageSizeOptions.*20, 50, 100|20/50/100" frontend/src/pages/admin/SCR-BATCH-RETRY-MGMT.test.tsx "T026 retry UI page-size options covered"

require_grep "REQ-386 OQ" frontend/src/pages/admin/SCR-BATCH-DEFINITION-MGMT.tsx "T027 definition Excel OQ visible"
require_grep "REQ-386 OQ" frontend/src/pages/admin/SCR-BATCH-EXECUTION-MGMT.tsx "T027 execution Excel OQ visible"
require_grep "REQ-386 OQ" frontend/src/pages/admin/SCR-BATCH-RESULT-MGMT.tsx "T027 result Excel OQ visible"
require_grep "REQ-386 OQ" frontend/src/pages/admin/SCR-BATCH-RETRY-MGMT.tsx "T027 retry Excel OQ visible"

require_grep "validateForm|validateRetry" frontend/src/pages/admin/SCR-BATCH-DEFINITION-MGMT.test.tsx "T028 definition required-field validation covered"
require_grep "validateForm|validateRetry" frontend/src/pages/admin/SCR-BATCH-EXECUTION-MGMT.test.tsx "T028 execution required-field validation covered"
require_grep "validateForm|validateRetry" frontend/src/pages/admin/SCR-BATCH-RETRY-MGMT.test.tsx "T028 retry required-field validation covered"
require_grep "saveConfirm|runConfirm|stopConfirm|rerunConfirm|retryConfirm" frontend/src/pages/admin/SCR-BATCH-DEFINITION-MGMT.test.tsx "T029 definition confirmation/result 안내 covered"
require_grep "saveConfirm|runConfirm|stopConfirm|rerunConfirm|retryConfirm" frontend/src/pages/admin/SCR-BATCH-EXECUTION-MGMT.test.tsx "T029 execution confirmation/result 안내 covered"
require_grep "saveConfirm|runConfirm|stopConfirm|rerunConfirm|retryConfirm" frontend/src/pages/admin/SCR-BATCH-RETRY-MGMT.test.tsx "T029 retry confirmation/result 안내 covered"
require_grep "10초 이상|progress" frontend/src/pages/admin/SCR-BATCH-EXECUTION-MGMT.test.tsx "T030 execution progress state covered"
require_grep "10초 이상|progress" frontend/src/pages/admin/SCR-BATCH-RETRY-MGMT.test.tsx "T030 retry progress state covered"

require_grep "meta.requestId" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchDefinitionManagementApiTest.java "T031 definition response meta requestId covered"
require_grep "meta.requestId" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchExecutionManagementApiTest.java "T031 execution/stop/rerun response meta requestId covered"
require_grep "meta.requestId" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchRetryManagementApiTest.java "T031 retry response meta requestId covered"
require_grep "updateOriginalExecutionResult|never\(\).*updateOriginalExecutionResult" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchRetryManagementApiTest.java "T033 original result overwrite guard covered"
reject_grep "update batch_execution_results|delete from batch_execution_logs|delete from batch_execution_results" backend/src/main/resources/mapper/batch "T033 mapper has no original result/log overwrite SQL"
reject_grep "http://localhost:8080|https?://127\.0\.0\.1:8080" frontend/src "T032 frontend API calls remain relative"
require_grep "node_modules|dist|build|coverage|\.git" frontend/.dockerignore "Docker build context ignores frontend generated artifacts"
require_grep "node_modules|dist|build|coverage|\.git" backend/.dockerignore "Docker build context ignores backend generated artifacts"

echo "basic23 cross-cutting verification passed"
