# BASIC-69 Phase 1 선행조건 확인 보고서

## 범위와 판정

- 대상: T001, T002만 확인했다. 후속 Phase의 API, UI, migration, 테스트는 변경하지 않았다.
- status: BLOCKED
- blocker: OQ-001 — BASIC-60 관리자 API의 메뉴 권한 경로 binding이 인증 필터에 등록되어 있지 않다.

## T001 — 공통 SessionCookie·역할·메뉴·BASIC-60 binding

| 확인 항목 | 결과 | 저장소 증거 |
|---|---|---|
| 단일 애플리케이션 구조 | PASS | `backend/`, `frontend/`, `infra/docker-compose.yml`가 같은 PostgreSQL 16 Compose topology를 사용한다. |
| SessionCookie Principal | PASS | `AuthController.SESSION_COOKIE` (`COMMON_FOUNDATION_SESSION`), `AuthenticationFilter`, `AuthService.currentUser`가 Cookie → `CurrentUser`를 연결한다. |
| 기존 역할 R01~R09 | PASS | `V1__common_foundation_schema.sql`의 `roles` 계약 및 `user_roles` 참조를 재사용한다. 신규 역할코드는 추가하지 않는다. |
| BASIC-60 메뉴와 역할 권한 | PASS | `V56__basic60_operational_settings_and_grades.sql`가 menu 601~603 및 R04/R09 `menu_permissions`/`function_permissions`를 seed한다. frontend router와 `apiClient`에도 세 설정 route 및 상대 `/api` 호출이 있다. |
| 서버 API 역할 검사 | PASS | `Basic60Controller.requireSettingsAdmin`이 `CurrentUser`의 R04 또는 R09만 허용한다. |
| BASIC-60 API → 메뉴 URL binding | FAIL — OQ-001 | `AuthenticationFilter.pathToUiRoute`는 `/api/admin/evaluation-element-management-item-settings`, `/api/admin/participation-allocation-rate-settings`, `/api/admin/management-item-evaluation-score-settings` 및 각각의 `/save` 경로를 UI route로 변환하지 않는다. 따라서 `EffectivePermissionService.canAccess`는 menu URL과 일치하지 않는 API 경로로 권한을 조회한다. |

### OQ-001 선행조건 실패

후속 단계에서 세 BASIC-60 설정 API를 정상적으로 재사용하려면 API path를 각각의 기존 UI menu URL로 매핑하는 공통 인증 binding이 필요하다. 현재 상태에서는 R04 사용자가 유효한 메뉴 권한을 갖더라도 filter의 menu guard에서 403을 받을 수 있다. R09는 `EffectivePermissionService`의 관리자 우회 때문에 영향을 받지 않는다.

이 phase는 확인과 보고만 허용하므로 공통 인증 filter나 권한 정책을 수정하지 않았다. OQ-001이 해소되기 전에는 BASIC-69의 후속 설정 API/UI 작업을 진행하지 않는다.

## T002 — Flyway·규정버전·설정 table 확인

| 확인 항목 | 결과 | 저장소 증거 |
|---|---|---|
| Flyway 증분 위치 | PASS | `application.yml`은 `classpath:db/migration`을 사용하며 기존 migration은 immutable 순번 파일이다. |
| 평가규정 버전 | PASS | `V27__basic33_evaluation_rule_foundation.sql`의 `evaluation_rule_versions`가 적용기간과 `DRAFT`/`CONFIRMED`/`DISCARDED` 상태를 정의한다. |
| 평가요소별 관리항목 설정 | PASS | `V56__basic60_operational_settings_and_grades.sql`의 `evaluation_element_management_item_settings` 및 `Basic60Mapper.xml`이 존재한다. |
| 참여구분별 배분율 설정 | PASS | 같은 V56 migration의 `participation_allocation_rate_settings` 및 mapper가 존재한다. |
| 관리항목별 평가점수 설정 | PASS | 같은 V56 migration의 `management_item_evaluation_score_settings` 및 mapper가 존재한다. |
| 증분 migration 위치 확정 | PASS | 세 설정 table과 seed가 이미 V56으로 materialize되어 있으므로 이번 확인 phase에는 migration을 추가하지 않는다. 이후 변경이 실제로 필요해질 경우 현재 마지막 번호 V57 다음의 새 V58+ migration만 추가하며 기존 V27/V56은 수정하지 않는다. |

## 보존 사항

- 기존 V27/V56 Flyway migration, BASIC-60 production code, frontend route/API client, Compose topology를 변경하지 않았다.
- 사전에 존재하던 테스트 fixture 변경도 수정하지 않았고, observe-mode 지시에 따라 테스트를 실행하지 않았다.

## Checkpoint

- T002: READY
- T001: OQ-001 해소 전 BLOCKED
- Phase 1 전체: BLOCKED — OQ-001 공통 메뉴 권한 binding 선행조건 실패가 visible 상태로 보고되었다.
