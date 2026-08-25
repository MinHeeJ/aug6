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
  if grep -RInE --exclude-dir=node_modules --exclude-dir=dist --exclude-dir=build --exclude-dir=target --exclude-dir=.git "$pattern" "$path" >/tmp/basic22-grep-hit.txt 2>/dev/null; then
    echo "FAIL $label" >&2
    cat /tmp/basic22-grep-hit.txt >&2
    rm -f /tmp/basic22-grep-hit.txt
    exit 1
  fi
  rm -f /tmp/basic22-grep-hit.txt
  echo "OK $label"
}

require_file backend/src/test/java/kr/ac/knue/commonfoundation/common/Basic22CrossCuttingVerificationTest.java
require_file frontend/src/pages/admin/BASIC22-cross-cutting.test.tsx
require_file tests/e2e/basic22-cross-cutting.spec.ts
require_file frontend/src/pages/admin/SCR-MESSAGE-MGMT.tsx
require_file frontend/src/pages/admin/SCR-NOTICE-MGMT.tsx
require_file frontend/src/pages/admin/SCR-HELP-MGMT.tsx
require_file frontend/src/pages/admin/SCR-MANUAL-MGMT.tsx
require_file infra/docker-compose.yml

# T019: default 20 rows and 20/50/100 changes are covered by backend contract and UI helper tests.
require_grep "DefaultToTwentyItems|default 20" backend/src/test/java/kr/ac/knue/commonfoundation/common/Basic22CrossCuttingVerificationTest.java "T019 backend contract verifies default 20 rows"
require_grep "TwentyFiftyOneHundred|20/50/100" backend/src/test/java/kr/ac/knue/commonfoundation/common/Basic22CrossCuttingVerificationTest.java "T019 backend contract verifies 20/50/100 page sizes"
require_grep "page=0&size=20|page=0&pageSize=20" frontend/src/pages/admin/BASIC22-cross-cutting.test.tsx "T019 frontend API helpers default to 20"
require_grep "message-page-size-select|notice-page-size-select|help-page-size-select|manual-page-size-select" tests/e2e/basic22-cross-cutting.spec.ts "T019 E2E covers every new page-size selector"

# T020: confirmation before writes, post-action guidance, and safe user/system error separation.
require_grep "window\.confirm|saveConfirm|createConfirm" frontend/src/pages/admin/SCR-MESSAGE-MGMT.tsx "T020 message save requires confirmation"
require_grep "window\.confirm|saveConfirm|createConfirm" frontend/src/pages/admin/SCR-NOTICE-MGMT.tsx "T020 notice save requires confirmation"
require_grep "window\.confirm|saveConfirm|createConfirm" frontend/src/pages/admin/SCR-HELP-MGMT.tsx "T020 help save requires confirmation"
require_grep "window\.confirm|saveConfirm|createConfirm" frontend/src/pages/admin/SCR-MANUAL-MGMT.tsx "T020 manual create requires confirmation"
require_grep "saveSuccess|createSuccess|SuccessState" frontend/src/pages/admin/BASIC22-cross-cutting.test.tsx "T020 post-processing success guidance is tested"
require_grep "handleUnexpectedError|Unexpected system error|INTERNAL_ERROR" backend/src/main/java/kr/ac/knue/commonfoundation/common/api/GlobalExceptionHandler.java "T020 system details are logged separately from user guidance"
reject_grep "password=secret" frontend/src/pages/admin "T020 frontend user guidance does not contain system secret samples"

# T021: quickstart browser/device smoke range is captured in executable E2E scope.
require_grep "Edge desktop|Chrome desktop|Safari desktop|Opera desktop|Whale desktop|iPadOS tablet|Android tablet" tests/e2e/basic22-cross-cutting.spec.ts "T021 browser and tablet smoke matrix is explicit"
require_grep "SCR-MESSAGE-MGMT|SCR-NOTICE-MGMT|SCR-HELP-MGMT|SCR-MANUAL-MGMT" tests/e2e/basic22-cross-cutting.spec.ts "T021 BASIC-22 routes are included in smoke scope"
require_grep "services:" infra/docker-compose.yml "T021 compose remains branch-preview runnable"

reject_grep "http://localhost:8080|https?://127\.0\.0\.1:8080" frontend/src "T021 frontend API calls remain relative"

echo "basic22 cross-cutting verification passed"
