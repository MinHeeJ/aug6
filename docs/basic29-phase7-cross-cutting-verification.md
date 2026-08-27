# BASIC-29 Phase 7 Cross-cutting verification

## Scope

- T029: 신규/변경 operation의 happy/auth/validation/business/side-effect assertion 분리 여부 검증
- T030: Docker Compose runtime, `/api/health`, 신규 5개 UI route smoke 검증

## Preserved behavior

- 기존 API envelope, R09 authorization, `ApiError` error model, request id propagation, dynamic SQL predicate rule, frontend relative `/api` 호출 규칙은 유지한다.
- 기존 Compose file은 수정하지 않았다. 기본 8080/3000 published port 계약을 보존한다.

## Verification evidence

### T029 static executable assertion audit

Command:

```bash
tests/smoke/basic29-cross-cutting.sh
```

Result:

- active session list happy assertion present
- terminate happy/request-id, auth, validation, business conflict, persistence side-effect assertions present as separate executable tests
- session termination history happy, auth, validation, immutability/filter assertions present
- business process log happy, auth, validation, immutable/filter assertions present
- sensitive information access log happy/no-plaintext, auth, validation, immutable/filter assertions present
- permission change log happy, auth, validation, immutability assertions present
- optional MyBatis filters do not use null-bound predicates
- frontend API calls remain relative

### T030 Docker Compose and route smoke

Command attempted first:

```bash
RUN_BASIC29_DOCKER_SMOKE=1 tests/smoke/basic29-cross-cutting.sh
```

Observed blocker in this shared runner:

- The repository Compose build succeeded after fixing test-compile errors.
- The default host ports 8080/3000 were already allocated by an existing unrelated `basic14-final` Compose stack.

To preserve the repository Compose file and avoid stopping unrelated containers, runtime smoke was executed with a temporary Compose override that only changed host published ports:

```bash
docker compose -f infra/docker-compose.yml -f /tmp/basic29-compose-port-override.yml up -d --wait
```

Compose result:

- `infra-database-1`: healthy
- `infra-backend-1`: healthy
- `infra-frontend-1`: healthy

The WSL host could not connect to the published override ports even though Docker reported the mappings, so the same containerized services were checked from inside the running Compose containers:

```bash
docker exec infra-backend-1 wget -qO- http://localhost:8080/api/health
for path in \
  /admin/security/active-sessions \
  /admin/security/session-termination-histories \
  /admin/audit/business-process-logs \
  /admin/audit/sensitive-information-access-logs \
  /admin/audit/permission-change-logs; do
  docker exec infra-frontend-1 wget -qO- "http://localhost$path" >/tmp/basic29-ui-smoke.html
  echo "UI $path OK"
done
```

Result:

- `/api/health` returned `{"success":true,"data":{"status":"UP","service":"common-foundation"},...}`
- `/admin/security/active-sessions` OK
- `/admin/security/session-termination-histories` OK
- `/admin/audit/business-process-logs` OK
- `/admin/audit/sensitive-information-access-logs` OK
- `/admin/audit/permission-change-logs` OK

## Build issues repaired during verification

Docker build runs `mvn -q -DskipTests package`, which still test-compiles Java test sources. Verification exposed and repaired existing test-source compile blockers:

- Escaped JSON string literals in `BusinessProcessLogApiTest`.
- Escaped quoted camelCase alias assertions in `BusinessProcessLogMapperContractTest`.
- Added missing `BusinessValidationException` import and corrected the `listSessionTerminationHistories` Mockito signature in `ActiveSessionManagementApiTest`.

## Added coverage artifact

- Added the missing BASIC-29 sensitive-information Playwright smoke spec so all five new UI routes have durable route-flow specs.
- Added `tests/smoke/basic29-cross-cutting.sh` as an executable Phase 7 verification script.
