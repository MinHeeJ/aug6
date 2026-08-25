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
  if grep -RInE --exclude-dir=node_modules --exclude-dir=dist --exclude-dir=build --exclude-dir=target --exclude-dir=.git "$pattern" "$path" >/tmp/basic17-grep-hit.txt 2>/dev/null; then
    echo "FAIL $label" >&2
    cat /tmp/basic17-grep-hit.txt >&2
    rm -f /tmp/basic17-grep-hit.txt
    exit 1
  fi
  rm -f /tmp/basic17-grep-hit.txt
  echo "OK $label"
}

require_file frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx
require_file frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.test.tsx
require_file frontend/src/components/States.tsx
require_file frontend/src/app/router.tsx
require_file frontend/src/api/apiClient.ts
require_file frontend/vite.config.ts
require_file tests/e2e/basic17-cross-cutting.spec.ts
require_file tests/smoke/auth-health.sh
require_file backend/src/main/java/kr/ac/knue/commonfoundation/privacy/PrivacyCryptoService.java
require_file backend/src/main/java/kr/ac/knue/commonfoundation/privacy/HmacSearchIdentifierAdapter.java
require_file backend/src/main/resources/mapper/privacy/PrivacyFieldPolicyMapper.xml
require_file backend/src/main/resources/db/migration/V6__basic17_privacy_field_policies.sql
require_file infra/docker-compose.yml

# T041: UI contract coverage for Excel/export denial, required input blocking, confirmation and result notice.
require_grep "exportAllowedYn" tests/e2e/basic17-cross-cutting.spec.ts "T041 Excel/export permission denial is covered by a UI/API smoke scenario"
require_grep "window\.confirm|저장 전 확인|저장하시겠습니까" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx "T041 save action requires confirmation"
require_grep "저장되었습니다|SuccessState" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx "T041 save action shows success result"
require_grep "changeReason.*\*|개인정보 필드.*\*" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx "T041 required fields are visibly marked"
require_grep "fieldErrors" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx "T041 field-level validation blocks invalid save responses"
require_grep "필수.*저장.*차단|field-level" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.test.tsx "T041 frontend unit test covers required-value blocking"

# T042: standard shell/tokens, accessibility hooks, and temporary-data cleanup.
require_grep "AdminShell" frontend/src/app/router.tsx "T042 privacy UI uses the shared admin shell route system"
require_grep "LoadingState|EmptyState|ErrorState|PermissionState|SuccessState" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx "T042 standard state components are used"
require_grep "bg-lightsecondary|text-primary|border-ld|text-muted" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx "T042 existing design tokens are reused"
require_grep "<label|aria-hidden=\"true\"|role=\"status\"" frontend/src/pages/admin/SCR-PRIVACY-POLICY-MGMT.tsx "T042 labels and accessible icon/status patterns exist"
require_grep "trap cleanup EXIT" tests/smoke/auth-health.sh "T042 auth smoke registers cleanup trap"
require_grep "rm -f .*TEMP_FILES" tests/smoke/auth-health.sh "T042 auth smoke deletes temporary files after use"

# T043: encryption, performance, size/security, and supported-environment smoke boundaries.
require_grep "AES/GCM/NoPadding|GCMParameterSpec|SecureRandom" backend/src/main/java/kr/ac/knue/commonfoundation/privacy/PrivacyCryptoService.java "T043 AES-256-GCM encryption boundary exists"
require_grep "암호화 키가 설정되지 않았습니다|KEY_MISSING|AEADBadTagException" backend/src/main/java/kr/ac/knue/commonfoundation/privacy/PrivacyCryptoService.java "T043 missing key and tamper failure paths exist"
require_grep "auditRecorder\.record" backend/src/main/java/kr/ac/knue/commonfoundation/privacy/PrivacyCryptoService.java "T043 decrypt audit recording exists"
require_grep "HmacSHA256|normalizeKey" backend/src/main/java/kr/ac/knue/commonfoundation/privacy/HmacSearchIdentifierAdapter.java "T043 HMAC search identifier exists"
require_grep "CREATE INDEX IF NOT EXISTS" backend/src/main/resources/db/migration/V6__basic17_privacy_field_policies.sql "T043 privacy query indexes exist"
reject_grep "(\?|:|#\{[A-Za-z0-9_.]+\}) IS NULL OR|COALESCE\(" backend/src/main/resources/mapper "T043 optional SQL filters avoid null-bound predicates"
reject_grep "http://localhost:8080|https?://127\.0\.0\.1:8080" frontend/src "T043 frontend API calls do not hardcode backend origins"
require_grep "allowedHosts" frontend/vite.config.ts "T043 Vite preview host policy is configured"
require_grep "iPad|Android|Windows|macOS|Chrome|Safari|Edge|Opera|Whale" tests/e2e/basic17-cross-cutting.spec.ts "T043 supported environment smoke matrix is documented in executable spec"

# Docker/preview static smoke.
require_grep "services:" infra/docker-compose.yml "T043 compose has top-level services"
require_grep "healthcheck:" infra/docker-compose.yml "T043 compose declares health checks"
require_grep "postgres:16" infra/docker-compose.yml "T043 PostgreSQL 16 image is used"
require_grep "node:20\.11\.0-alpine AS build" frontend/Dockerfile "T043 frontend Dockerfile uses Node 20 build stage"

echo "basic17 cross-cutting verification passed"
