# BASIC-40 Phase 1 Shared Prerequisites Report

## Scope

- T001 [REQ-1166]: 기존 backend/frontend/infra/docker-compose.yml와 PostgreSQL 연결이 canonical architecture와 일치하는지 확인한다.
- T002 [REQ-1167]: CMN-FR-001~025, CMN-FR-052~059, CMN-FR-071~082 공통 API/서비스/화면 재사용 가능성을 점검하고 누락 시 선행조건 실패로 보고한다.
- T003 [REQ-1170]: 기존 Flyway migration 수정 없이 신규 migration 파일명과 rollback 계획을 예약한다.
- T004 [REQ-1180]: 기존 정본 설계문서와 design-doc-change-set patch를 적용한 effective contract를 구현 입력으로 고정한다.

## Requested behavior to change

- 이번 phase는 구현 준비 phase로, 신규 업무 API/UI/schema를 아직 추가하지 않는다.
- BASIC-40 후속 phase가 사용할 effective OpenAPI contract를 durable test fixture로 고정한다.
- BASIC-40 신규 Flyway migration 번호와 rollback 운영 방침을 예약한다.
- 기존 공통기능 재사용 가능성 및 선행조건 상태를 repository evidence로 남긴다.

## Existing behavior to preserve

- 기존 `backend`, `frontend`, `infra/docker-compose.yml`, PostgreSQL 16/Flyway 기반 단일 app topology를 유지한다.
- 기존 SessionCookie 인증, `AuthenticationPort`, `AuthenticationFilter`, `EffectivePermissionService`, menu permission guard를 재사용한다.
- 기존 R01~R09 role seed와 role name을 변경하지 않는다.
- 기존 common API/service/screen, Excel, batch, audit, notice/manual attachment download 구현을 대체하거나 중복 구현하지 않는다.
- 기존 Flyway migration 파일은 수정하지 않는다.
- 후속 phase 전까지 `appeal_period_settings`, `result_view_period_settings`, `exception_period_settings` table/API/UI는 생성하지 않는다.

## T001 Architecture/runtime precondition result

| Check | Result | Evidence |
|---|---|---|
| backend directory | PASS | `backend/pom.xml` uses Spring Boot `3.3.5`, Java `17`, MyBatis, Flyway, PostgreSQL dependency, and Spring Boot repackage goal |
| frontend directory | PASS | `frontend/package.json` uses React `18.3.1`, Vite `5.4.11`, TypeScript, npm scripts |
| compose file | PASS | `infra/docker-compose.yml` has `database`, `backend`, `frontend` services under top-level `services:` |
| PostgreSQL connection | PASS | backend compose datasource URL is `jdbc:postgresql://database:5432/${POSTGRES_DB:-common_foundation}` and database image is `postgres:16.4-alpine` |
| branch preview ports | PASS | app services publish `8080:8080` and `3000:80`; database host port is not published |
| health endpoint | PASS | backend healthcheck calls `http://localhost:8080/api/health`; required health endpoint remains `/api/health` |
| build context safety | PASS | compose build contexts are static `../backend` and `../frontend`; both contexts have `.dockerignore` |

## T002 Common-feature reuse result

| Common area | Result | Reuse target |
|---|---|---|
| 사용자/조직/인증/권한/메뉴/코드 | PASS | `auth`, `users`, `organizations`, `roles`, `userroles`, `menus`, `permissions`, `codes` backend packages and corresponding React admin screens |
| SessionCookie and server guard | PASS | `AuthenticationFilter`, `AuthController`, `EffectivePermissionService`, `PermissionMapper` |
| Role seed R01~R09 | PASS | existing common seed migrations; no new role code reservation for BASIC-40 |
| PostgreSQL/Flyway/MyBatis | PASS | `backend/src/main/resources/db/migration` with existing V1-V40 migrations and mapper-based modules |
| Excel/batch/audit operations | PASS | existing `excel`, `batch`, `audit`, `securitysessions`, `permissionops` modules |
| Attachment-like file download support | PASS | existing `notices` and `manuals` modules preserve original file name and tokenized download flows |
| Missing reusable contract | NONE | No 선행조건 실패 raised for T002 |

## T003 Migration reservation and rollback plan

- Reserved migration filename: `backend/src/main/resources/db/migration/V41__basic40_remaining_period_controls.sql`
- Reserved content owner: 후속 BASIC-40 data/API phase.
- Scope for V41: only BASIC-40 tables/seeds required by `appeal_period_settings`, `result_view_period_settings`, `exception_period_settings` and associated menu/permission fixture rows.
- Existing migrations V1-V40 must remain immutable.
- Rollback plan: Flyway undo is not assumed. If rollback is required after V41 is applied, add a later forward migration that deactivates BASIC-40 seed/menu rows and drops only BASIC-40 tables after confirming no production data dependency; otherwise restore from database backup before applying V41.

## T004 Effective contract lock

- Durable OpenAPI fixture updated: `backend/src/test/resources/contracts/openapi.yaml`
- Fixture sha256: `89246657df499030298d18272773b9060169e67a871da816c7020833bd40227d`
- Contract lock artifacts:
  - `docs/requirements/basic40/effective-contract-lock.json`
  - `backend/src/test/resources/requirements/basic40/effective-contract-lock.json`
- Locked operationIds for later phases:
  - `listAppealPeriods`, `saveAppealPeriod`
  - `listResultViewPeriods`, `saveResultViewPeriod`
  - `listExceptionPeriods`, `saveExceptionPeriod`

## Verification evidence

### Static Phase 1 smoke

Command:

```bash
tests/smoke/basic40-phase1-shared-prerequisites.sh
```

Result:

- PASS: required repository structure exists
- PASS: compose topology matches canonical runtime
- PASS: common backend/frontend reuse targets exist
- PASS: role seeds and migration reservation boundary are valid
- PASS: BASIC-40 effective OpenAPI operations are locked in durable fixture
- PASS: effective contract lock and migration reservation exist
- PASS: frontend API source keeps relative path contract
- PASS: BASIC-40 Phase 1 shared prerequisites READY

### Docker Compose config validation

Command:

```bash
docker compose -f infra/docker-compose.yml config >/tmp/basic40-compose-config.yml && echo 'docker compose config OK'
```

Result:

- PASS: docker compose config OK

### Deferred commands

- `mvn test` was intentionally not run because this codegen phase explicitly prohibits Maven test execution.
- npm/package-manager commands were intentionally not run because this codegen phase prohibits package manager execution and dependency materialization.
- Dev servers, watchers, and runtime containers were not started.

## Checkpoint

- status: READY
- missing_contracts: 없음
- contract_violations: 없음
- later phase work intentionally not implemented: BASIC-40 Phase 2~5 API/schema/UI/tests.
