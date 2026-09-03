# BASIC-45 Phase 1 Foundation 선행조건 확인 보고서

## 판정

- status: READY
- missing_contracts: 없음
- contract_violations: 없음

## 확인한 기존 공통 계약

| 영역 | 확인 결과 | repository 증거 |
|---|---|---|
| 단일 저장소 구조 | 충족 | backend/, frontend/, infra/docker-compose.yml 존재 |
| 세션 인증 | 충족 | AuthController.SESSION_COOKIE=`COMMON_FOUNDATION_SESSION`, AuthenticationFilter, CurrentUser/AuthenticatedSession 재사용 가능 |
| R01~R09 역할 | 충족 | 기존 roles/user_roles/menu_permissions 기반 role code와 EffectivePermissionService 재사용 가능 |
| 배치 서비스 | 충족 | backend/src/main/java/kr/ac/knue/commonfoundation/batch 의 BatchDefinition/Execution/Result/Retry 서비스·컨트롤러 존재 |
| 감사·추적 | 충족 | business_process_audit_logs, request_id 컬럼, 공통 ApiResponse meta traceId/requestId 지원 재사용 가능 |
| React shell route guard | 충족 | frontend/src/app/AuthProvider.tsx, frontend/src/app/router.tsx, AdminShell 메뉴 기반 화면 guard 재사용 가능 |
| PostgreSQL/Flyway | 충족 | backend/src/main/resources/db/migration 의 기존 증분 migration 위에 V46 추가 가능 |

## 보존할 기존 동작

- 새 프로젝트, 별도 인증체계, 별도 사용자·조직·권한 테이블, 두 번째 Docker Compose를 만들지 않는다.
- 기존 SessionCookie, CurrentUser Principal, R01~R09 role code, EffectivePermissionService 권한 guard를 보존한다.
- 기존 batch_* 공통 운영 테이블과 batch 패키지는 변경하지 않고 BASIC-45 업무 테이블에서 batchId/requestId를 연결한다.
- 기존 공통 ApiResponse/ApiError envelope, validation error shape, frontend /api 상대경로 규칙을 보존한다.
- 기존 frontend 화면, CSS token, routing 동작은 이번 Phase 1에서 변경하지 않는다.

## 이번 Phase 1 변경 범위

- BASIC-45 평가자료·일괄처리 요청·결과·점수계산 세대·최종평가 확정 foundation schema를 증분 Flyway migration으로 추가한다.
- B45-SEED-001~005 검증 fixture와 BASIC-45 메뉴/권한 seed를 추가한다.
- 신규 업무 API prefix를 기존 AuthenticationFilter의 UI route permission mapping에 연결한다.
- 신규 업무 API가 사용할 공통 envelope/requestId/batchId naming foundation contract test와 helper를 추가한다.

## 선행조건 실패 보고 여부

누락 계약이 없어서 선행조건 실패로 중단하지 않았다. 본 문서를 T001 READY evidence로 남긴다.
