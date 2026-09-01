# BASIC-36 Phase 1 선행조건 확인 보고서

## 판정

- status: READY
- missing_contracts: 없음
- contract_violations: 없음

## 확인한 기존 공통 계약

| 영역 | 확인 결과 | repository 증거 |
|---|---|---|
| 단일 저장소 구조 | 충족 | backend/, frontend/, infra/docker-compose.yml 존재 |
| 인증 | 충족 | backend auth 패키지의 세션 쿠키 기반 AuthenticationFilter/CurrentUser 재사용 가능 |
| 권한 | 충족 | EffectivePermissionService와 PermissionMapper 기반 메뉴 URL 권한 guard 재사용 가능 |
| 메뉴 | 충족 | backend menus 패키지와 frontend AdminShell/router route registry 존재 |
| 코드 | 충족 | backend codes 패키지와 기존 코드 관리 화면/테스트 존재 |
| 감사 | 충족 | securitysessions/audit 관련 운영 로그 모듈과 변경 로그 화면/테스트 존재 |
| 배치 | 충족 | backend batch 패키지와 배치 정의/실행/결과/재처리 화면/테스트 존재 |
| PostgreSQL/Flyway | 충족 | backend/src/main/resources/db/migration 기존 증분 migration 존재 |
| React shell 및 /api 상대경로 | 충족 | frontend/src/app/router.tsx, frontend/src/api/apiClient.ts의 /api 상대경로 요청 |
| Docker Compose runtime | 충족 | infra/docker-compose.yml의 backend/frontend/database 서비스와 기존 /api reverse proxy 구조 유지 |

## 보존할 기존 동작

- 새 프로젝트, 별도 인증체계, 별도 사용자·조직·권한 테이블, 두 번째 Docker Compose를 만들지 않는다.
- 기존 Flyway migration은 수정하지 않고 BASIC-36 증분 migration만 추가한다.
- 기존 세션 Principal, R01~R09 role code, 메뉴 URL 권한 guard, 공통 코드·감사·배치 모듈을 재사용한다.
- 기존 frontend shell, layout, CSS token, apiClient의 상대 /api 호출 규칙을 유지한다.
- 기존 infra/docker-compose.yml과 frontend/nginx /api reverse proxy는 수정하지 않는다.

## Phase 1 변경 범위

- BASIC-36 foundation migration skeleton 추가.
- React shell route registry에 BASIC-36 신규 업무 route placeholder 추가.
- 이후 phase의 API 권한 연결을 위해 BASIC-36 API prefix → UI route mapping contract 추가.

## 선행조건 실패 보고 여부

누락 계약이 없어서 별도 선행조건 실패 보고서는 생성하지 않았다. 본 문서를 READY evidence로 남긴다.
