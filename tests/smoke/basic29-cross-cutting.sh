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
  if grep -RInE --exclude-dir=node_modules --exclude-dir=dist --exclude-dir=build --exclude-dir=target --exclude-dir=.git "$pattern" "$path" >/tmp/basic29-grep-hit.txt 2>/dev/null; then
    echo "FAIL $label" >&2
    cat /tmp/basic29-grep-hit.txt >&2
    rm -f /tmp/basic29-grep-hit.txt
    exit 1
  fi
  rm -f /tmp/basic29-grep-hit.txt
  echo "OK $label"
}

smoke_url() {
  local url="$1"
  local label="$2"
  local status
  status="$(curl -fsS -o /tmp/basic29-smoke-response.txt -w '%{http_code}' "$url")" || fail "$label curl failed"
  [ "$status" = "200" ] || fail "$label returned HTTP $status"
  echo "OK $label"
}

require_file backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/audit/BusinessProcessLogApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/audit/SensitiveInformationAccessLogApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/audit/PermissionChangeLogApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionMapperContractTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/audit/BusinessProcessLogMapperContractTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/audit/SensitiveInformationAccessLogMapperContractTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/audit/PermissionChangeLogMapperContractTest.java
require_file tests/e2e/basic29-active-sessions.spec.ts
require_file tests/e2e/basic29-session-termination-history.spec.ts
require_file tests/e2e/basic29-business-process-logs.spec.ts
require_file tests/e2e/basic29-sensitive-information-access-logs.spec.ts
require_file tests/e2e/basic29-permission-change-logs.spec.ts
require_file infra/docker-compose.yml

# T029: every BASIC-29 operation keeps distinct executable assertions for happy/auth/validation/business/side-effect obligations.
require_grep "listActiveSessionsReturnsEnvelopeWithOnlyActiveSessionStatusFields" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 active session list happy contract assertion exists"
require_grep "terminateActiveSessionRequiresReasonAndRecordsRequestIdInResponseMeta" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 terminate happy/request-id assertion exists"
require_grep "terminateActiveSessionRejectsNonR09BeforeServiceSideEffect" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 terminate auth assertion is separate"
require_grep "terminateActiveSessionRequiresReasonBeforeServiceSideEffect" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 terminate validation assertion is separate"
require_grep "serviceRejectsTerminatingNonActiveSessionWithoutMutation" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 terminate business conflict assertion is separate"
require_grep "terminateSessionMutatesOnlyTerminationColumnsAndWritesImmutableAuditRows" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionMapperContractTest.java "T029 terminate side-effect persistence assertion exists"
require_grep "listSessionTerminationHistoriesReturnsEnvelopeWithTerminationTypeReasonAndDateFilters" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 termination history happy assertion exists"
require_grep "listSessionTerminationHistoriesRejectsNonR09BeforeServiceLookup" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 termination history auth assertion is separate"
require_grep "serviceRejectsInvalidSessionTerminationHistoryFilterWithoutMapperLookup" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionManagementApiTest.java "T029 termination history validation assertion is separate"
require_grep "sessionTerminationHistoryListReadsImmutableHistoryWithDynamicFiltersOnly" backend/src/test/java/kr/ac/knue/commonfoundation/securitysessions/ActiveSessionMapperContractTest.java "T029 termination history business/immutability assertion exists"
require_grep "listBusinessProcessLogsReturnsEnvelopeWithActionStatesActorResultAndRequestId" backend/src/test/java/kr/ac/knue/commonfoundation/audit/BusinessProcessLogApiTest.java "T029 business process happy assertion exists"
require_grep "listBusinessProcessLogsRejectsNonR09BeforeMapperLookup" backend/src/test/java/kr/ac/knue/commonfoundation/audit/BusinessProcessLogApiTest.java "T029 business process auth assertion is separate"
require_grep "serviceRejectsInvalidActionResultAndDateRangeWithoutMapperLookup" backend/src/test/java/kr/ac/knue/commonfoundation/audit/BusinessProcessLogApiTest.java "T029 business process validation assertion is separate"
require_grep "businessProcessLogListReadsImmutableRowsWithDynamicFiltersOnly" backend/src/test/java/kr/ac/knue/commonfoundation/audit/BusinessProcessLogMapperContractTest.java "T029 business process immutable/filter side-effect assertion exists"
require_grep "listSensitiveInformationAccessLogsReturnsEnvelopeWithoutProtectedPlaintext" backend/src/test/java/kr/ac/knue/commonfoundation/audit/SensitiveInformationAccessLogApiTest.java "T029 sensitive information happy/no-plaintext assertion exists"
require_grep "listSensitiveInformationAccessLogsRejectsNonR09BeforeMapperLookup" backend/src/test/java/kr/ac/knue/commonfoundation/audit/SensitiveInformationAccessLogApiTest.java "T029 sensitive information auth assertion is separate"
require_grep "serviceRejectsInvalidInformationResultAndDateRangeWithoutMapperLookup" backend/src/test/java/kr/ac/knue/commonfoundation/audit/SensitiveInformationAccessLogApiTest.java "T029 sensitive information validation assertion is separate"
require_grep "sensitiveInformationAccessLogListReadsImmutableRowsWithDynamicFiltersOnly" backend/src/test/java/kr/ac/knue/commonfoundation/audit/SensitiveInformationAccessLogMapperContractTest.java "T029 sensitive information immutable/filter assertion exists"
require_grep "listPermissionChangeLogsReturnsEnvelopeWithApproverChangerBeforeAfterAndReason" backend/src/test/java/kr/ac/knue/commonfoundation/audit/PermissionChangeLogApiTest.java "T029 permission change happy assertion exists"
require_grep "listPermissionChangeLogsRejectsNonR09BeforeMapperLookup" backend/src/test/java/kr/ac/knue/commonfoundation/audit/PermissionChangeLogApiTest.java "T029 permission change auth assertion is separate"
require_grep "serviceRejectsInvalidTargetTypeAndDateRangeWithoutMapperLookup" backend/src/test/java/kr/ac/knue/commonfoundation/audit/PermissionChangeLogApiTest.java "T029 permission change validation assertion is separate"
require_grep "permissionChangeLogListReadsImmutableRowsWithDynamicFiltersOnly" backend/src/test/java/kr/ac/knue/commonfoundation/audit/PermissionChangeLogMapperContractTest.java "T029 permission change business/immutability assertion exists"
reject_grep "\? IS NULL OR|:param IS NULL OR|#\{[A-Za-z0-9_]+\} IS NULL OR|COALESCE\(" backend/src/main/resources/mapper "T029 optional filters do not use null-bound SQL predicates"
reject_grep "http://localhost:8080|https?://127\.0\.0\.1:8080" frontend/src "T029 frontend API calls remain relative"

# T030: run only when explicitly requested by the caller/environment, because it builds and starts containers.
if [ "${RUN_BASIC29_DOCKER_SMOKE:-0}" = "1" ]; then
  compose_files=( -f infra/docker-compose.yml )
  if [ -n "${BASIC29_COMPOSE_OVERRIDE:-}" ]; then
    compose_files+=( -f "${BASIC29_COMPOSE_OVERRIDE}" )
  fi
  docker compose "${compose_files[@]}" up -d --wait
  smoke_url "${BASIC29_BACKEND_HEALTH_URL:-http://localhost:8080/api/health}" "T030 backend /api/health"
  frontend_base="${BASIC29_FRONTEND_BASE_URL:-http://localhost:3000}"
  smoke_url "${frontend_base}/admin/security/active-sessions" "T030 UI route /admin/security/active-sessions"
  smoke_url "${frontend_base}/admin/security/session-termination-histories" "T030 UI route /admin/security/session-termination-histories"
  smoke_url "${frontend_base}/admin/audit/business-process-logs" "T030 UI route /admin/audit/business-process-logs"
  smoke_url "${frontend_base}/admin/audit/sensitive-information-access-logs" "T030 UI route /admin/audit/sensitive-information-access-logs"
  smoke_url "${frontend_base}/admin/audit/permission-change-logs" "T030 UI route /admin/audit/permission-change-logs"
else
  echo "SKIP T030 docker runtime smoke; set RUN_BASIC29_DOCKER_SMOKE=1 to execute compose/health/UI route checks"
fi

echo "basic29 cross-cutting verification passed"
