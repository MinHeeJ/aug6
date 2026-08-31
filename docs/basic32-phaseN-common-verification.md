# BASIC-32 Phase N Common Verification

## Scope

- T999: 신규 BASIC-32 목록 화면 전체에 기본 20건 및 20/50/100 표시 건수 UI/API 검증을 적용한다.
- T1000: 접근성, 성능, 보안, 브라우저 smoke 검증 경로와 실행 결과를 기록한다.

## Preserved behavior

- 기존 backend, frontend, infra/docker-compose.yml 단일 구성과 PostgreSQL 서비스 연결을 유지했다.
- 기존 API envelope, R09 권한 검사, ApiError 오류 모델, 상대 `/api` 호출 규칙, 동적 SQL predicate 규칙을 보존했다.
- 기존 BASIC-32 기능 구현과 기존 테스트를 제거하거나 약화하지 않고 공통 검증 테스트와 smoke만 추가했다.

## Changed behavior / added artifacts

- 모든 신규 BASIC-32 목록 API의 기본 size=20 및 허용 표시 건수 20/50/100 계약을 하나의 MockMvc 공통 테스트로 보강했다.
- 모든 신규 BASIC-32 목록 화면의 page size select 기본값과 20/50/100 선택 동작을 하나의 Vitest 공통 테스트로 보강했다.
- 평가조직 매핑 화면 테스트에 20건/50건/100건 옵션 검증을 추가했다.
- 전체 신규 route와 대표 viewport/browser smoke를 재사용 가능한 Playwright spec으로 추가했다.
- 정적/선택적 runtime 검증 스크립트 `tests/smoke/basic32-common-verification.sh`를 추가했다.

## Verification evidence

### T999/T1000 static smoke

Command:

```bash
tests/smoke/basic32-common-verification.sh
```

Result:

- PASS: BASIC-32 6개 화면에 `data-screen-id`, page-size select `data-testid`, label/aria 기반 접근성 표식, 20/50/100 옵션이 존재한다.
- PASS: BASIC-32 6개 화면 테스트가 20건/50건/100건 표시 건수를 검증한다.
- PASS: BASIC-32 6개 API 테스트와 `Basic32CommonVerificationTest`가 default 20 및 20/50/100 size 계약을 검증한다.
- PASS: Playwright browser smoke spec이 신규 6개 route, default 20, 20/50/100 선택, API response leakage, tablet/desktop viewport smoke를 포함한다.
- PASS: MyBatis mapper에 null-bound optional filter 패턴이 없다.
- PASS: frontend source에 `http://localhost:8080` 또는 `127.0.0.1:8080` 절대 API 호출이 없다.
- PASS: BASIC-32 backend source에 password/secret/token 민감 literal이 없다.
- SKIP: frontend/dist 크기 검사는 build 산출물이 없어 실행하지 않았다.
- SKIP: Docker/browser runtime smoke는 이 codegen phase에서 dev server/container를 시작하지 않는 실행 제약 때문에 기본 실행에서 제외했다. 필요 시 `RUN_BASIC32_DOCKER_SMOKE=1 tests/smoke/basic32-common-verification.sh`로 수행한다.

### Docker compose config validation

Command:

```bash
docker compose -f infra/docker-compose.yml config >/tmp/basic32-compose-config.yml
```

Result:

- PASS: `docker compose config OK`

### Frontend Vitest execution attempt

Command:

```bash
cd frontend && npm run test -- --run src/pages/admin/BASIC32-common-verification.test.tsx
```

Observed blocker:

- `vitest: not found`
- `node_modules` is absent in this runner and this codegen phase must not run package managers or materialize dependencies. The durable Vitest test file was still added for the later verification/repair phase after `npm ci`.

### Backend Maven tests

- Maven test execution was intentionally not run because the Spring Boot/Maven backend test authoring contract for this codegen phase says not to run `mvn test` or other long-running Maven test commands during codegen.
- The new MockMvc test source was added under `backend/src/test/java` for the later verification phase.
