# BASIC-69 Phase 1 선행 확인 보고서

## 판정

- status: READY
- missing_contracts: 없음
- OQ-001 선행조건 실패: 없음

## 보존할 기존 동작

- 단일 `backend`, `frontend`, `infra/docker-compose.yml`, PostgreSQL 실행 구성을 유지한다.
- SessionCookie 인증 Principal과 `AuthenticationFilter`의 메뉴 URL 권한 검증을 재사용한다.
- 기존 R01~R09 seed와 메뉴·권한·데이터 범위 정책을 변경하거나 별도 공통 체계를 만들지 않는다.
- 기존 Flyway migration은 불변으로 유지하고, 후속 데이터 변경은 새 증분 migration으로만 적용한다.

## T001 공통 계약 확인

| 확인 항목 | 결과 | 재사용 증거 |
|---|---|---|
| SessionCookie 인증 | PASS | `auth/AuthController`, `AuthenticationFilter`, `CurrentUser` |
| R01~R09 역할 | PASS | `V2__common_foundation_seed.sql`의 역할 seed |
| 메뉴 기반 접근 통제 | PASS | `EffectivePermissionService`, `PermissionMapper`, `menus`, `menu_permissions` |
| BASIC-60 세 화면 메뉴 binding | PASS | `V56__basic60_operational_settings_and_grades.sql`의 메뉴 601~603 및 실행정보 |
| BASIC-60 API → 메뉴 URL 권한 검증 | PASS | `AuthenticationFilter`가 세 설정 API와 save API를 해당 `/admin/...` 메뉴 URL로 변환 |
| React shell 및 상대 API 호출 | PASS | `app/router.tsx`, `api/apiClient.ts`의 BASIC-60 화면·`/api/...` 호출 |
| 공통 감사·Excel·배치 기능 | PASS | 기존 `audit`, `excel`, `batch` 모듈 |

## T002 데이터·Flyway 확인

| 확인 항목 | 결과 | 증거 |
|---|---|---|
| 규정 버전 상태 | PASS | V27의 `evaluation_rule_versions`와 V56의 DRAFT/CONFIRMED seed |
| 평가요소별 관리항목 설정 | PASS | V56의 `evaluation_element_management_item_settings` |
| 참여구분별 배분율 설정 | PASS | V56의 `participation_allocation_rate_settings` |
| 관리항목별 평가점수 설정 | PASS | V56의 `management_item_evaluation_score_settings` |
| 기존 migration 불변성 | PASS | 최신 기존 migration V57을 포함해 수정하지 않음 |
| 다음 증분 migration 위치 | 확정 | `backend/src/main/resources/db/migration/V58__basic69_<purpose>.sql` |

V58은 후속 Phase에서 실제 필요한 변경이 있을 때만 생성한다. 현재 Phase 1은 기존 BASIC-60 계약의 재사용 가능 여부 확인만 수행하므로 새 schema, seed, 메뉴, API 또는 화면을 추가하지 않는다.
