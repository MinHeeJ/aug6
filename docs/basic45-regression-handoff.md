# BASIC-45 Phase 7 Regression and Handoff

## 변경 보존 범위

- 보존: 기존 공통 인증, SessionCookie, R01~R09 역할, 메뉴/기능 권한, batch/audit 기반, 기존 API envelope와 `/api/health` 계약.
- 보존: 이전 phase에서 추가된 평가자료 생성, 평가자료 삭제, 점수 재계산, 최종평가 확정·취소, 처리 결과 조회 API와 화면 경로.
- 변경: Phase 7에 한정하여 통합 E2E 회귀 시나리오, OpenAPI 요구 회계 guard, handoff smoke script를 추가했다.

## T060 / REQ-1459 — 전체 흐름 E2E

Durable Playwright spec:

- `tests/e2e/basic45-evaluation-batch-regression.spec.ts`

검증 범위:

1. BASIC-45 다섯 화면 route render 및 권한 오류 부재.
2. 평가자료 생성 후보 조회 후 생성 batchId 반환.
3. 생성 batchId 기준 삭제 미리보기와 논리삭제.
4. 동일 원천 재생성 후 점수 재계산 preview의 beforeScore/afterScore 확인.
5. 재계산 실행 후 최종평가 확정.
6. 확정취소 후 재계산 및 재확정.
7. 생성, 삭제, 재생성, 재계산, 확정, 취소, 재확정 batchId가 통합 결과 조회 API에서 재조회되는지 확인.
8. batch error detail endpoint가 batchId와 오류 배열 envelope를 반환하는지 확인.

실행 명령:

```bash
cd frontend
npx playwright test ../tests/e2e/basic45-evaluation-batch-regression.spec.ts
```

## T061 / REQ-1391 — 공통 회귀, build, compose, health

Durable smoke script:

- `tests/smoke/basic45-regression-handoff.sh`

기본 실행 범위:

- `.gitignore` 필수 제외 패턴 확인.
- `backend/.dockerignore`, `frontend/.dockerignore` 필수 제외 패턴 확인.
- OpenAPI/Requirement Accounting guard 실행.
- Docker Compose V2가 있으면 `docker compose -f infra/docker-compose.yml config` 검증.

런타임 compose/health까지 실행하려면:

```bash
BASIC45_RUN_DOCKER_UP=1 bash tests/smoke/basic45-regression-handoff.sh
```

이 모드는 다음을 추가 수행한다.

- `docker compose -f infra/docker-compose.yml up -d --wait`
- `curl -fsS http://127.0.0.1:8080/api/health`

현재 코드 생성 환경에서는 Maven CLI가 없어 backend Maven build를 직접 실행하지 못할 수 있다. Maven 사용 가능 환경의 권장 검증 명령은 다음과 같다.

```bash
cd backend
mvn test
mvn -DskipTests package
```

Frontend 권장 검증 명령:

```bash
cd frontend
npm run typecheck
npm run test -- --run
npm run build
```

## T062 / REQ-1388 — OpenAPI 및 요구 회계

Durable guard:

- `tests/smoke/basic45-requirement-accounting.py`

검증 범위:

- `backend/src/main/resources/contracts/openapi.yaml`
- `backend/src/test/resources/contracts/openapi.yaml`

확인 사항:

- `x-uncovered-requirements`가 남아 있지 않다.
- BASIC-45 신규 operationId가 OpenAPI fixture에 모두 존재한다.
- 각 operation의 핵심 `x-related-requirements`가 operation block 안에 존재한다.
- 이 handoff 문서가 T060, T061, T062와 REQ-1459, REQ-1391, REQ-1388을 명시한다.

실행 명령:

```bash
python3 tests/smoke/basic45-requirement-accounting.py
```

## 산출물 목록

- `tests/e2e/basic45-evaluation-batch-regression.spec.ts`
- `tests/smoke/basic45-requirement-accounting.py`
- `tests/smoke/basic45-regression-handoff.sh`
- `docs/basic45-regression-handoff.md`
- `backend/src/main/resources/contracts/openapi.yaml`
- `backend/src/test/resources/contracts/openapi.yaml`
- `backend/src/main/java/kr/ac/knue/commonfoundation/basic45/ScoreRecalculationService.java` (이전 phase 산출물의 Java string escape compile 오류 보정)
