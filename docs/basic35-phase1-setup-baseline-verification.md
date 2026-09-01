# BASIC-35 Phase 1 Setup / Baseline Verification

## Scope

- T001: 기존 R01~R09 role seed와 SessionCookie/menu guard 확인. canonical_id: REQ-1045
- T002: 기존 R01~R09 role seed와 SessionCookie/menu guard 확인. canonical_id: REQ-1046
- T003: 기존 R01~R09 role seed와 SessionCookie/menu guard 확인. canonical_id: REQ-1047
- T004: 기존 R01~R09 role seed와 SessionCookie/menu guard 확인. canonical_id: REQ-1048
- T005: 기존 공통 회귀 테스트와 `/api/health` baseline 확인. canonical_id: REQ-1071

## Preserved behavior

- 기존 `backend`, `frontend`, `infra/docker-compose.yml`, PostgreSQL 기반 단일 구성을 변경하지 않았다.
- 기존 R01~R09 역할 seed를 새로 정의하거나 역할명을 변경하지 않았다.
- 기존 `COMMON_FOUNDATION_SESSION` SessionCookie, backend `AuthenticationFilter`, frontend menu-url route guard를 유지했다.
- 기존 `/api/health` endpoint, frontend 상대경로 `/api/health` client, compose healthcheck를 유지했다.
- 이후 단계에서 구현할 업무기간 API/UI, schema, migration은 이번 Phase 1 범위 밖으로 두었다.

## Changed behavior / added artifacts

- `BusinessPeriodBaselineVerificationTest`를 추가해 R01~R09 seed, R09 menu permission seed, SessionCookie/menu guard, `/api/health` envelope를 회귀 테스트로 고정했다.
- `LoginPage.test.tsx`에 role code만으로 접근을 허용하지 않고 current user menu URL을 기준으로 route guard가 동작함을 검증하는 회귀 테스트를 추가했다.
- `tests/smoke/basic35-phase1-baseline.sh`를 추가해 Phase 1 baseline을 빠르게 정적 검증하고, 선택적으로 compose config/runtime health smoke를 실행할 수 있게 했다.

## Verification evidence

### Phase 1 static baseline smoke

Command:

```bash
tests/smoke/basic35-phase1-baseline.sh
```

Result:

- PASS: R01~R09 seed가 기존 `V2__common_foundation_seed.sql`에 존재한다.
- PASS: admin 사용자는 기존 R09 role seed를 재사용한다.
- PASS: R09 menu permission seed가 기존 role target으로 존재한다.
- PASS: 업무기간 전용 신규 role code를 migration에 추가하지 않았다.
- PASS: `COMMON_FOUNDATION_SESSION` Cookie 이름, HttpOnly, SameSite=Lax 설정이 유지된다.
- PASS: backend filter는 SessionCookie를 추출하고 admin/business API에 대해 기존 `EffectivePermissionService` menu guard를 사용한다.
- PASS: frontend route guard는 role code 단독이 아니라 current user menu URL을 사용한다.
- PASS: `/api/health` endpoint와 frontend 상대경로 health client, compose healthcheck가 유지된다.
- PASS: frontend source에 `http://localhost:8080` 또는 `127.0.0.1:8080` 절대 API 호출이 없다.
- SKIP: runtime `/api/health` smoke는 이 codegen phase에서 서버/container를 시작하지 않는 실행 제약 때문에 기본 실행에서 제외했다. 필요 시 `RUN_BASIC35_RUNTIME_SMOKE=1 tests/smoke/basic35-phase1-baseline.sh`로 수행한다.

### Docker compose config validation

Command:

```bash
docker compose -f infra/docker-compose.yml config >/tmp/basic35-compose-config.yml && echo 'docker compose config OK'
```

Result:

- PASS: `docker compose config OK`

### Backend Maven tests

- Maven test execution was intentionally not run because the Spring Boot/Maven backend test authoring contract for this codegen phase says not to run `mvn test` or other long-running Maven test commands during codegen.
- The new JUnit 5 baseline verification test source was added under `backend/src/test/java` for the later verification/repair phase.

### Frontend Vitest tests

- Frontend package-manager commands were intentionally not run in this codegen phase because the instructions prohibit package manager execution and dependency materialization.
- The existing `LoginPage.test.tsx` regression suite was extended for the later verification/repair phase after dependencies are available.
