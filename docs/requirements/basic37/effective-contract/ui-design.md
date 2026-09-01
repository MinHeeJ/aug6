# UI Design: 프로젝트 생성

## Design Reference

- reference_name: ex
- reference_url: https://tailwindadmin-reactjs-minisidebar.netlify.app/
- 적용 범위: 레퍼런스는 복제 대상이 아니라 UI/UX 방향성 계약이다. 기능 도메인, route, screen_id, API path, operationId는 승인된 `spec.md`의 Canonical Naming Ledger와 `contracts/openapi.yaml`을 따른다.
- 적용 원칙: 미니멀 icon sidebar, header profile area, light/dark theme, compact table/form density, responsive content layout, dashboard/card spacing, color token 사용 방식을 시스템 관리 도메인에 맞게 적용한다.
- 금지 원칙: 레퍼런스 사이트의 업무 도메인, mock data, route, menu label을 가져오지 않는다. 요구 source_id token으로 화면ID, route, operationId, menu_path를 만들지 않는다.

## Route Inventory

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story |
|---|---|---|---|---|---|---|
| /login | SCR-LOGIN | anonymous, R01~R09 after session | 인증 | login / getCurrentUser | sessions | US-01 |
| /admin/users | SCR-USER-MGMT | R09 | 시스템 관리 > 사용자·조직 관리 > 사용자 관리 | searchUsers | users | US-02 |
| /admin/organizations | SCR-ORG-MGMT | R09 | 시스템 관리 > 사용자·조직 관리 > 조직 관리 | searchOrganizations / getOrganizationTree / saveOrganizationParentRelation | organizations | US-03 |
| /admin/roles | SCR-ROLE-MGMT | R09 | 시스템 관리 > 역할·권한 관리 > 역할 관리 | listRoles / updateRole | roles | US-04 |
| /admin/user-roles | SCR-USER-ROLE-MGMT | R09 | 시스템 관리 > 역할·권한 관리 > 사용자 역할 관리 | listUserRoleAssignments / assignUserRole / updateUserRole / revokeUserRole | user_roles | US-05 |
| /admin/menu-permissions | SCR-MENU-PERMISSION-MGMT | R09 | 시스템 관리 > 역할·권한 관리 > 메뉴 권한 관리 | listMenuPermissions / saveMenuPermissions | menu_permissions | US-06 |
| /admin/menu-structure | SCR-MENU-STRUCTURE-MGMT | R09 | 시스템 관리 > 메뉴 관리 > 메뉴 구조 관리 | getMenuTree / updateMenuParent / reorderMenus | menus | US-07 |
| /admin/menu-info | SCR-MENU-INFO-MGMT | R09 | 시스템 관리 > 메뉴 관리 > 메뉴 정보 관리 | getMenuExecution / updateMenuExecution | menu_execution_info | US-08 |
| /admin/code-groups | SCR-CODE-GROUP-MGMT | R09 | 시스템 관리 > 공통코드 관리 > 코드그룹 관리 | listCodeGroups / createCodeGroup / updateCodeGroup | code_groups | US-09 |
| /admin/detail-codes | SCR-DETAIL-CODE-MGMT | R09 | 시스템 관리 > 공통코드 관리 > 상세코드 관리 | listDetailCodes / createDetailCode / updateDetailCode | detail_codes | US-10 |
| /admin/evaluation-scores | SCR-EVAL-SCORE-MGMT | R04, R08, R09 | 평가 기준 관리 > 평가 기준정보 관리 > 평가점수 관리 | listEvaluationScores / saveEvaluationScore | evaluation_score_rules | US-01 |
| /admin/participation-rates | SCR-PARTICIPATION-RATE-MGMT | R04, R08, R09 | 평가 기준 관리 > 평가 기준정보 관리 > 참여구분·배분율 관리 | listParticipationRates / saveParticipationRate | participation_rate_rules | US-02 |
| /admin/calculation-formulas | SCR-CALC-FORMULA-MGMT | R04, R08, R09 | 평가 기준 관리 > 평가 기준정보 관리 > 계산식 관리 | listCalculationFormulas / saveCalculationFormula | calculation_formula_versions | US-03 |
| /admin/evaluation-rule-sets | SCR-EVAL-RULE-SET-MGMT | R04, R08, R09 | 평가 기준 관리 > 평가 기준정보 관리 > 업적평가 기준·점수규칙 관리 | listEvaluationRuleSets / saveEvaluationRuleSet | evaluation_rule_sets | US-04 |
| /admin/journal-indexing-infos | SCR-JOURNAL-INDEXING-MGMT | R04, R08, R09 | 평가 기준 관리 > 평가 기준정보 관리 > 학술지·후보지 등재정보 관리 | listJournalIndexingInfos / saveJournalIndexingInfo | journal_indexing_infos | US-05 |

## Navigation Map

- `/login`은 shell 밖의 인증 진입 route이며, 성공 후 R09 시스템관리자에게 시스템 관리 menu shell을 표시한다.
- 인증 후 protected shell은 `시스템 관리` top menu 아래에 `사용자·조직 관리`, `역할·권한 관리`, `메뉴 관리`, `공통코드 관리` middle group을 제공한다.
- R09가 아닌 사용자 또는 권한 없는 인증 사용자는 menu_permissions의 effective result에 따라 leaf menu가 숨겨져야 하며, 직접 route/API 접근은 `contracts/openapi.yaml`의 401/403 응답 계약을 따른다.
- `SCR-CODE-GROUP-MGMT`에서 상세코드 이동은 같은 공통코드 관리 group 안의 `SCR-DETAIL-CODE-MGMT` route로 이어진다. 구체 row action은 후속 feature batch가 작성한다.
- 범위 밖 업무 메뉴는 이 skeleton의 Route Inventory와 Screen Skeleton Ledger에 포함하지 않는다.

## Common Shell and Permission-Specific UI Behavior

### Common Shell

- Shell layout은 mini-sidebar + top header + content region 구조를 사용한다.
- Sidebar collapsed 상태에서는 icon과 tooltip 중심으로 표시하고, expanded 상태에서는 top/middle/leaf hierarchy label을 표시한다.
- Header는 site name, current user/profile area, theme toggle affordance를 포함한다.
- Content region은 후속 per-screen batch가 작성할 검색조건, 목록, 상세/편집, message, loading/empty/error/permission/success state를 수용하는 공통 container를 제공한다.
- Browser API base는 상대경로 `/api/...`만 사용한다.

### Permission-Specific Behavior

- 인증 세션이 없으면 protected route 접근 시 `/login`으로 이동하거나 인증 필요 상태를 표시한다.
- 인증되었으나 해당 leaf menu 권한이 없으면 sidebar에서 leaf를 숨긴다.
- 사용자가 숨겨진 route를 직접 입력하면 permission denied content state를 표시하고, API 호출은 403 `ApiError`를 화면 오류 상태와 연결한다.
- menu 권한은 역할, 조직, 사용자 단위 설정 결과를 동일한 server authorization과 menu visibility에 적용한다.
- R09 seed administrator는 1차 목표 9개 관리 leaf menu가 기본으로 보이는 기준 persona다.

## Screen Skeleton Ledger

| 화면ID | route | role | menu_path | primary_entity | owning story | operationId 또는 path |
|---|---|---|---|---|---|---|
| SCR-LOGIN | /login | anonymous, R01~R09 after session | 인증 | sessions | US-01 | login / getCurrentUser |
| SCR-USER-MGMT | /admin/users | R09 | 시스템 관리 > 사용자·조직 관리 > 사용자 관리 | users | US-02 | searchUsers |
| SCR-ORG-MGMT | /admin/organizations | R09 | 시스템 관리 > 사용자·조직 관리 > 조직 관리 | organizations | US-03 | searchOrganizations / getOrganizationTree / saveOrganizationParentRelation |
| SCR-ROLE-MGMT | /admin/roles | R09 | 시스템 관리 > 역할·권한 관리 > 역할 관리 | roles | US-04 | listRoles / updateRole |
| SCR-USER-ROLE-MGMT | /admin/user-roles | R09 | 시스템 관리 > 역할·권한 관리 > 사용자 역할 관리 | user_roles | US-05 | listUserRoleAssignments / assignUserRole / updateUserRole / revokeUserRole |
| SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | R09 | 시스템 관리 > 역할·권한 관리 > 메뉴 권한 관리 | menu_permissions | US-06 | listMenuPermissions / saveMenuPermissions |
| SCR-MENU-STRUCTURE-MGMT | /admin/menu-structure | R09 | 시스템 관리 > 메뉴 관리 > 메뉴 구조 관리 | menus | US-07 | getMenuTree / updateMenuParent / reorderMenus |
| SCR-MENU-INFO-MGMT | /admin/menu-info | R09 | 시스템 관리 > 메뉴 관리 > 메뉴 정보 관리 | menu_execution_info | US-08 | getMenuExecution / updateMenuExecution |
| SCR-CODE-GROUP-MGMT | /admin/code-groups | R09 | 시스템 관리 > 공통코드 관리 > 코드그룹 관리 | code_groups | US-09 | listCodeGroups / createCodeGroup / updateCodeGroup |
| SCR-DETAIL-CODE-MGMT | /admin/detail-codes | R09 | 시스템 관리 > 공통코드 관리 > 상세코드 관리 | detail_codes | US-10 | listDetailCodes / createDetailCode / updateDetailCode |

## 공통 계약 참조

| 항목 | 계약 | 관련 canonical_id |
|---|---|---|
| technology_stack.backend | Java 17, Spring Boot 3.3.x, Maven, MyBatis, PostgreSQL 16, executable JAR | REQ-088 |
| technology_stack.frontend | React 18, TypeScript, Vite 5, nginx static serving and `/api/*` reverse proxy | REQ-089 |
| technology_stack.infra | Docker Compose backend/frontend/database and Flyway migration | REQ-001, REQ-090, REQ-100 |
| version_bom | spring_boot 3.3.x; node 20.x; vite 5.x; react 18.x; postgres 16.x | REQ-088, REQ-089 |
| required_outputs | backend_dir=`backend`; frontend_dir=`frontend`; compose_file=`infra/docker-compose.yml`; health_endpoint=`/api/health` | REQ-001, REQ-101 |
| execution_persistence | imperative + MyBatis + blocking; exact marker JSON block의 semantic SSOT는 `plan.md` | REQ-088, REQ-090 |



## Menu Information Architecture

| top | middle | leaf | 화면ID | route | role | grouping rationale | default behavior |
|---|---|---|---|---|---|---|---|
| 인증 | 로그인 | 로그인 | SCR-LOGIN | /login | anonymous, R01~R09 after session | 인증 route는 protected system management shell의 전제이므로 별도 top entry로 둔다. | shell 밖 단독 화면이며 인증 성공 후 접힘 상태의 시스템 관리 sidebar로 전환한다. |
| 시스템 관리 | 사용자·조직 관리 | 사용자 관리 | SCR-USER-MGMT | /admin/users | R09 | 원문 menu path가 `시스템 관리 > 사용자·조직 관리 > 사용자 관리`로 명시되어 있다. | top `시스템 관리`는 expanded, middle group은 current route에서 expanded, 다른 middle group은 collapsed 가능하다. |
| 시스템 관리 | 사용자·조직 관리 | 조직 관리 | SCR-ORG-MGMT | /admin/organizations | R09 | 원문 menu path가 `시스템 관리 > 사용자·조직 관리 > 조직 관리`로 명시되어 있다. | current route 진입 시 사용자·조직 관리 group을 expanded로 유지한다. |
| 시스템 관리 | 역할·권한 관리 | 역할 관리 | SCR-ROLE-MGMT | /admin/roles | R09 | 원문 menu path가 `시스템 관리 > 역할·권한 관리 > 역할 관리`로 명시되어 있다. | current route 진입 시 역할·권한 관리 group을 expanded로 유지한다. |
| 시스템 관리 | 역할·권한 관리 | 사용자 역할 관리 | SCR-USER-ROLE-MGMT | /admin/user-roles | R09 | 원문 menu path가 `시스템 관리 > 역할·권한 관리 > 사용자 역할 관리`로 명시되어 있다. | current route 진입 시 역할·권한 관리 group을 expanded로 유지한다. |
| 시스템 관리 | 역할·권한 관리 | 메뉴 권한 관리 | SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | R09 | 원문 menu path가 `시스템 관리 > 역할·권한 관리 > 메뉴 권한 관리`로 명시되어 있다. | current route 진입 시 역할·권한 관리 group을 expanded로 유지한다. |
| 시스템 관리 | 메뉴 관리 | 메뉴 구조 관리 | SCR-MENU-STRUCTURE-MGMT | /admin/menu-structure | R09 | 원문 menu path가 `시스템 관리 > 메뉴 관리 > 메뉴 구조 관리`로 명시되어 있다. | current route 진입 시 메뉴 관리 group을 expanded로 유지한다. |
| 시스템 관리 | 메뉴 관리 | 메뉴 정보 관리 | SCR-MENU-INFO-MGMT | /admin/menu-info | R09 | 원문 menu path가 `시스템 관리 > 메뉴 관리 > 메뉴 정보 관리`로 명시되어 있다. | current route 진입 시 메뉴 관리 group을 expanded로 유지한다. |
| 시스템 관리 | 공통코드 관리 | 코드그룹 관리 | SCR-CODE-GROUP-MGMT | /admin/code-groups | R09 | 원문 menu path가 `시스템 관리 > 공통코드 관리 > 코드그룹 관리`로 명시되어 있다. | current route 진입 시 공통코드 관리 group을 expanded로 유지한다. |
| 시스템 관리 | 공통코드 관리 | 상세코드 관리 | SCR-DETAIL-CODE-MGMT | /admin/detail-codes | R09 | 원문 menu path가 `시스템 관리 > 공통코드 관리 > 상세코드 관리`로 명시되어 있고 코드그룹 상세 연결 journey와 같은 business object를 공유한다. | current route 진입 시 공통코드 관리 group을 expanded로 유지한다. |

## Screen Inventory

| 화면ID | archetype | interaction_summary | route | role | state | operationId 또는 path | canonical_id | source_id | 비고 | menu_path | primary_entity |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SCR-LOGIN | authentication form | 사용자가 `loginId`와 `password`를 입력해 `login`을 실행하고, 성공 시 `getCurrentUser`로 R09 시스템관리자 세션과 1차 목표 메뉴 접근 가능 상태를 확인한다. 실패 시 입력 오류 또는 인증 실패 메시지를 같은 화면에 표시하고, 필요 시 `getHealth`로 서비스 상태를 확인한다. | /login | anonymous, R01~R09 after session | loading/empty/error/permission/success | login / getCurrentUser | REQ-078 | null | seed 관리자 `admin`/`admin` 안내와 README 또는 quickstart 검증 안내를 로그인 화면 하단 도움말로 노출한다. | 인증 | sessions |
| SCR-USER-MGMT | search/list/detail | R09 시스템관리자가 사용자를 검색하고 KORUS 원천정보를 읽기 전용으로 확인한 뒤 로컬 관리 항목인 시스템 사용여부와 업무 역할을 저장한다. 목록·상세·편집·저장/취소·로딩/빈 결과/오류/권한 없음/성공 상태를 제공한다. | /admin/users | R09 | loading/empty/error/permission/success | searchUsers | REQ-010 | CMN-FR-001 | 상세 요구사항과 화면 상태·action·데이터 매핑은 아래 섹션에서 canonical_id별로 추적한다. KORUS 원천 필드는 직접 편집하지 않는다. | 시스템 관리 > 사용자·조직 관리 > 사용자 관리 | users |
| SCR-ORG-MGMT | tree editor | R09 시스템관리자가 조직코드로 대학·대학원·단과대학·학과·부서를 조회하고, 조직 계층을 확인한 뒤 선택 조직의 상위조직과 적용 시작일·종료일을 저장한다. | /admin/organizations | R09 | loading/empty/error/permission/success | searchOrganizations / getOrganizationTree / saveOrganizationParentRelation | REQ-016, REQ-019, REQ-020, REQ-021, REQ-080 | CMN-FR-002 | 상위조직 변경 이력 조회 범위는 REQ-009 OQ이므로 화면에는 저장 결과와 관계 적용기간 보존 증거만 표시하고 별도 이력 조회 CTA는 OQ-UI-031로 보존한다. | 시스템 관리 > 사용자·조직 관리 > 조직 관리 | organizations |
| SCR-ROLE-MGMT | search/list/detail | R09 시스템관리자가 R01~R09 역할 목록과 역할별 목적을 조회하고, 선택한 역할의 역할명·목적·부여 기준·데이터 범위 기본값·변경 사유를 편집한 뒤 저장한다. | /admin/roles | R09 | loading/empty/error/permission/success | listRoles / updateRole | REQ-026, REQ-081 | CMN-FR-005, null | 역할코드는 readonly 불변이며 신규 역할코드 추가 CTA는 제공하지 않는다. updateRole은 REQ-025/REQ-027/REQ-028/API·data 계약을 참조한다. | 시스템 관리 > 역할·권한 관리 > 역할 관리 | roles |
| SCR-USER-ROLE-MGMT | permission assignment editor | R09 시스템관리자가 사용자별 현재 역할과 유효기간을 조회하고, R01~R09 중 하나 이상의 역할을 부여·변경·회수하며 보직 기반 역할과 수동 역할을 구분해 확인한다. | /admin/user-roles | R09 | loading/empty/error/permission/success | listUserRoleAssignments / assignUserRole / updateUserRole / revokeUserRole | REQ-030, REQ-031, REQ-032, REQ-033, REQ-034, REQ-082 | CMN-FR-006, null | 승인자는 별도 입력하지 않고 REQ-035 data-contract에 따라 로그인 관리자를 자동 기록한다. 사용자 식별/검색 UI의 구체 필드는 OQ-UI-051로 보존한다. | 시스템 관리 > 역할·권한 관리 > 사용자 역할 관리 | user_roles |
| SCR-MENU-PERMISSION-MGMT | permission matrix | R09 시스템관리자가 ROLE/ORGANIZATION/USER 대상 유형과 대상 식별자를 선택해 대메뉴·중메뉴·화면 접근권한을 조회하고, 선택한 메뉴의 accessAllowed 값을 ALLOW 또는 DENY로 저장한다. | /admin/menu-permissions | R09 | loading/empty/error/permission/success | listMenuPermissions / saveMenuPermissions | REQ-036, REQ-038, REQ-039, REQ-040, REQ-083 | CMN-FR-007, null | 화면은 권한 설정 UI만 제공하며, 권한 우선순위 USER > ORGANIZATION > ROLE 및 DENY 우선 규칙은 saveMenuPermissions/listMenuPermissions의 API 계약 REQ-037/REQ-041/REQ-042/REQ-043을 참조한다. | 시스템 관리 > 역할·권한 관리 > 메뉴 권한 관리 | menu_permissions |
| SCR-MENU-STRUCTURE-MGMT | tree editor | R09 시스템관리자가 대메뉴·중메뉴·소메뉴 계층구조를 조회하고, 선택 메뉴의 부모메뉴를 지정·변경하거나 동일 계층 내 표시 순서를 재정렬한 뒤 저장한다. | /admin/menu-structure | R09 | loading/empty/error/permission/success | getMenuTree / updateMenuParent / reorderMenus | REQ-044, REQ-045, REQ-046, REQ-084 | CMN-FR-013, null | 자기 자신 또는 하위 메뉴를 부모로 지정하는 저장은 updateMenuParent 서버 검증에서 차단한다(REQ-047/REQ-048 API 계약 참조). 메뉴명·화면ID·URL·아이콘·업무구분·설명 편집은 US-08 `SCR-MENU-INFO-MGMT` 소유다. | 시스템 관리 > 메뉴 관리 > 메뉴 구조 관리 | menus |
| SCR-MENU-INFO-MGMT | content editor | R09 시스템관리자가 메뉴를 선택해 메뉴명·화면ID·URL·아이콘·업무구분·설명을 조회하고, 실행 화면 연결 정보를 저장한 뒤 같은 메뉴의 실행정보를 다시 확인한다. | /admin/menu-info | R09 | loading/empty/error/permission/success | getMenuExecution / updateMenuExecution | REQ-049, REQ-050, REQ-051, REQ-052, REQ-085 | CMN-FR-014 | Screen Skeleton Ledger의 화면ID·route·role·menu_path·primary_entity·operationId를 그대로 사용한다. 메뉴 목록 자체의 tree 제공은 getMenuTree가 담당하지만 이 fragment의 API-backed 저장/조회는 getMenuExecution / updateMenuExecution만 사용한다. | 시스템 관리 > 메뉴 관리 > 메뉴 정보 관리 | menu_execution_info |
| SCR-CODE-GROUP-MGMT | search/list/detail | R09 시스템관리자가 코드그룹을 조회하고, 그룹ID·명칭·설명·관리부서를 등록·수정한 뒤 목록에서 코드그룹별 상세코드 목록으로 이동한다. | /admin/code-groups | R09 | loading/empty/error/permission/success | listCodeGroups / createCodeGroup / updateCodeGroup | REQ-053, REQ-054, REQ-055, REQ-086 | CMN-FR-016 | Screen Skeleton Ledger의 화면ID·route·role·menu_path·primary_entity·operationId를 그대로 사용한다. 그룹ID 등록 후 수정 가능 여부는 REQ-056 OQ이므로 화면 계약에 OQ-UI-090으로 보존하고 임의로 editable/readonly를 확정하지 않는다. | 시스템 관리 > 공통코드 관리 > 코드그룹 관리 | code_groups |
| SCR-DETAIL-CODE-MGMT | tree editor + effective-period form | R09 시스템관리자가 코드그룹을 지정해 상세코드 목록과 상위코드 계층을 조회하고, 코드값·코드명·상위코드·정렬순서·추가속성·사용여부·유효기간을 등록·수정한 뒤 같은 코드그룹 상세코드를 재조회한다. | /admin/detail-codes | R09 | loading/empty/error/permission/success | listDetailCodes / createDetailCode / updateDetailCode | REQ-057, REQ-058, REQ-059, REQ-060, REQ-061, REQ-087 | CMN-FR-017, null | Screen Skeleton Ledger의 화면ID·route·role·menu_path·primary_entity·operationId를 그대로 사용한다. `additionalAttributes`의 구조·개수·형식과 연계 코드 매핑 기준은 REQ-062 OQ이므로 OQ-UI-100으로 표시하고 임의 필드 세트를 확정하지 않는다. | 시스템 관리 > 공통코드 관리 > 상세코드 관리 | detail_codes |

## Screen Action Mapping

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| ACT-LOGIN-001 | SCR-LOGIN | `[ 로그인 ]` 클릭 | login | POST | `admin`/`admin`이 유효하면 session cookie가 생성되고 `getCurrentUser` 확인 단계로 진행한다. | 필수 입력 누락은 field-level 오류, credential 불일치는 401 인증 실패 메시지를 상태 메시지 영역에 표시한다. |
| ACT-LOGIN-002 | SCR-LOGIN | `login` 성공 직후 현재 사용자 확인 | getCurrentUser | GET | 현재 사용자 `admin`과 R09 시스템관리자 역할이 확인되면 protected shell 이동을 허용한다. | 세션이 없거나 만료되면 401 상태를 로그인 화면에 표시하고, R09 또는 메뉴 권한이 없으면 permission 상태를 표시한다. |
| ACT-LOGIN-003 | SCR-LOGIN | `[ 서비스 상태 확인 ]` 클릭 | getHealth 또는 GET /api/health | GET | `/api/health` 200 결과를 로컬 검증 안내 panel에 표시한다. | backend 상태 확인 실패 또는 네트워크 오류를 서비스 상태 오류 메시지로 표시한다. |
| ACT-LOGIN-004 | SCR-LOGIN | 로그인 화면의 README 또는 quickstart 확인 안내 클릭 또는 포커스 | N/A — 문서 안내 local behavior; 외부 API나 업무 데이터를 변경하지 않음 | N/A | 실행 방법, 로그인 계정, 주요 화면 검증 방법은 README 또는 quickstart에 명시된다는 안내를 화면 하단 도움말로 노출한다. | 문서 링크 target이 구현 시 확정되지 않으면 `OQ-UI-002`를 표시하고 인증 흐름은 차단하지 않는다. |
| ACT-LOGIN-005 | SCR-LOGIN | 로그인 입력값 수정 | N/A — client-side form state reset | N/A | 사용자가 `loginId` 또는 `password`를 수정하면 해당 field 오류와 인증 실패 메시지가 해제된다. | 입력값이 계속 비어 있으면 다음 `login` 요청에서 field-level 오류가 다시 표시된다. |
| ACT-USER-SEARCH | SCR-USER-MGMT | 검색조건 영역의 `조회` 클릭 또는 Enter | searchUsers | GET | 사용자 목록에 조건에 맞는 교번·성명·소속·직급·재직상태·역할·사용여부와 보직·퇴직일자·최종 동기화일시가 표시된다. | 400/401/403/5xx 시 목록 영역에 ApiError 메시지를 표시하고 이전 성공 목록을 저장 성공으로 오인하지 않는다. |
| ACT-USER-RESET | SCR-USER-MGMT | `조건 초기화` 클릭 | N/A | N/A | employeeNo, name, organizationCodeFilter, rankName, employmentStatus, roleCodeFilter, systemUseYn 입력을 비우는 local-only 동작이며 서버 데이터 변경이 없다. | local-only 동작이므로 API 오류는 없고, 초기화 후 조회하지 않았음을 화면 상태로 유지한다. |
| ACT-USER-SELECT | SCR-USER-MGMT | 사용자 목록 행 클릭 | N/A | N/A | 선택 행의 KORUS 원천정보와 로컬 관리 필드를 상세/편집 폼에 표시하는 local-only 동작이다. | 선택 가능한 행이 없으면 상세/저장 버튼을 비활성화한다. |
| ACT-USER-SAVE-ACCOUNT | SCR-USER-MGMT | `시스템 사용여부 저장` 클릭 | updateUserAccount | PATCH | `사용자 정보가 저장되었습니다.` 메시지와 함께 같은 검색조건으로 재조회된 목록/상세에서 systemUseYn 변경값이 보인다. | 400 field error는 systemUseYn/changeReason 옆에 표시하고, 401/403은 permission 또는 인증 필요 상태로 전환하며, KORUS 원천 필드는 변경하지 않는다. |
| ACT-USER-SAVE-ROLES | SCR-USER-MGMT | `업무 역할 저장` 클릭 | updateUserRoles | PATCH | `사용자 정보가 저장되었습니다.` 메시지와 함께 선택 행 역할 표시와 상세 roleCodes가 저장값으로 갱신된다. | 400 field error는 roleCodes/validStartDate/validEndDate/changeReason 옆에 표시하고, 401/403은 permission 또는 인증 필요 상태로 전환하며 기존 역할 표시를 유지한다. |
| ACT-USER-CANCEL | SCR-USER-MGMT | `취소` 클릭 | N/A | N/A | 상세/편집 폼의 미저장 systemUseYn, roleCodes, 유효기간, changeReason 값을 마지막 조회 결과로 되돌리는 local-only 동작이다. | local-only 동작이므로 API 오류는 없고 선택 행이 없으면 버튼을 비활성화한다. |
| ACT-ORG-SEARCH | SCR-ORG-MGMT | 조직코드 또는 조직유형 입력 후 `조회` 클릭 | searchOrganizations / GET /api/admin/organizations | GET | 조직코드 기준으로 대학·대학원·단과대학·학과·부서 목록이 표시된다. | 401/403은 permission state, 4xx/5xx ApiError는 오류 banner와 `다시 조회` 표시. |
| ACT-ORG-TREE-LOAD | SCR-ORG-MGMT | 화면 진입 또는 `다시 조회` 클릭 | getOrganizationTree / GET /api/admin/organizations/tree | GET | 조직의 상위·하위 관계가 tree로 표시되고 선택 node가 목록 row와 동기화된다. | 계층 조회 실패 banner를 표시하고 목록 조회 결과는 유지한다. |
| ACT-ORG-ROW-SELECT | SCR-ORG-MGMT | 조직 목록 row 또는 tree node 클릭 | N/A | local-only | 선택 조직의 organization_code, organization_name, organization_type, 현재 parentOrganizationCode/effective dates form이 열린다. | 선택 데이터가 없으면 form에 `조직을 선택하세요`를 표시한다. |
| ACT-ORG-SAVE-RELATION | SCR-ORG-MGMT | `상위관계 저장` 클릭 후 확인 dialog 승인 | saveOrganizationParentRelation / PUT /api/admin/organizations/{organizationCode}/parent-relations | PUT | 선택 조직 관계와 effectiveStartDate/effectiveEndDate 저장 후 tree와 선택 상세를 재조회한다. | 날짜 역전 또는 기간 중복은 field-level/server error를 표시하고 기존 관계를 유지한다. |
| ACT-ORG-CANCEL | SCR-ORG-MGMT | `취소` 클릭 | N/A | local-only | 편집 중 parentOrganizationCode/effectiveStartDate/effectiveEndDate/changeReason 값을 마지막 조회 상태로 되돌리고 선택 조직은 유지한다. | not applicable |
| ACT-ORG-HISTORY-OQ | SCR-ORG-MGMT | 상위조직 변경 이력 확인 필요 영역 | N/A | support-only | OQ-UI-031로 `조직 개편 시 상위조직 변경 이력 조회 화면/API 제공 범위와 보존 단위`가 미확정임을 표시한다. | 미확정 범위를 임의 history CTA 또는 다운로드로 구현하지 않는다. |
| ACT-ROLE-LIST-QUERY | SCR-ROLE-MGMT | 화면 진입 또는 [조회] 클릭 | listRoles | GET | R01~R09 역할 목록과 role_name, purpose가 목록에 표시된다. | 401/403이면 permission 상태, 그 외 ApiError이면 목록 상단 error banner 표시. |
| ACT-ROLE-LIST-RESET | SCR-ROLE-MGMT | [초기화] 클릭 | N/A | N/A | OQ-UI-041 검색조건이 확정될 때까지 local-only로 검색조건 영역을 초기 표시 상태로 되돌린다. | local-only action이므로 서버 오류는 없고, 미확정 검색조건은 OQ-UI-041로 유지한다. |
| ACT-ROLE-ROW-SELECT | SCR-ROLE-MGMT | 역할 목록에서 R01~R09 row 클릭 | N/A | N/A | 선택한 row가 강조되고 상세/편집 패널에 role_code readonly, role_name, purpose, assignment_criteria, default_data_scope가 표시된다. | 선택 row가 없으면 상세/편집 패널에 `역할을 선택하세요` 안내를 표시한다. |
| ACT-ROLE-SAVE | SCR-ROLE-MGMT | [저장] 클릭 후 확인 modal 승인 | updateRole | PUT | 저장 성공 message를 표시하고 listRoles 재조회 후 선택 roleCode 상세에 최신 값이 표시된다. | 400 ApiError.fields를 필드 하단에 표시하고 기존 persisted 값은 목록 재조회 전까지 변경 확정으로 표시하지 않는다. 403이면 permission 상태로 전환한다. |
| ACT-ROLE-CANCEL | SCR-ROLE-MGMT | [취소] 클릭 | N/A | N/A | 편집 중 입력값을 마지막으로 선택/조회된 상세값으로 복원하고 success/error message를 닫는다. | local-only action이므로 서버 오류는 없으며 선택 row가 없으면 버튼을 비활성화한다. |
| ACT-ROLE-PAGE | SCR-ROLE-MGMT | pagination page/size 변경 | listRoles | GET | 요청한 page/size에 맞는 역할 목록이 표시된다. | listRoles 오류 banner를 표시하고 직전 목록은 읽기 전용으로 유지한다. |
| ACT-USER-ROLE-LIST-QUERY | SCR-USER-ROLE-MGMT | 화면 진입 또는 [조회] 클릭 | listUserRoleAssignments | GET | 사용자 역할 목록에 assignment_id, user_id, role_code, assignment_type, 유효 시작일, 유효 종료일, status가 표시된다. | 401/403이면 permission 상태, 그 외 ApiError이면 목록 상단 error banner 표시. |
| ACT-USER-ROLE-RESET | SCR-USER-ROLE-MGMT | [초기화] 클릭 | N/A | N/A | roleCodeFilter와 OQ-UI-051 사용자 식별 입력을 초기 상태로 되돌리고 목록 선택을 해제한다. | local-only action이므로 서버 오류는 없고, 미확정 사용자 검색 방식은 OQ-UI-051로 유지한다. |
| ACT-USER-ROLE-ROW-SELECT | SCR-USER-ROLE-MGMT | 사용자 역할 목록 row 클릭 | N/A | N/A | 선택한 assignment_id row가 강조되고 폼에 assignment_id, user_id, role_code, assignment_type, valid_start_date, valid_end_date, status가 채워진다. | 선택 row가 없으면 변경 저장·역할 회수 CTA를 비활성화하고 `역할 row를 선택하세요`를 표시한다. |
| ACT-USER-ROLE-CURRENT-QUERY | SCR-USER-ROLE-MGMT | userId 입력 또는 row 선택 후 [현재 역할 조회] 클릭 | listCurrentUserRoles 또는 GET /api/admin/users/{userId}/roles | GET | 선택 user_id의 현재 역할과 역할별 유효기간이 현재 역할 영역에 표시되고 POSITION/MANUAL badge가 구분된다. | userId가 비어 있으면 field error를 표시하고 API를 호출하지 않는다. 403이면 permission 상태로 전환한다. |
| ACT-USER-ROLE-ASSIGN | SCR-USER-ROLE-MGMT | [역할 부여] 클릭 후 확인 modal 승인 | assignUserRole | POST | 새 user_roles row가 생성되고 목록/현재 역할 재조회 후 role_code, valid_start_date, valid_end_date, approver_user_id 자동 기록 안내가 표시된다. | 400 ApiError.fields를 필드 하단에 표시하고 새 row 성공 메시지는 표시하지 않는다. 403이면 permission 상태로 전환한다. |
| ACT-USER-ROLE-UPDATE | SCR-USER-ROLE-MGMT | 선택 row에서 [변경 저장] 클릭 후 확인 modal 승인 | updateUserRole | PUT | 선택 assignment_id의 role_code 또는 유효기간 변경 결과가 목록/현재 역할 재조회 후 표시된다. | assignment_id가 없으면 local error를 표시한다. 400 ApiError.fields를 필드 하단에 표시하고 persisted 성공 표시를 하지 않는다. |
| ACT-USER-ROLE-REVOKE | SCR-USER-ROLE-MGMT | 선택 row에서 [역할 회수] 클릭 후 위험 확인 modal 승인 | revokeUserRole | DELETE | 선택 assignment_id가 status=REVOKED로 표시되거나 현재 역할 목록에서 제거되고 `회수 처리되었습니다` 메시지가 표시된다. | assignment_id가 없으면 local error를 표시한다. 400/403 ApiError이면 기존 ACTIVE row를 성공 처리로 표시하지 않는다. |
| ACT-USER-ROLE-CANCEL | SCR-USER-ROLE-MGMT | [취소] 클릭 | N/A | N/A | 편집 중 입력값을 마지막으로 선택/조회된 값으로 복원하고 confirm modal과 success/error message를 닫는다. | local-only action이므로 서버 오류는 없으며 선택 row가 없으면 변경/회수 관련 복원은 비활성화한다. |
| ACT-USER-ROLE-PAGE | SCR-USER-ROLE-MGMT | pagination page/size 변경 | listUserRoleAssignments | GET | 요청한 page/size에 맞는 사용자 역할 목록이 표시된다. | listUserRoleAssignments 오류 banner를 표시하고 직전 목록은 읽기 전용으로 유지한다. |
| ACT-MENU-PERMISSION-QUERY | SCR-MENU-PERMISSION-MGMT | 화면 진입 또는 targetType/targetId 입력 후 [조회] 클릭 | listMenuPermissions | GET | 선택한 targetType/targetId의 대메뉴·중메뉴·화면 접근권한 matrix와 ALLOW/DENY 값이 표시된다. | 401/403이면 permission 상태, 그 외 ApiError이면 matrix 상단 error banner를 표시한다. |
| ACT-MENU-PERMISSION-RESET | SCR-MENU-PERMISSION-MGMT | [초기화] 클릭 | N/A | N/A | local-only로 targetType을 ROLE, targetId를 비움, accessAllowed를 전체로 되돌리고 아직 API를 호출하지 않는다. | local-only action이므로 서버 오류는 없고, 초기화 후 [조회] 전에는 기존 선택 상세를 닫는다. |
| ACT-MENU-PERMISSION-ROW-SELECT | SCR-MENU-PERMISSION-MGMT | matrix에서 대메뉴·중메뉴·화면 row 클릭 | N/A | N/A | 선택 row가 강조되고 상세/저장 패널에 targetType, targetId, menuId, accessAllowed, changeReason 입력이 표시된다. | 선택 row가 없으면 상세/저장 패널에 `메뉴 권한 행을 선택하세요` 안내를 표시하고 [저장]은 비활성화한다. |
| ACT-MENU-PERMISSION-ACCESS-CHANGE | SCR-MENU-PERMISSION-MGMT | 선택 row의 accessAllowed select를 ALLOW 또는 DENY로 변경 | N/A | N/A | 변경 상태가 `편집 중`으로 표시되고 [저장]이 활성화된다. | accessAllowed가 ALLOW/DENY가 아니면 local validation 메시지 `ALLOW 또는 DENY만 선택할 수 있습니다`를 표시한다. |
| ACT-MENU-PERMISSION-SAVE | SCR-MENU-PERMISSION-MGMT | [저장] 클릭 후 확인 modal 승인 | saveMenuPermissions | PUT | 저장 성공 message를 표시하고 listMenuPermissions 재조회 후 matrix에 최신 accessAllowed가 표시된다. | 400 ApiError.fields를 필드 하단에 표시하고 기존 persisted 값은 재조회 전까지 변경 확정으로 표시하지 않는다. 403이면 permission 상태로 전환한다. |
| ACT-MENU-PERMISSION-CANCEL | SCR-MENU-PERMISSION-MGMT | [취소] 클릭 | N/A | N/A | 선택 row의 accessAllowed와 changeReason을 마지막으로 조회된 값으로 복원하고 success/error message를 닫는다. | local-only action이므로 서버 오류는 없으며 선택 row가 없으면 버튼을 비활성화한다. |
| ACT-MENU-PERMISSION-PAGE | SCR-MENU-PERMISSION-MGMT | pagination page/size 변경 | listMenuPermissions | GET | 요청한 page/size에 맞는 메뉴 권한 matrix page가 표시된다. | listMenuPermissions 오류 banner를 표시하고 직전 matrix는 읽기 전용으로 유지한다. |
| ACT-MENU-STRUCTURE-LOAD | SCR-MENU-STRUCTURE-MGMT | 화면 진입 또는 [새로고침] 클릭 | getMenuTree | GET | 대메뉴·중메뉴·소메뉴 tree가 parent_menu_id 관계와 display_order 순서대로 표시된다. | 401/403이면 permission 상태, 그 외 ApiError이면 tree 영역 상단 error banner 표시. |
| ACT-MENU-STRUCTURE-SELECT | SCR-MENU-STRUCTURE-MGMT | tree에서 메뉴 node 클릭 | N/A | N/A | 선택 node가 강조되고 선택 메뉴의 menu_name, menu_type, 현재 parent_menu_id, display_order가 편집 영역에 표시된다. | local-only action이므로 서버 오류는 없고 선택 node가 없으면 부모메뉴 저장·순서 저장 CTA를 비활성화한다. |
| ACT-MENU-STRUCTURE-PARENT-CHANGE | SCR-MENU-STRUCTURE-MGMT | 새 부모메뉴 select 변경 | N/A | N/A | parentMenuId 편집값을 local state에 반영하고 자기 자신/하위 메뉴 선택 가능성은 저장 전 안내 문구로 표시한다. | local-only action이므로 서버 오류는 없으며 최종 차단은 updateMenuParent ApiError.fields.parentMenuId로 표시한다. |
| ACT-MENU-STRUCTURE-PARENT-SAVE | SCR-MENU-STRUCTURE-MGMT | [부모메뉴 저장] 클릭 후 확인 modal 승인 | updateMenuParent | PUT | 저장 성공 message를 표시하고 getMenuTree 재조회 후 선택 메뉴가 새 부모 아래에 표시된다. | parentMenuId가 자기 자신 또는 하위 메뉴이면 400 ApiError.fields.parentMenuId를 표시하고 기존 tree를 유지한다. 403이면 permission 상태로 전환한다. |
| ACT-MENU-STRUCTURE-REORDER-EDIT | SCR-MENU-STRUCTURE-MGMT | 동일 계층 내 위/아래 이동 또는 drag handle 조작 | N/A | N/A | 현재 parent_menu_id 아래 형제 메뉴의 orderedMenuIds 편집 순서를 화면에 즉시 반영하고 [순서 저장]을 활성화한다. | local-only action이므로 서버 오류는 없고 중복/누락 orderedMenuIds는 저장 시 reorderMenus 오류로 표시한다. |
| ACT-MENU-STRUCTURE-REORDER-SAVE | SCR-MENU-STRUCTURE-MGMT | [순서 저장] 클릭 후 확인 modal 승인 | reorderMenus | PUT | 저장 성공 message를 표시하고 getMenuTree 재조회 후 동일 계층 메뉴가 저장된 display_order 순서로 보인다. | orderedMenuIds 누락·중복 또는 권한 오류가 있으면 ApiError를 순서 영역 아래에 표시하고 직전 확정 tree를 유지한다. |
| ACT-MENU-STRUCTURE-CANCEL | SCR-MENU-STRUCTURE-MGMT | [취소] 클릭 | N/A | N/A | 부모메뉴 선택값과 orderedMenuIds 편집값을 마지막 getMenuTree 응답 기준으로 복원하고 success/error message를 닫는다. | local-only action이므로 서버 오류는 없으며 선택 node가 없으면 버튼을 비활성화한다. |
| ACT-MENU-INFO-001 | SCR-MENU-INFO-MGMT | 메뉴 목록에서 행을 선택한다. | getMenuExecution | GET | 선택한 menuId의 메뉴명·화면ID·URL·아이콘·업무구분·설명이 편집 폼에 표시된다. | 401/403이면 permission 상태, 4xx/5xx이면 error 상태에 ApiError.message를 표시한다. |
| ACT-MENU-INFO-002 | SCR-MENU-INFO-MGMT | 검색조건의 `조회`를 클릭한다. | getMenuExecution | GET | 조건에 맞는 메뉴 실행정보가 있으면 메뉴 목록에 메뉴명·화면ID·URL이 표시되고 첫 선택 또는 사용자가 선택한 행의 실행정보가 표시된다. | 조회 실패 시 메뉴 목록은 기존 또는 빈 상태를 유지하고 오류 안내를 표시한다. |
| ACT-MENU-INFO-003 | SCR-MENU-INFO-MGMT | `저장`을 클릭하고 확인 메시지에서 승인한다. | updateMenuExecution | PUT | 저장 성공 메시지를 표시하고 getMenuExecution 재조회 결과로 메뉴명·화면ID·URL·아이콘·업무구분·설명을 갱신한다. | 필수 field 누락 또는 서버 검증 실패 시 field-level 오류를 표시하고 저장 전 값은 변경하지 않는다. |
| ACT-MENU-INFO-004 | SCR-MENU-INFO-MGMT | `취소`를 클릭한다. | N/A | local-only | API 호출 없이 마지막 getMenuExecution 응답값으로 폼을 되돌리고 편집 중 표시를 제거한다. | 로컬 되돌림 대상이 없으면 선택 전 empty 안내를 표시한다. |
| ACT-MENU-INFO-005 | SCR-MENU-INFO-MGMT | 저장 성공 후 메뉴 클릭 동작을 확인한다. | N/A | navigation-only | 저장된 screenId와 URL로 메뉴 클릭 시 실행 화면 식별과 이동 대상이 일치함을 화면에 표시한다. | 연결 URL이 없거나 permission 상태이면 실행 이동을 막고 원인 메시지를 표시한다. |
| ACT-MENU-INFO-006 | SCR-MENU-INFO-MGMT | `조건 초기화`를 클릭한다. | N/A | local-only | 메뉴명·URL 검색 입력을 비우고 목록/편집 선택은 유지하지 않는다. | 초기화 중 오류는 없음; 로컬 상태만 변경한다. |
| ACT-CODE-GROUP-001 | SCR-CODE-GROUP-MGMT | route `/admin/code-groups` 진입 또는 검색조건의 `조회`를 클릭한다. | listCodeGroups | GET | 조건에 맞는 코드그룹 목록이 그룹ID·명칭·설명·관리부서와 함께 표시된다. | 401/403이면 permission 상태, 4xx/5xx이면 error 상태에 ApiError.message를 표시한다. |
| ACT-CODE-GROUP-002 | SCR-CODE-GROUP-MGMT | `초기화`를 클릭한다. | N/A | local-only | groupIdFilter와 groupName 검색 입력을 비우고 목록 선택 상태를 해제한다. | 로컬 상태 변경이므로 서버 오류는 없고, 이미 표시된 ApiError 안내만 닫는다. |
| ACT-CODE-GROUP-003 | SCR-CODE-GROUP-MGMT | `신규 등록`을 클릭한다. | N/A | local-only | 빈 등록 폼을 열고 groupId, groupName, description, managingDepartment, changeReason 입력을 활성화한다. | 로컬 화면 전환이므로 서버 오류는 없고, 작성 중인 수정 폼이 있으면 취소 확인을 표시한다. |
| ACT-CODE-GROUP-004 | SCR-CODE-GROUP-MGMT | 신규 등록 폼에서 `저장`을 클릭하고 확인한다. | createCodeGroup | POST | 성공 메시지를 표시하고 listCodeGroups 재조회 결과에 신규 groupId 행이 보인다. | 필수 field 누락 또는 서버 검증 실패 시 해당 입력 아래 ApiError.fields를 표시하고 목록은 변경하지 않는다. |
| ACT-CODE-GROUP-005 | SCR-CODE-GROUP-MGMT | 목록 행을 선택해 수정 폼을 연 뒤 `저장`을 클릭하고 확인한다. | updateCodeGroup | PUT | 성공 메시지를 표시하고 listCodeGroups 재조회 결과에서 선택한 groupId의 명칭·설명·관리부서 수정값이 보인다. | groupId 수정 가능 여부 또는 필수값 검증 실패 시 ApiError.fields를 표시하고 저장 전 목록 값을 유지한다. |
| ACT-CODE-GROUP-006 | SCR-CODE-GROUP-MGMT | 목록 행의 `상세코드`를 클릭한다. | N/A | navigation-only | 선택 행의 groupId를 전달해 `/admin/detail-codes`로 이동하고 상세코드 관리 화면에서 해당 코드그룹 기준 조회를 시작할 수 있다. | groupId가 없는 행이면 이동하지 않고 `코드그룹을 먼저 선택하세요` 메시지를 표시한다. |
| ACT-CODE-GROUP-007 | SCR-CODE-GROUP-MGMT | 등록·수정 폼의 `취소`를 클릭한다. | N/A | local-only | API 호출 없이 폼 입력을 마지막 조회 또는 빈 상태로 되돌리고 목록 선택을 유지하거나 해제한다. | 작성 중 변경값이 있으면 로컬 취소 확인을 표시하며, 서버 상태는 변경하지 않는다. |
| ACT-DETAIL-CODE-LOAD | SCR-DETAIL-CODE-MGMT | `/admin/detail-codes` 진입 시 route query groupId가 있거나 [조회]를 클릭한다. | listDetailCodes | GET | 해당 groupId의 상세코드가 목록에 code_value, code_name, parent_code_value, sort_order, system_use_yn, valid_start_date, valid_end_date로 표시되고 계층 미리보기가 갱신된다. | groupId 누락 또는 4xx/5xx이면 error 상태에 ApiError.message를 표시하고 목록은 빈 결과 또는 마지막 확정 결과로 유지한다. |
| ACT-DETAIL-CODE-SELECT | SCR-DETAIL-CODE-MGMT | 상세코드 목록에서 행을 선택한다. | N/A | N/A | 선택 행이 강조되고 codeValue, codeName, parentCodeValue, sortOrder, additionalAttributes, systemUseYn, validStartDate, validEndDate가 수정 폼에 채워진다. | local-only action이므로 서버 오류는 없고 선택 행이 없으면 수정 저장 CTA를 비활성화한다. |
| ACT-DETAIL-CODE-NEW | SCR-DETAIL-CODE-MGMT | [신규]를 클릭한다. | N/A | N/A | 같은 groupId 아래 신규 등록 mode로 전환하고 codeValue, codeName, parentCodeValue, sortOrder, additionalAttributes, systemUseYn, validStartDate, validEndDate, changeReason 입력을 초기화한다. | groupId가 없으면 `코드그룹을 먼저 지정하세요` 로컬 오류를 표시하고 등록 mode로 전환하지 않는다. |
| ACT-DETAIL-CODE-CREATE | SCR-DETAIL-CODE-MGMT | 신규 등록 mode에서 [저장] 클릭 후 확인 modal 승인 | createDetailCode | POST | 저장 성공 메시지를 표시하고 listDetailCodes 재조회 후 새 code_value 행과 parent_code_value 계층 위치가 보인다. | 필수값 누락, additionalAttributes 미확정 구조, 권한 오류가 있으면 ApiError.fields 또는 permission 상태를 표시하고 신규 row는 목록에 추가하지 않는다. |
| ACT-DETAIL-CODE-UPDATE | SCR-DETAIL-CODE-MGMT | 선택 행 수정 mode에서 [저장] 클릭 후 확인 modal 승인 | updateDetailCode | PUT | 저장 성공 메시지를 표시하고 listDetailCodes 재조회 후 선택한 code_value의 code_name, parent_code_value, sort_order, additional_attributes, system_use_yn, valid_start_date, valid_end_date 변경값이 보인다. | 400 ApiError.fields를 해당 입력 아래에 표시하고 목록의 기존 확정값은 유지한다. 403이면 permission 상태로 전환한다. |
| ACT-DETAIL-CODE-CANCEL | SCR-DETAIL-CODE-MGMT | [취소]를 클릭한다. | N/A | N/A | API 호출 없이 마지막 listDetailCodes 응답 기준으로 선택 행과 편집 폼을 복원하고 신규/수정 dirty 상태를 제거한다. | local-only action이므로 서버 오류는 없으며 마지막 조회값이 없으면 empty 안내를 표시한다. |
| ACT-DETAIL-CODE-RESET-FILTER | SCR-DETAIL-CODE-MGMT | [조건 초기화]를 클릭한다. | N/A | N/A | groupId 입력을 비우고 목록, 계층 미리보기, 선택 행, 편집 폼을 선택 전 empty 상태로 되돌린다. | local-only action이므로 서버 오류는 없고 저장 중에는 버튼을 비활성화한다. |

## Feature — US-01

### F-01 UI Design Fragment — US-01 인증과 시드 관리자 접근

#### Design Reference 적용 범위

- story: US-01
- scope: 인증과 시드 관리자 접근
- read-only skeleton source: `.specify/specs/001-feature/ui-design.md`의 Screen Skeleton Ledger
- 이 fragment가 상세화하는 화면: `SCR-LOGIN` only
- 레퍼런스 적용: `ex` 레퍼런스의 미니멀 sidebar·header·프로필·light/dark theme 방향은 로그인 성공 후 protected shell로 전환되는 시각 방향에만 적용한다. `/login` 자체는 shell 밖 단독 인증 화면으로 유지한다.
- 금지: 이 fragment는 route inventory, navigation map, common shell, 다른 story 화면을 재작성하지 않는다.



#### SCR-LOGIN Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ 한국교원대학교 교수업적평가시스템                                           │
│ 공통기능 1차 범위                                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌───────────────────────────────┐      ┌───────────────────────────────┐   │
│   │ 로그인                         │      │ 로컬 검증 안내                │   │
│   │                               │      │                               │   │
│   │ 사용자 ID                     │      │ 시드 관리자 계정              │   │
│   │ [ admin____________________ ] │      │ - loginId: admin              │   │
│   │                               │      │ - password: admin             │   │
│   │ 비밀번호                      │      │                               │   │
│   │ [ •••••___________________ ] │      │ Docker Compose 실행 직후      │   │
│   │                               │      │ 로그인 가능해야 한다.          │   │
│   │ [ 로그인 ]                    │      │                               │   │
│   │                               │      │ [ 서비스 상태 확인 ]           │   │
│   │ 상태 메시지 영역              │      │ 결과: /api/health 200 또는    │   │
│   │ - 입력 누락: field-level error│      │       오류 메시지              │   │
│   │ - 인증 실패: 401 메시지       │      │                               │   │
│   │ - 성공: R09 시스템관리자 확인 │      │ README 또는 quickstart에서     │   │
│   │                               │      │ 실행·로그인·주요 화면 검증     │   │
│   │ [README/quickstart 확인 안내] │      │ 방법을 확인한다.               │   │
│   └───────────────────────────────┘      └───────────────────────────────┘   │
│                                                                              │
│ 성공 흐름: login 성공 → getCurrentUser 확인 → 시스템 관리 shell로 이동       │
│ 권한 거부 흐름: R09가 아니거나 1차 목표 메뉴 권한 없음 → permission 상태 표시│
│ 취소/재입력 흐름: 입력값 수정 또는 비밀번호 재입력 → 로그인 재시도           │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Screen States

| 화면ID | state | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|
| SCR-LOGIN | loading | `[ 로그인 ]` 클릭 후 버튼이 처리 중으로 비활성화되고 상태 메시지 영역에 인증 요청 중임을 표시한다. | login | REQ-075 |
| SCR-LOGIN | empty | 최초 진입 시 `loginId`, `password` 입력칸은 비어 있을 수 있으며, 로컬 검증 안내에는 seed 계정 `admin`/`admin`이 표시된다. | N/A — 초기 render local state | REQ-078 |
| SCR-LOGIN | error | `loginId` 또는 `password` 누락 시 field-level 오류를 입력칸 아래에 표시하고, 인증 실패 401이면 상태 메시지 영역에 인증 실패를 표시한다. | login | REQ-098 |
| SCR-LOGIN | permission | 로그인 후 `getCurrentUser` 결과가 R09 시스템관리자 또는 1차 목표 메뉴 접근 권한을 만족하지 않으면 시스템 관리 shell로 이동하지 않고 권한 없음 상태를 표시한다. | getCurrentUser | REQ-096 |
| SCR-LOGIN | success | `admin`/`admin` 로그인 성공 후 `getCurrentUser`에서 R09 시스템관리자임이 보이면 1차 목표 메뉴 접근 검증이 가능한 protected shell로 이동한다. | login / getCurrentUser | REQ-071 |

#### Per-Screen UI Contract




#### Data Binding — SCR-LOGIN

| UI element | bound source | direction | operationId 또는 path | canonical_id | notes |
|---|---|---|---|---|---|
| `loginId` 입력 | `LoginRequest.loginId` | user input → request body | login | REQ-075 | seed 계정 안내는 `admin` 값을 표시하지만 입력값 자동 고정은 요구하지 않는다. |
| `password` 입력 | `LoginRequest.password` | user input → request body | login | REQ-075 | seed 계정 안내는 `admin` 값을 표시한다. |
| 상태 메시지 영역 | `ApiResponse.success`, `ApiError.error.message`, `ApiError.error.fields` | response → screen | login / getCurrentUser / getHealth | REQ-075 | 2xx/4xx envelope는 `contracts/openapi.yaml`의 `ApiResponse`/`ApiError` 참조를 따른다. |
| R09 시스템관리자 확인 | current user roles | response → route guard | getCurrentUser | REQ-096 | R09가 확인되지 않으면 permission 상태로 남고 protected shell 이동을 막는다. |
| session cookie 상태 | HttpOnly SameSite=Lax `SESSION` cookie | server set-cookie → browser session | login / getCurrentUser | REQ-093 | local HTTP Docker profile은 `Secure=false`, 운영 TLS profile은 `Secure=true` 계약을 따른다. |
| seed 계정 안내 panel | README 또는 quickstart에 명시될 계정 정보 | static/help content | N/A — documentation reference | REQ-097 | 화면은 `admin`/`admin` 로컬 검증 안내를 제공하고 문서 명시 요구를 사용자가 인지할 수 있게 한다. |
| 서비스 상태 결과 | `/api/health` status | response → help panel | getHealth 또는 GET /api/health | REQ-101 | 업무 데이터가 아니라 실행 상태 확인 결과만 표시한다. |

#### List-Capable Screen Notes

| 화면ID | real filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-LOGIN | not applicable | not applicable | not applicable | not applicable | not applicable | 로그인 form의 입력값이 없는 최초 상태를 empty state로 표시하며 목록 empty result는 없다. |

#### Role/State Transition Matrix

| 화면ID | actor/session state | UI behavior | server authorization/API behavior | canonical_id |
|---|---|---|---|---|
| SCR-LOGIN | anonymous, no session | `/login` form과 seed 관리자 안내를 표시한다. | `login` 요청은 credential을 검증하고 성공 시 session cookie를 발급한다. | REQ-075 |
| SCR-LOGIN | anonymous, invalid or missing credential | form을 유지하고 field-level 오류 또는 인증 실패 메시지를 표시한다. | `login`은 400 field error 또는 401 `ApiError`를 반환한다. | REQ-098 |
| SCR-LOGIN | authenticated, R09 시스템관리자 | `/login`에서 protected shell 이동을 허용하고 1차 목표 9개 route 검증 흐름으로 이어진다. | `getCurrentUser`는 R09 역할과 현재 사용자 정보를 반환한다. | REQ-096 |
| SCR-LOGIN | authenticated, not R09 or denied menu permission | permission 상태를 표시하고 시스템 관리 shell로 이동하지 않는다. | 보호 API 또는 route guard는 권한 없음 결과를 반환하거나 차단한다. | REQ-071 |
| SCR-LOGIN | active session logout requested by shell or session control | 로그인 화면 복귀 또는 세션 종료 메시지를 표시한다. | `logout`은 `sessions` 상태를 종료 처리한다. | REQ-075 |

#### Navigation / Sequence Flow — authentication form

```text
[entry /login]
   │
   ├─ empty: loginId/password 입력 대기
   │
   ├─ [서비스 상태 확인] ── getHealth ── 200 표시
   │                         └─ 실패: health 오류 표시 후 /login 유지
   │
   └─ [로그인]
        ├─ 입력 누락 ── login 400 ── field-level error 표시 후 /login 유지
        ├─ credential 불일치 ── login 401 ── 인증 실패 표시 후 /login 유지
        └─ admin/admin 성공 ── session cookie 생성
              └─ getCurrentUser
                    ├─ R09 확인 ── protected system management shell 이동
                    └─ R09/메뉴 권한 미충족 ── permission 상태 표시 후 /login 유지
```



#### 공통 계약 참조

| 항목 | 계약 | 관련 canonical_id |
|---|---|---|
| authentication boundary | `AuthenticationPort` 뒤 기준 구현은 내부 계정·비밀번호·HttpOnly SameSite=Lax 세션 쿠키를 사용한다. | REQ-093 |
| seed credential | 기본 로그인 계정의 아이디와 비밀번호는 `admin`으로 통일한다. | REQ-098 |
| seed administrator role | 시드 관리자는 R09 시스템관리자 역할을 가진다. | REQ-096 |
| UI API base | 브라우저 API 호출은 상대경로 `/api/...` 계약을 따른다. | REQ-070 |
| health endpoint | `getHealth` 또는 `GET /api/health`는 로컬 실행 상태 확인용으로 로그인 화면 보조 panel에서만 사용한다. | REQ-101 |


## Feature — US-02

### F-02 UI Design Fragment: US-02 사용자 관리



#### Wireframe: SCR-USER-MGMT

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ 시스템 관리 > 사용자·조직 관리 > 사용자 관리                                      R09 시스템관리자 │
├──────────────┬─────────────────────────────────────────────────────────────────────────────────────────┤
│ mini sidebar │ 사용자 관리                                                                           │
│ [시스템관리] │ KORUS 원천 인사정보는 읽기 전용이며, 시스템 사용여부와 업무 역할만 로컬 DB에 저장한다. │
│  사용자·조직 │                                                                                         │
│  > 사용자관리│ ┌─ 검색조건 ────────────────────────────────────────────────────────────────────────┐ │
│              │ │ 교번 [employeeNo________] 성명 [name________] 소속 [organizationCodeFilter____]     │ │
│              │ │ 직급 [rankName_________] 재직상태 [employmentStatus v] 역할 [roleCodeFilter v]      │ │
│              │ │ 사용여부 [systemUseYn v]                                  [조회] [조건 초기화]     │ │
│              │ └────────────────────────────────────────────────────────────────────────────────────┘ │
│              │                                                                                         │
│              │ ┌─ 사용자 목록 ──────────────────────────────────────────────────────────────────────┐ │
│              │ │ 교번 | 성명 | 소속 | 직급 | 재직상태 | 역할 | 사용여부 | 보직 | 퇴직일자 | 최종 동기화일시 │ │
│              │ │ 10001 | 홍길동 | EDU-001 | 교수 | ACTIVE | R01,R09 | Y | 학과장 | - | 2026-08-18  │ │
│              │ │ 10002 | 김교원 | EDU-002 | 부교수 | LEAVE | R01 | N | - | - | 2026-08-18        │ │
│              │ │                                                                              1 / N │ │
│              │ └────────────────────────────────────────────────────────────────────────────────────┘ │
│              │                                                                                         │
│              │ ┌─ 선택 사용자 상세 및 로컬 관리 ───────────────────────────────────────────────────┐ │
│              │ │ KORUS 원천정보(읽기 전용): 교번 10001, 성명 홍길동, 소속 EDU-001, 직급 교수,       │ │
│              │ │ 재직상태 ACTIVE, 보직 학과장, 퇴직일자 -, 최종 동기화일시 2026-08-18 09:00          │ │
│              │ │ 로컬 관리: 시스템 사용여부 [Y v]  업무 역할 [R01 교원 ☑] [R09 시스템관리자 ☑]       │ │
│              │ │ 유효 시작일 [OQ-UI-201] 유효 종료일 [OQ-UI-202] 변경 사유 [____________________]   │ │
│              │ │ [시스템 사용여부 저장] [업무 역할 저장] [취소]                                      │ │
│              │ │ 메시지: 저장 성공 시 "사용자 정보가 저장되었습니다." / 오류 시 ApiError.fields 표시 │ │
│              │ └────────────────────────────────────────────────────────────────────────────────────┘ │
│              │                                                                                         │
│              │ 상태 영역: loading=목록 skeleton, empty=조건에 맞는 사용자가 없습니다, error=오류 재시도, │
│              │ permission=사용자 관리 권한이 없습니다, success=저장 후 목록과 상세 갱신                 │
└──────────────┴─────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Archetype Navigation / Sequence Flow

| archetype | entry | success | cancel | error | permission-denied |
|---|---|---|---|---|---|
| search/list/detail | R09가 mini sidebar에서 `시스템 관리 > 사용자·조직 관리 > 사용자 관리`를 선택하면 `/admin/users`로 진입하고 `searchUsers`가 기본 목록을 로드한다. | `시스템 사용여부 저장` 성공 시 `updateUserAccount` 후 현재 검색조건으로 `searchUsers`를 재호출하고 선택 행의 `system_use_yn`을 갱신한다. `업무 역할 저장` 성공 시 `updateUserRoles` 후 선택 행의 역할 표시와 상세 역할 영역을 갱신한다. | `취소`를 누르면 편집 중인 `systemUseYn`, `roleCodes`, `validStartDate`, `validEndDate`, `changeReason` 입력값을 마지막 조회 결과로 되돌리고 `/admin/users`에 머문다. | `searchUsers` 또는 저장 API가 4xx/5xx `ApiError`를 반환하면 목록 또는 상세 영역 상단에 오류 메시지와 field-level 오류를 표시하고 기존 선택 행 값은 저장 성공으로 표시하지 않는다. | 인증 세션이 없으면 `/login`으로 이동한다. 인증되었으나 R09 또는 사용자 관리 메뉴 권한이 없으면 `사용자 관리 권한이 없습니다` 상태를 표시하고 `searchUsers`, `updateUserAccount`, `updateUserRoles` 호출 결과의 403을 연결한다. |

#### UI States: SCR-USER-MGMT

| 상태 | 브라우저 관찰 결과 | API/operationId | canonical_id |
|---|---|---|---|
| loading | `/admin/users` 진입 또는 조회 클릭 직후 검색조건은 유지되고 사용자 목록 영역에 skeleton row 또는 로딩 문구가 표시된다. | searchUsers | REQ-015 |
| empty | 조건에 맞는 사용자 데이터가 없으면 목록 영역에 `조건에 맞는 사용자가 없습니다`가 표시되고 상세/저장 버튼은 비활성화된다. | searchUsers | REQ-010 |
| error | API 오류 시 `사용자 조회 또는 저장 중 오류가 발생했습니다`와 ApiError.fields의 필드별 메시지를 검색/상세 영역 가까이에 표시한다. | searchUsers / updateUserAccount / updateUserRoles | REQ-015 |
| permission | R09 권한 또는 사용자 관리 메뉴 접근권한이 없으면 검색조건과 목록 대신 `사용자 관리 권한이 없습니다`를 표시하고 저장 CTA를 숨긴다. | searchUsers / updateUserAccount / updateUserRoles | REQ-015 |
| success | 저장 성공 후 `사용자 정보가 저장되었습니다.` 메시지를 표시하고 같은 조건으로 재조회된 목록에서 변경된 사용여부 또는 역할이 보인다. | updateUserAccount / updateUserRoles / searchUsers | REQ-013 |

#### Per-Screen UI Contract




#### Data Binding: SCR-USER-MGMT

| UI 영역 | 표시/입력 이름 | source field 또는 parameter | source artifact | 편집 가능 여부 | canonical_id |
|---|---|---|---|---|---|
| 검색조건 | 교번 | employeeNo query parameter | searchUsers / GET /api/admin/users | 입력 가능 | REQ-010 |
| 검색조건 | 성명 | name query parameter | searchUsers / GET /api/admin/users | 입력 가능 | REQ-010 |
| 검색조건 | 소속 | organizationCodeFilter query parameter | searchUsers / GET /api/admin/users | 입력 가능 | REQ-010 |
| 검색조건 | 직급 | rankName query parameter | searchUsers / GET /api/admin/users | 입력 가능 | REQ-010 |
| 검색조건 | 재직상태 | employmentStatus query parameter; korus_personnel_snapshots.employment_status | searchUsers / data-model.md | 입력 가능 | REQ-010 |
| 검색조건 | 역할 | roleCodeFilter query parameter; RoleCodeEnum R01~R09 | searchUsers / contracts/openapi.yaml | 입력 가능 | REQ-010 |
| 검색조건 | 사용여부 | systemUseYn query parameter; SystemUseYnEnum Y,N | searchUsers / contracts/openapi.yaml | 입력 가능 | REQ-010 |
| 목록 | 보직 | korus_personnel_snapshots.position_name 또는 organization_user_mappings.position_name | data-model.md | 읽기 전용 | REQ-011 |
| 목록 | 퇴직일자 | korus_personnel_snapshots.retirement_date | data-model.md | 읽기 전용 | REQ-011 |
| 목록 | 최종 동기화일시 | korus_personnel_snapshots.last_synced_at | data-model.md | 읽기 전용 | REQ-011 |
| 상세 | KORUS 원천정보 | employee_no, name, organization_code, rank_name, employment_status, position_name, retirement_date, last_synced_at | korus_personnel_snapshots | 읽기 전용; payload 제외 | REQ-012 |
| 편집 폼 | 시스템 사용여부 | users.system_use_yn / UpdateUserAccountRequest.systemUseYn | updateUserAccount | 입력 가능 | REQ-013 |
| 편집 폼 | 업무 역할 | user_roles.role_code / UpdateUserRolesRequest.roleCodes | updateUserRoles | 입력 가능 | REQ-014 |
| 편집 폼 | 유효 시작일 | user_roles.valid_start_date / UpdateUserRolesRequest.validStartDate | updateUserRoles | OQ-UI-201: 사용자 관리 화면에서 유효기간을 직접 편집할지 원문이 명시하지 않아 편집 UI는 표시하되 필수/기본값은 구현 전 확인한다. | REQ-014 |
| 편집 폼 | 유효 종료일 | user_roles.valid_end_date / UpdateUserRolesRequest.validEndDate | updateUserRoles | OQ-UI-202: 사용자 관리 화면에서 업무 역할 변경 시 종료일 정책이 원문에 없어 빈 값 허용 여부를 API contract에 맞춰 확인한다. | REQ-014 |
| 편집 폼 | 변경 사유 | users.change_reason, user_roles.change_reason / request changeReason | updateUserAccount / updateUserRoles | 입력 가능 | REQ-013 |

#### List Behavior: SCR-USER-MGMT

| 화면ID | real filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-USER-MGMT | employeeNo, name, organizationCodeFilter, rankName, employmentStatus, roleCodeFilter, systemUseYn | OQ-UI-203: 원문과 OpenAPI에 기본 정렬 기준이 명시되지 않았다. 구현 전 기본 정렬 기준을 정하지 않으면 API 반환 순서를 그대로 표시한다. | page, size query parameter를 사용한다. | not applicable: 원문은 선택한 사용자 단건의 사용여부와 업무 역할 변경만 정의한다. | not applicable: 파일·Excel 기능은 1차 범위 제외다. | `조건에 맞는 사용자가 없습니다`를 목록 영역에 표시하고 상세/저장 CTA를 비활성화한다. |

#### Role / State Transition Matrix

| 화면ID | 사용자/상태 | 허용 UI 동작 | 서버 authorization/operation | 차단 UI 동작 | canonical_id |
|---|---|---|---|---|---|
| SCR-USER-MGMT | R09, 사용자 관리 메뉴 권한 있음 | searchUsers, updateUserAccount, updateUserRoles를 수행하고 성공 후 재조회한다. | SessionCookie + x-roles R09 | KORUS 원천 필드 직접 편집은 UI에서 읽기 전용으로 차단한다. | REQ-013 |
| SCR-USER-MGMT | 인증 없음 | protected route 진입 시 `/login` 이동 또는 인증 필요 상태를 표시한다. | searchUsers/updateUserAccount/updateUserRoles 401 ApiError | 목록 조회와 저장 CTA를 실행하지 않는다. | REQ-015 |
| SCR-USER-MGMT | 인증됨, R09 또는 메뉴 권한 없음 | permission 상태를 표시한다. | searchUsers/updateUserAccount/updateUserRoles 403 ApiError | 사용자 목록, 상세 저장 CTA를 표시하지 않는다. | REQ-015 |
| SCR-USER-MGMT | 선택 행 없음 | 검색조건과 조회는 가능하다. | searchUsers | updateUserAccount/updateUserRoles/취소는 비활성화한다. | REQ-015 |
| SCR-USER-MGMT | 저장 성공 | success 메시지와 재조회 결과를 표시한다. | updateUserAccount/updateUserRoles 후 searchUsers | 이전 미저장 값을 성공값처럼 표시하지 않는다. | REQ-013 |
| SCR-USER-MGMT | 저장 검증 오류 | ApiError.fields를 입력 옆에 표시하고 상세 영역을 error 상태로 둔다. | updateUserAccount/updateUserRoles 400 ApiError | 기존 users.system_use_yn 또는 user_roles row를 화면에서 변경 완료로 표시하지 않는다. | REQ-014 |




## Feature — US-03

### F-03 UI Design Fragment — US-03 조직 관리



#### Archetype Strategy

- 선택 archetype: tree editor.
- 근거: 원문은 조직코드 기준 조회, 상위·하위 계층 조회, 선택 조직과 상위조직 관계 및 적용기간 등록·변경을 같은 `조직 관리` capability로 정의한다.
- 적용 방식: 좌측은 조직코드 검색과 대학·대학원·단과대학·학과·부서 목록, 중앙은 계층 tree, 우측은 선택 조직의 상위조직·적용기간 편집 form으로 분리한다.
- 금지: dashboard card, KPI, export, bulk action, 파일 다운로드, 감사로그 화면은 원문에 없으므로 배치하지 않는다.

#### Navigation and State Flow

```text
[시스템 관리 > 사용자·조직 관리 > 조직 관리]
        |
        v
SCR-ORG-MGMT 진입
        |
        +-- loading: searchOrganizations와 getOrganizationTree 호출 중 skeleton row/트리 placeholder 표시
        |
        +-- permission: R09 권한이 없거나 보호 API가 403이면 "조직 관리 권한이 없습니다" 표시, 저장 CTA 비활성화
        |
        +-- error: 조회 실패 또는 ApiError 수신 시 오류 banner와 다시 조회 버튼 표시
        |
        +-- empty: organizationCodeFilter 결과가 없으면 목록에 "조회된 조직이 없습니다" 표시, tree와 편집 form은 선택 전 상태
        |
        +-- success: 목록/계층 표시 → 조직 선택 → 상위조직·적용기간 편집 → 저장 확인 → 저장 성공
                                      |                                      |
                                      |                                      +-- 성공: 선택 조직 상세를 갱신하고 tree를 재조회해 새 상위·하위 관계를 표시
                                      |                                      +-- 오류: field-level error를 effectiveStartDate/effectiveEndDate/parentOrganizationCode/changeReason 옆에 표시
                                      |
                                      +-- 취소: 변경 중 form 값을 마지막 조회 상태로 되돌리고 목록/계층 선택은 유지
```

#### Wireframes

##### SCR-ORG-MGMT

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar │ Header: 한국교원대학교 교수업적평가시스템 · R09 시스템관리자 · theme toggle                 │
├──────────────┼─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 시스템 관리  │ 시스템 관리 > 사용자·조직 관리 > 조직 관리                         [다시 조회]              │
│  사용자·조직 │ ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │
│   > 조직 관리│ │ 검색조건: 조직코드 [____________________]  조직유형 [대학/대학원/단과대학/학과/부서] [조회] │ │
│              │ └─────────────────────────────────────────────────────────────────────────────────────────┘ │
│              │ ┌───────────────────────────────┬───────────────────────────────┬─────────────────────────┐ │
│              │ │ 조직 목록                      │ 조직 계층                      │ 선택 조직 관계 편집        │ │
│              │ │ columns:                       │ ┌ 대학                         │ 조직코드: 1000 (readonly)   │ │
│              │ │ - 조직코드                     │ │ └ 단과대학                   │ 조직명: 사범대학 (readonly) │ │
│              │ │ - 조직명                       │ │   └ 학과                     │ 조직유형: COLLEGE (readonly)│ │
│              │ │ - 조직유형                     │ │     └ 부서                   │ 상위조직코드 [____찾기____] │ │
│              │ │ - 사용여부                     │ │ 선택 node 강조               │ 적용 시작일 [YYYY-MM-DD] *  │ │
│              │ │ - 상태                         │ │ empty: 계층 정보 없음        │ 적용 종료일 [YYYY-MM-DD]    │ │
│              │ │ row click: 상세/계층 선택       │ │ error: 계층 조회 실패        │ 변경 사유 [______________] * │ │
│              │ │ pagination: page/size           │ │ OQ-UI-031: 이력 조회 CTA 범위│ [상위관계 저장] [취소]      │ │
│              │ └───────────────────────────────┴───────────────────────────────┴─────────────────────────┘ │
│              │ ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │
│              │ │ 메시지 영역: 저장 성공 시 "조직 관계가 저장되었습니다"와 갱신된 상위조직/적용기간 표시       │ │
│              │ │ 오류 영역: effectiveEndDate < effectiveStartDate, 기간 중복, 권한 없음 field/server 오류 표시 │ │
│              │ └─────────────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────┴─────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Screen States

| 화면ID | state | browser-observable behavior | 관련 canonical_id |
|---|---|---|---|
| SCR-ORG-MGMT | loading | `/admin/organizations` 진입 직후 조직 목록 skeleton과 조직 계층 loading placeholder를 표시하고 `상위관계 저장` CTA를 비활성화한다. | REQ-080 |
| SCR-ORG-MGMT | empty | `organizationCodeFilter` 조회 결과가 없으면 조직 목록에 `조회된 조직이 없습니다`를 표시하고 선택 조직 관계 편집 form은 `조직을 선택하세요` 상태로 둔다. | REQ-019 |
| SCR-ORG-MGMT | error | `searchOrganizations`, `getOrganizationTree`, `saveOrganizationParentRelation` 중 하나가 ApiError를 반환하면 상단 오류 banner와 field-level 오류를 표시하며 기존 선택 조직 값은 유지한다. | REQ-021 |
| SCR-ORG-MGMT | permission | 인증 사용자가 R09 권한 또는 leaf menu 권한이 없으면 `조직 관리 권한이 없습니다` 상태를 표시하고 목록·tree·저장 CTA를 사용할 수 없게 한다. | REQ-080 |
| SCR-ORG-MGMT | success | 저장 성공 후 선택 조직의 상위조직코드, 적용 시작일, 적용 종료일을 갱신해 보여주고 조직 계층을 재조회하여 새 부모-자식 관계를 tree에 반영한다. | REQ-016, REQ-020, REQ-021 |

#### Per-Screen UI Contract




#### Data Binding

##### SCR-ORG-MGMT 조회·표시 binding

| UI region | field/parameter | source entity/table | OpenAPI operationId/path | behavior | canonical_id |
|---|---|---|---|---|---|
| 검색조건 | organizationCodeFilter | organizations.organization_code | searchOrganizations / GET /api/admin/organizations | 조직코드 기준 필터로 목록을 재조회한다. | REQ-019 |
| 검색조건 | organization_type | organizations.organization_type | searchOrganizations / GET /api/admin/organizations | 대학·대학원·단과대학·학과·부서 구분을 화면 필터로 제공한다. OpenAPI query parameter 명칭은 OQ-UI-032로 확정 필요하다. | REQ-016 |
| 조직 목록 | organization_code | organizations.organization_code | searchOrganizations / GET /api/admin/organizations | row 선택과 saveOrganizationParentRelation path parameter의 원천이다. | REQ-016 |
| 조직 목록 | organization_name | organizations.organization_name | searchOrganizations / GET /api/admin/organizations | 조직명 표시 데이터다. | REQ-019 |
| 조직 목록 | organization_type | organizations.organization_type | searchOrganizations / GET /api/admin/organizations | 대학·대학원·단과대학·학과·부서 구분을 표시한다. | REQ-016 |
| 조직 목록 | system_use_yn | organizations.system_use_yn | searchOrganizations / GET /api/admin/organizations | 조직 사용여부를 표시한다. | REQ-016 |
| 조직 목록 | status | organizations.status | searchOrganizations / GET /api/admin/organizations | ACTIVE/INACTIVE/DELETED 상태를 표시하되 상태 변경 CTA는 이 story 원문에 없으므로 제공하지 않는다. | REQ-016 |
| 조직 계층 | parent_organization_code relationship | organization_relations.parent_organization_code | getOrganizationTree / GET /api/admin/organizations/tree | 상위·하위 조직 관계를 tree node로 표시한다. | REQ-020 |
| 관계 편집 form | parentOrganizationCode | organization_relations.parent_organization_code | saveOrganizationParentRelation / PUT /api/admin/organizations/{organizationCode}/parent-relations | 선택 조직의 상위조직을 저장한다. | REQ-021 |
| 관계 편집 form | effectiveStartDate | organization_relations.effective_start_date | saveOrganizationParentRelation / PUT /api/admin/organizations/{organizationCode}/parent-relations | 적용 시작일을 저장하고 이력 보존 데이터의 근거가 된다. | REQ-021 |
| 관계 편집 form | effectiveEndDate | organization_relations.effective_end_date | saveOrganizationParentRelation / PUT /api/admin/organizations/{organizationCode}/parent-relations | 적용 종료일을 저장하며 null이면 현재 적용 상태로 표시한다. | REQ-021 |
| 관계 편집 form | changeReason | organization_relations.change_reason | saveOrganizationParentRelation / PUT /api/admin/organizations/{organizationCode}/parent-relations | 변경 사유를 저장 요청에 포함한다. | REQ-021 |

##### SCR-ORG-MGMT list behavior

| 화면ID | filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-ORG-MGMT | organizationCodeFilter; organization_type은 UI 표시 필요가 있으나 OpenAPI query parameter 확정은 OQ-UI-032 | organization_code 오름차순 | page/size query parameter 사용 | not applicable | not applicable | `조회된 조직이 없습니다`를 목록 영역에 표시하고 tree 선택 및 저장 form을 비활성화한다. |

#### Role/State Transition Matrix

| 화면ID | condition | UI behavior | server authorization/validation | canonical_id |
|---|---|---|---|---|
| SCR-ORG-MGMT | anonymous 또는 인증 세션 없음 | protected route 접근 시 로그인 필요 상태 또는 `/login` 이동 | 보호 API는 401 ApiError를 반환한다. | REQ-080 |
| SCR-ORG-MGMT | R09 권한 또는 leaf menu 권한 없음 | `조직 관리 권한이 없습니다` permission state와 저장 CTA 비활성화 | `searchOrganizations`, `getOrganizationTree`, `saveOrganizationParentRelation`는 403 ApiError를 반환한다. | REQ-080 |
| SCR-ORG-MGMT | 조직 row 선택 전 | 관계 편집 form은 readonly 안내 `조직을 선택하세요`를 표시한다. | saveOrganizationParentRelation path parameter가 없으므로 호출하지 않는다. | REQ-021 |
| SCR-ORG-MGMT | effectiveEndDate < effectiveStartDate | field-level error를 날짜 입력 옆에 표시하고 저장 성공 message를 표시하지 않는다. | saveOrganizationParentRelation는 400 ApiError를 반환하고 organization_relations 기존 row를 유지한다. | REQ-022 |
| SCR-ORG-MGMT | 동일 조직 상위관계 기간 중복 | 중복 기간 오류 banner와 field-level error를 표시한다. | saveOrganizationParentRelation는 업무 규칙 위반 ApiError를 반환하고 organization_relations 및 organization_relation_history를 추가하지 않는다. | REQ-023 |
| SCR-ORG-MGMT | 유효한 parentOrganizationCode/effectiveStartDate/effectiveEndDate/changeReason | 저장 확인 dialog 후 success message와 갱신된 tree를 표시한다. | saveOrganizationParentRelation가 organization_relations를 저장하고 organization_relation_history 보존 side effect를 만든다. | REQ-021 |



#### OQ-UI Items

| oq_id | 화면ID | missing source fact | prohibited inference | Code Agent behavior until resolved |
|---|---|---|---|---|
| OQ-UI-031 | SCR-ORG-MGMT | REQ-009가 요구한 조직 개편 시 상위조직 변경 이력 조회 화면/API 제공 범위와 보존 단위 | 임의로 이력 상세 화면, 다운로드, 감사로그 viewer, 별도 route를 만들지 않는다. | `SCR-ORG-MGMT` 안에는 이력 조회 미확정 안내만 표시하고, 구현 가능한 범위는 saveOrganizationParentRelation 성공 후 tree/선택 상세 재조회로 제한한다. |
| OQ-UI-032 | SCR-ORG-MGMT | organization_type UI filter의 OpenAPI query parameter 명칭 | `organizationType` 같은 새 query parameter를 확정된 계약처럼 사용하지 않는다. | UI는 조직유형 표시와 클라이언트 보조 필터 또는 비활성 filter로 처리하고, API 계약이 확정되기 전에는 `searchOrganizations`의 확정 query인 organizationCodeFilter, page, size만 필수 호출에 사용한다. |


## Feature — US-04

### UI Design Fragment F-04 — US-04 역할 관리

#### Design Reference 적용 메모

- 레퍼런스 `ex`는 mini-sidebar, header profile area, light/dark theme, compact table/form density, responsive content container의 방향성만 적용한다.
- 기능 계약의 화면ID, route, role, menu_path, primary_entity, operationId는 승인된 `ui-design.md` Screen Skeleton Ledger와 `contracts/openapi.yaml`을 따른다.
- 이 fragment는 US-04 `역할 관리`의 화면 상세만 작성하며 전역 route inventory, navigation map, common shell은 작성하지 않는다.



#### SCR-ROLE-MGMT Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar │ Header: 시스템 관리 / 역할·권한 관리 / 역할 관리   R09 관리자 │
├──────────────┴───────────────────────────────────────────────────────────────┤
│ 역할 관리                                                                    │
│ R01~R09 기준 역할의 목적을 조회하고 역할 기준정보를 관리한다.                 │
│                                                                              │
│ 검색조건                                                                      │
│ ┌──────────────────────────────────────────────────────────────────────────┐ │
│ │ OQ-UI-041: 역할 검색조건 원문 미정                                       │ │
│ │ [조회] [초기화]                                                          │ │
│ └──────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│ 역할 목록                                                    page 1 / size 10 │
│ ┌──────────┬───────────────────────┬──────────────────────────────────────┐ │
│ │ 역할코드 │ 역할명                │ 역할별 목적                          │ │
│ ├──────────┼───────────────────────┼──────────────────────────────────────┤ │
│ │ R01      │ 교원                  │ 본인 관련 업무 수행                  │ │
│ │ R02      │ 학과장                │ 소속 학과 교원 관련 업무 확인        │ │
│ │ R03      │ 단과대학(원) 행정실   │ 단과대학 또는 대학원 행정 처리       │ │
│ │ R04      │ 교수지원과            │ 기준정보와 평가 관련 행정 관리       │ │
│ │ R05      │ 산학협력단            │ 연구비·간접비·지식재산 자료 관리    │ │
│ │ R06      │ 입학인재관리과        │ 입학·취업률 관련 자료 관리           │ │
│ │ R07      │ 실적부서              │ 담당 실적 자료 관리                  │ │
│ │ R08      │ 점수산출 감사자       │ 산출 과정과 근거 조회                │ │
│ │ R09      │ 시스템관리자          │ 사용자·조직·메뉴·권한·코드 관리     │ │
│ └──────────┴───────────────────────┴──────────────────────────────────────┘ │
│                                                                              │
│ 선택 역할 상세/편집                                                           │
│ ┌──────────────────────────────────────────────────────────────────────────┐ │
│ │ 역할코드        [R09] readonly; roleCode 변경 금지                       │ │
│ │ 역할명          [시스템관리자____________________________________]       │ │
│ │ 목적            [사용자·조직·메뉴·권한·코드 관리________________]       │ │
│ │ 부여 기준       [시스템 관리 담당자______________________________]       │ │
│ │ 데이터 범위 기본값 [전체________________________________________]       │ │
│ │ 변경 사유       [역할 기준정보 정비_____________________________]       │ │
│ │                                                                          │ │
│ │ [저장] → 확인 modal: 선택 역할 R09의 기준정보를 저장하시겠습니까?        │ │
│ │ [취소] → 선택 전 상세값으로 되돌림                                      │ │
│ │                                                                          │ │
│ │ success: 저장 후 목록 재조회, 선택 역할 상세에 최신 역할명·목적 표시    │ │
│ │ error: ApiError.fields를 필드 하단에 표시하고 기존 상세값 유지           │ │
│ └──────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Screen States

| 화면ID | state | trigger | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|
| SCR-ROLE-MGMT | loading | `/admin/roles` 진입 또는 [조회] 클릭 후 listRoles 응답 대기 | 역할 목록 영역에 skeleton row와 `역할 목록을 불러오는 중입니다` 메시지를 표시하고 저장 버튼은 비활성화한다. | listRoles | REQ-026 |
| SCR-ROLE-MGMT | empty | listRoles가 빈 목록을 반환 | 목록 table 아래에 `표시할 역할이 없습니다`를 표시하되, R01~R09 seed 누락은 REQ-024 seed/test fixture 결함으로 기록한다. | listRoles | REQ-026 |
| SCR-ROLE-MGMT | error | listRoles 또는 updateRole이 ApiError를 반환 | 목록/폼 상단에 ApiError.message를 표시하고, updateRole 400이면 ApiError.fields를 `roleName`, `purpose`, `assignmentCriteria`, `defaultDataScope`, `changeReason` 필드 아래에 연결한다. | listRoles / updateRole | REQ-081 |
| SCR-ROLE-MGMT | permission | 인증되었으나 R09 권한이 없거나 서버가 403을 반환 | `역할 관리 권한이 없습니다` 상태를 표시하고 목록·상세·저장 CTA를 렌더링하지 않는다. | listRoles / updateRole | REQ-081 |
| SCR-ROLE-MGMT | success | listRoles 200 또는 updateRole 200 | R01~R09 역할 목록과 역할별 목적을 표시하고, 저장 성공 시 목록을 재조회해 선택 역할 상세에 갱신된 값과 `저장되었습니다` 메시지를 표시한다. | listRoles / updateRole | REQ-026, REQ-081 |

#### Per-Screen UI Contract




#### Data Binding

##### Role List Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-ROLE-MGMT | 역할 목록 column | 역할코드 | listRoles data[].roleCode | roles.role_code | REQ-026 | R01~R09 고정 코드이며 row identity로 사용한다. |
| SCR-ROLE-MGMT | 역할 목록 column | 역할명 | listRoles data[].roleName | roles.role_name | REQ-026 | 역할명 변경 후에도 roleCode는 유지된다. |
| SCR-ROLE-MGMT | 역할 목록 column | 역할별 목적 | listRoles data[].purpose | roles.purpose | REQ-026 | 목록 조회의 핵심 표시값이다. |

##### Role Detail/Edit Binding

| 화면ID | UI field | editable | API source/payload | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-ROLE-MGMT | 역할코드 | no | updateRole path `roleCode`; payload에 roleCode 입력 금지 | roles.role_code | REQ-028 | 역할명 변경 후에도 역할코드를 유지해야 하므로 readonly로만 표시한다. |
| SCR-ROLE-MGMT | 역할명 | yes | RoleUpdateRequest.roleName | roles.role_name | REQ-028 | 역할명은 표시명으로 관리하며 roleCode를 대체하지 않는다. |
| SCR-ROLE-MGMT | 목적 | yes | RoleUpdateRequest.purpose | roles.purpose | REQ-025 | 역할별 목적 관리 데이터다. |
| SCR-ROLE-MGMT | 부여 기준 | yes | RoleUpdateRequest.assignmentCriteria | roles.assignment_criteria | REQ-027 | 부여 기준 등록·변경 입력이다. |
| SCR-ROLE-MGMT | 데이터 범위 기본값 | yes | RoleUpdateRequest.defaultDataScope | roles.default_data_scope | REQ-027 | 데이터 범위 기본값 등록·변경 입력이다. |
| SCR-ROLE-MGMT | 변경 사유 | yes | RoleUpdateRequest.changeReason | roles.change_reason | REQ-065 | 변경 추적 공통 규칙을 위한 사유 입력이며 상세 감사로그 화면을 만들지 않는다. |

#### List Behavior

| 화면ID | filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-ROLE-MGMT | OQ-UI-041: 역할 검색조건 영역은 공통 화면 구조에 필요하지만 원문에는 역할 전용 검색 필드가 없다. 확정 전 임의 roleName/roleCode filter를 만들지 않는다. | role_code 오름차순(R01 → R09) 표시를 기본으로 한다. | listRoles의 page, size query parameter를 사용한다. | not applicable — 원문에 역할 bulk action 없음. | not applicable — 파일/Excel 기능은 제외 범위이며 역할 export 요구가 없다. | `표시할 역할이 없습니다`를 표시하고 저장 CTA는 선택 역할이 없으므로 비활성화한다. |

#### Role/State Transition Matrix

| 화면ID | UI state/action | server authorization | allowed UI behavior | blocked UI behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|---|
| SCR-ROLE-MGMT | R09 진입 | SessionCookie + R09 | listRoles 호출, 역할 목록 표시, row 선택, updateRole 저장 가능 | 신규 역할코드 추가 CTA 없음 | listRoles / updateRole | REQ-026, REQ-081 |
| SCR-ROLE-MGMT | 비인증 진입 | SessionCookie 없음 | protected route guard가 인증 필요 흐름으로 보낸다. | 목록·상세·저장 CTA 렌더링 금지 | listRoles | REQ-081 |
| SCR-ROLE-MGMT | R09 외 인증 사용자 진입 | 403 또는 menu permission deny | permission 상태만 표시한다. | 목록·상세·저장 CTA 렌더링 금지 | listRoles / updateRole | REQ-081 |
| SCR-ROLE-MGMT | 역할 row 선택 후 편집 | SessionCookie + R09 | roleName, purpose, assignmentCriteria, defaultDataScope, changeReason 편집 가능 | roleCode 수정 및 R01~R09 외 신규 roleCode 생성 금지 | updateRole | REQ-081 |
| SCR-ROLE-MGMT | updateRole 성공 | SessionCookie + R09 | 목록 재조회, 상세 패널 최신 값 반영, success message 표시 | 저장 성공 전 optimistic 확정 표시 금지 | updateRole / listRoles | REQ-081 |
| SCR-ROLE-MGMT | updateRole 검증 실패 | SessionCookie + R09 + server validation | ApiError.fields를 필드별 표시하고 편집값 수정 가능 | persisted 성공 message 표시 금지 | updateRole | REQ-081 |

#### Navigation and Sequence Flow

```text
entry
  └─ 시스템 관리 > 역할·권한 관리 > 역할 관리 클릭
      ├─ no session → /login 또는 인증 필요 상태
      ├─ no R09 permission → SCR-ROLE-MGMT permission state
      └─ R09 session → listRoles loading
           ├─ 200 with rows → role list success → row select → detail/edit panel
           │     ├─ [취소] → selected row values restored, stay /admin/roles
           │     └─ [저장] → confirmation modal
           │           ├─ confirm → updateRole
           │           │     ├─ 200 → success message → listRoles refresh → stay /admin/roles
           │           │     ├─ 400 → field errors shown → stay in edit panel
           │           │     └─ 403 → permission state
           │           └─ cancel modal → return to edit panel without API call
           ├─ 200 empty → empty state, save disabled
           └─ 4xx/5xx → error banner with retry [조회]
```



#### Code Agent Handoff

| 화면ID | route | ownership | read actions | mutation/local actions | required states/tests | OQ |
|---|---|---|---|---|---|---|
| SCR-ROLE-MGMT | /admin/roles | US-04 역할 관리 vertical slice; frontend file target `frontend/src/pages/admin/SCR-ROLE-MGMT.tsx`, test target `frontend/src/pages/admin/SCR-ROLE-MGMT.test.tsx` | listRoles GET `/api/admin/roles` | updateRole PUT `/api/admin/roles/{roleCode}`; local row select, reset, cancel; 신규 roleCode create 없음 | loading, empty, error, permission, success; T035 screen state test와 T034 API contract test가 먼저 실패해야 한다. | OQ-UI-041: 역할 검색조건 원문 미정. |


## Feature — US-05

### UI Design Fragment F-05 — US-05 사용자 역할 관리

#### Design Reference 적용 메모

- 레퍼런스 `ex`는 mini-sidebar, header profile area, light/dark theme, compact table/form density, responsive content container의 방향성만 적용한다.
- 기능 계약의 화면ID, route, role, menu_path, primary_entity, operationId는 승인된 `ui-design.md` Screen Skeleton Ledger와 `contracts/openapi.yaml`을 따른다.
- 이 fragment는 US-05 `사용자 역할 관리`의 화면 상세만 작성하며 전역 route inventory, navigation map, common shell은 작성하지 않는다.
- `spec-wireframe-design` skill은 현재 profile에서 발견되지 않아 로드할 수 없었다. 대신 승인된 `spec-design-artifacts`/`spec-ui-ascii-design` 계약에 따라 source-backed layout만 사용하고, 미확정 UI 세부는 OQ-UI-051로 보존한다.



#### SCR-USER-ROLE-MGMT Wireframe

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar │ Header: 시스템 관리 / 역할·권한 관리 / 사용자 역할 관리   R09 관리자 │
├──────────────┴─────────────────────────────────────────────────────────────────────┤
│ 사용자 역할 관리                                                                    │
│ 사용자별 현재 역할과 유효기간을 확인하고 역할을 부여·변경·회수한다.                 │
│                                                                                    │
│ 검색조건                                                                            │
│ ┌────────────────────────────────────────────────────────────────────────────────┐ │
│ │ 역할코드 [전체 ▾ R01 R02 R03 R04 R05 R06 R07 R08 R09]                         │ │
│ │ OQ-UI-051: 사용자 식별/검색 입력 방식 미정 — userId 직접 입력 또는 사용자 선택  │ │
│ │ [조회] [초기화]                                                                │ │
│ └────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                    │
│ 사용자 역할 목록                                                page 1 / size 10    │
│ ┌───────────────┬─────────┬──────────┬──────────────┬──────────────┬──────────┐   │
│ │ assignment_id │ user_id │ role_code│ assignment_type│ 유효 시작일 │ 유효 종료일│   │
│ ├───────────────┼─────────┼──────────┼──────────────┼──────────────┼──────────┤   │
│ │ 1001          │ 1       │ R09      │ MANUAL       │ 2026-01-01   │           │   │
│ │ 1002          │ 27      │ R02      │ POSITION     │ 2026-03-01   │ 2027-02-28│   │
│ └───────────────┴─────────┴──────────┴──────────────┴──────────────┴──────────┘   │
│                                                                                    │
│ 선택 사용자 현재 역할                                                               │
│ ┌────────────────────────────────────────────────────────────────────────────────┐ │
│ │ 선택 user_id: [1____________________] [현재 역할 조회]                         │ │
│ │ 현재 역할: R09 시스템관리자 / MANUAL / 2026-01-01 ~ 없음 / ACTIVE             │ │
│ │ 보직 기반 역할은 assignment_type=POSITION badge, 수동 부여 역할은 MANUAL badge │ │
│ └────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                    │
│ 역할 부여·변경 폼                                                                  │
│ ┌────────────────────────────────────────────────────────────────────────────────┐ │
│ │ assignment_id [1001] readonly(변경/회수 시 path parameter)                     │ │
│ │ 대상 user_id   [1____________________]                                        │ │
│ │ 역할코드       [R09 ▾ R01 R02 R03 R04 R05 R06 R07 R08 R09]                    │ │
│ │ 부여 구분      [MANUAL] readonly for manual assignment; POSITION은 조회 구분용 │ │
│ │ 유효 시작일    [2026-01-01]                                                   │ │
│ │ 유효 종료일    [____________]                                                 │ │
│ │ 변경 사유      [사용자 역할 운영 변경_______________________________]          │ │
│ │ 승인자/처리자  [로그인 R09 관리자 자동 기록] readonly                          │ │
│ │                                                                                │ │
│ │ [역할 부여] → 확인: 대상 user_id에 선택 역할을 부여하시겠습니까?              │ │
│ │ [변경 저장] → 확인: assignment_id의 역할/유효기간을 변경하시겠습니까?          │ │
│ │ [역할 회수] → 위험 확인: 선택 역할을 회수 처리하시겠습니까?                   │ │
│ │ [취소] → 마지막 조회/선택값으로 복원                                           │ │
│ │                                                                                │ │
│ │ success: 저장/회수 후 목록과 현재 역할을 재조회하고 최신 status를 표시         │ │
│ │ error: ApiError.fields를 userId, roleCode, validStartDate, validEndDate,       │ │
│ │        changeReason 아래에 표시하고 기존 persisted 값은 유지                  │ │
│ └────────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────┘
```

#### Screen States

| 화면ID | state | trigger | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|
| SCR-USER-ROLE-MGMT | loading | `/admin/user-roles` 진입, [조회], [현재 역할 조회], [역할 부여], [변경 저장], [역할 회수] 후 응답 대기 | 목록 또는 현재 역할 영역에 skeleton row와 `사용자 역할 정보를 불러오는 중입니다` 메시지를 표시하고 부여·변경·회수 CTA를 비활성화한다. | listUserRoleAssignments / listCurrentUserRoles / assignUserRole / updateUserRole / revokeUserRole | REQ-034, REQ-082 |
| SCR-USER-ROLE-MGMT | empty | listUserRoleAssignments 또는 listCurrentUserRoles가 빈 목록을 반환 | 목록 table 아래에 `표시할 사용자 역할이 없습니다`를 표시하고, 선택 user_id의 현재 역할이 없으면 `현재 부여된 역할이 없습니다`를 표시한다. | listUserRoleAssignments / listCurrentUserRoles | REQ-034 |
| SCR-USER-ROLE-MGMT | error | 조회·부여·변경·회수 operation이 ApiError를 반환 | 화면 상단에 ApiError.message를 표시하고, 400이면 ApiError.fields를 userId, roleCode, validStartDate, validEndDate, changeReason 필드 하단에 연결한다. | listUserRoleAssignments / listCurrentUserRoles / assignUserRole / updateUserRole / revokeUserRole | REQ-082 |
| SCR-USER-ROLE-MGMT | permission | 인증되었으나 R09 권한이 없거나 서버가 403을 반환 | `사용자 역할 관리 권한이 없습니다` 상태를 표시하고 목록·현재 역할·부여/변경/회수 폼과 CTA를 렌더링하지 않는다. | listUserRoleAssignments / assignUserRole / updateUserRole / revokeUserRole | REQ-082 |
| SCR-USER-ROLE-MGMT | success | 조회 200, assignUserRole 200, updateUserRole 200, revokeUserRole 200 | 사용자 역할 목록에 role_code, assignment_type, 유효기간, status가 표시되고, 저장/회수 성공 시 `처리되었습니다` 메시지와 함께 목록 및 선택 user_id 현재 역할이 재조회된다. | listUserRoleAssignments / listCurrentUserRoles / assignUserRole / updateUserRole / revokeUserRole | REQ-030, REQ-031, REQ-032, REQ-033, REQ-034, REQ-082 |

#### Per-Screen UI Contract




#### Data Binding

##### User Role List Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-USER-ROLE-MGMT | 사용자 역할 목록 column | assignment_id | listUserRoleAssignments data[].assignmentId | user_roles.assignment_id | REQ-030 | 변경·회수 대상 row identity이며 updateUserRole/revokeUserRole path parameter로 사용한다. |
| SCR-USER-ROLE-MGMT | 사용자 역할 목록 column | user_id | listUserRoleAssignments data[].userId | user_roles.user_id | REQ-034 | 사용자별 현재 역할 조회 기준이다. |
| SCR-USER-ROLE-MGMT | 사용자 역할 목록 column | role_code | listUserRoleAssignments data[].roleCode | user_roles.role_code | REQ-030 | R01~R09 역할 참조값이다. |
| SCR-USER-ROLE-MGMT | 사용자 역할 목록 column | assignment_type | listUserRoleAssignments data[].assignmentType | user_roles.assignment_type | REQ-033 | POSITION/MANUAL badge로 보직 기반 역할과 수동 역할을 구분한다. |
| SCR-USER-ROLE-MGMT | 사용자 역할 목록 column | valid_start_date | listUserRoleAssignments data[].validStartDate | user_roles.valid_start_date | REQ-030 | 역할별 유효기간 시작일이다. |
| SCR-USER-ROLE-MGMT | 사용자 역할 목록 column | valid_end_date | listUserRoleAssignments data[].validEndDate | user_roles.valid_end_date | REQ-031 | 역할별 유효기간 종료일이며 변경 저장 대상이다. |
| SCR-USER-ROLE-MGMT | 사용자 역할 목록 column | status | listUserRoleAssignments data[].status | user_roles.status | REQ-032 | 회수 처리 후 REVOKED 상태를 사용자에게 구분 표시한다. |
| SCR-USER-ROLE-MGMT | 사용자 역할 목록/상세 표시 | approver_user_id | listUserRoleAssignments data[].approverUserId | user_roles.approver_user_id | REQ-035 | data-contract 의존 표시값이며 fragment accounting에는 별도 row를 쓰지 않는다. |

##### User Role Form Binding

| 화면ID | UI field | editable | API source/payload | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-USER-ROLE-MGMT | assignment_id | no | updateUserRole/revokeUserRole path `assignmentId` | user_roles.assignment_id | REQ-031 | 선택 row의 변경·회수 identity로만 사용한다. |
| SCR-USER-ROLE-MGMT | 대상 user_id | yes | UserRoleAssignmentRequest.userId 또는 listCurrentUserRoles path `userId` | user_roles.user_id | REQ-034 | OQ-UI-051 확정 전 사용자 검색/선택 UI는 임의 확장하지 않는다. |
| SCR-USER-ROLE-MGMT | 역할코드 | yes | UserRoleAssignmentRequest.roleCode | user_roles.role_code | REQ-030 | R01~R09 선택값으로 역할을 부여한다. |
| SCR-USER-ROLE-MGMT | 부여 구분 | no | response assignmentType | user_roles.assignment_type | REQ-033 | POSITION/MANUAL 구분 표시용이며 수동 부여 폼에서 POSITION 생성 정책은 확정하지 않는다. |
| SCR-USER-ROLE-MGMT | 유효 시작일 | yes | UserRoleAssignmentRequest.validStartDate | user_roles.valid_start_date | REQ-030 | 역할별 유효기간 기록 대상이다. |
| SCR-USER-ROLE-MGMT | 유효 종료일 | yes | UserRoleAssignmentRequest.validEndDate | user_roles.valid_end_date | REQ-031 | 변경 저장으로 갱신 가능한 유효기간 종료일이다. |
| SCR-USER-ROLE-MGMT | 변경 사유 | yes | UserRoleAssignmentRequest.changeReason 또는 RevokeUserRoleRequest.changeReason | user_roles.change_reason | REQ-032 | 회수/변경 처리 정보와 변경 추적 공통 규칙의 사유다. |
| SCR-USER-ROLE-MGMT | 승인자/처리자 | no | authenticated admin -> approverUserId | user_roles.approver_user_id | REQ-035 | 별도 승인자 입력값을 만들지 않고 로그인 관리자가 자동 기록된다는 안내를 표시한다. |

#### List Behavior

| 화면ID | filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-USER-ROLE-MGMT | roleCodeFilter는 listUserRoleAssignments query parameter로 제공한다. 사용자 식별/검색 입력 방식은 OQ-UI-051이며, 확정 전 임의 성명/교번 filter를 만들지 않는다. | assignment_id 내림차순 또는 API 기본 정렬을 사용한다. 원문에 별도 정렬 기준이 없으므로 화면은 정렬 변경 CTA를 제공하지 않는다. | listUserRoleAssignments의 page, size query parameter를 사용한다. | not applicable — 원문에 사용자 역할 bulk action 없음. | not applicable — 파일/Excel 기능은 제외 범위이며 사용자 역할 export 요구가 없다. | `표시할 사용자 역할이 없습니다`와 `현재 부여된 역할이 없습니다`를 구분 표시하고, 변경·회수 CTA는 선택 assignment_id가 없으므로 비활성화한다. |

#### Role/State Transition Matrix

| 화면ID | UI state/action | server authorization | allowed UI behavior | blocked UI behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|---|
| SCR-USER-ROLE-MGMT | R09 진입 | SessionCookie + R09 | listUserRoleAssignments 호출, 목록 표시, row 선택, 현재 역할 조회, 부여·변경·회수 가능 | 승인자 직접 입력, bulk action, export CTA 제공 금지 | listUserRoleAssignments / listCurrentUserRoles / assignUserRole / updateUserRole / revokeUserRole | REQ-030, REQ-031, REQ-032, REQ-034, REQ-082 |
| SCR-USER-ROLE-MGMT | 비인증 진입 | SessionCookie 없음 | protected route guard가 인증 필요 흐름으로 보낸다. | 목록·현재 역할·부여/변경/회수 CTA 렌더링 금지 | listUserRoleAssignments | REQ-082 |
| SCR-USER-ROLE-MGMT | R09 외 인증 사용자 진입 | 403 또는 menu permission deny | permission 상태만 표시한다. | 목록·폼·저장/회수 CTA 렌더링 금지 | listUserRoleAssignments / assignUserRole / updateUserRole / revokeUserRole | REQ-082 |
| SCR-USER-ROLE-MGMT | 현재 역할 조회 성공 | SessionCookie + R09 | 선택 user_id의 role_code, assignment_type, valid_start_date, valid_end_date, status 표시 | POSITION/MANUAL 구분 누락 금지 | listCurrentUserRoles | REQ-033, REQ-034 |
| SCR-USER-ROLE-MGMT | 역할 부여 성공 | SessionCookie + R09 | user_roles none -> ACTIVE 결과를 목록과 현재 역할 재조회로 표시한다. | 승인자 직접 입력값으로 approverUserId 덮어쓰기 금지 | assignUserRole / listCurrentUserRoles | REQ-030 |
| SCR-USER-ROLE-MGMT | 역할 변경 성공 | SessionCookie + R09 | 선택 assignment_id의 role_code 또는 유효기간 변경을 재조회 결과로 표시한다. | path assignmentId 없이 updateUserRole 호출 금지 | updateUserRole / listCurrentUserRoles | REQ-031 |
| SCR-USER-ROLE-MGMT | 역할 회수 성공 | SessionCookie + R09 | user_roles ACTIVE -> REVOKED 상태 또는 현재 역할 목록 제외를 표시한다. | 회수 성공 전 row를 제거하거나 성공 message를 표시하지 않는다. | revokeUserRole / listCurrentUserRoles | REQ-032 |
| SCR-USER-ROLE-MGMT | 검증 실패 | SessionCookie + R09 + server validation | ApiError.fields를 필드별 표시하고 편집값 수정 가능 | persisted 성공 message 표시 금지 | assignUserRole / updateUserRole / revokeUserRole | REQ-082 |

#### Navigation and Sequence Flow

```text
entry
  └─ 시스템 관리 > 역할·권한 관리 > 사용자 역할 관리 클릭
      ├─ no session → /login 또는 인증 필요 상태
      ├─ no R09 permission → SCR-USER-ROLE-MGMT permission state
      └─ R09 session → listUserRoleAssignments loading
           ├─ 200 with rows → user role list success → row select → assignment edit panel
           │     ├─ [현재 역할 조회] → listCurrentUserRoles
           │     │     ├─ 200 with current roles → POSITION/MANUAL badge와 유효기간 표시
           │     │     └─ 200 empty → 현재 부여된 역할 없음 표시
           │     ├─ [역할 부여] → confirmation modal
           │     │     ├─ confirm → assignUserRole
           │     │     │     ├─ 200 → 처리되었습니다 → listUserRoleAssignments/listCurrentUserRoles refresh → stay /admin/user-roles
           │     │     │     ├─ 400 → field errors shown → stay in form
           │     │     │     └─ 403 → permission state
           │     │     └─ cancel modal → return to form without API call
           │     ├─ [변경 저장] → confirmation modal → updateUserRole
           │     │     ├─ 200 → 처리되었습니다 → refreshed changed assignment visible
           │     │     └─ 400/403 → field error or permission state
           │     ├─ [역할 회수] → destructive confirmation modal → revokeUserRole
           │     │     ├─ 200 → 회수 처리되었습니다 → REVOKED or removed from current roles
           │     │     └─ 400/403 → no persisted success 표시
           │     └─ [취소] → selected row values restored, stay /admin/user-roles
           ├─ 200 empty → empty state, update/revoke disabled
           └─ 4xx/5xx → error banner with retry [조회]
```



#### Code Agent Handoff

| 화면ID | route | ownership | read actions | mutation/local actions | required states/tests | OQ |
|---|---|---|---|---|---|---|
| SCR-USER-ROLE-MGMT | /admin/user-roles | US-05 사용자 역할 관리 vertical slice; frontend file target `frontend/src/pages/admin/SCR-USER-ROLE-MGMT.tsx`, test target `frontend/src/pages/admin/SCR-USER-ROLE-MGMT.test.tsx` | listUserRoleAssignments GET `/api/admin/user-roles`; listCurrentUserRoles GET `/api/admin/users/{userId}/roles` | assignUserRole POST `/api/admin/user-roles`; updateUserRole PUT `/api/admin/user-roles/{assignmentId}`; revokeUserRole DELETE `/api/admin/user-roles/{assignmentId}`; local row select, reset, cancel, confirmation modal | loading, empty, error, permission, success; T040 screen state test와 T039 API contract test가 먼저 실패해야 한다. | OQ-UI-051: 사용자 역할 관리에서 user_id를 직접 입력할지 사용자 검색/선택 component로 고를지 source UI 세부가 미정이다. |


## Feature — US-06

### UI Design Fragment F-06 — US-06 메뉴 권한 관리

#### Design Reference 적용 메모

- 레퍼런스 `ex`는 mini-sidebar, header profile area, light/dark theme, compact table/form density, responsive content container의 방향성만 적용한다.
- 기능 계약의 화면ID, route, role, menu_path, primary_entity, operationId는 승인된 `ui-design.md` Screen Skeleton Ledger와 `contracts/openapi.yaml`을 따른다.
- 이 fragment는 US-06 `메뉴 권한 관리`의 화면 상세만 작성하며 전역 route inventory, navigation map, common shell은 작성하지 않는다.



#### SCR-MENU-PERMISSION-MGMT Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar │ Header: 시스템 관리 / 역할·권한 관리 / 메뉴 권한 관리 R09 관리자 │
├──────────────┴───────────────────────────────────────────────────────────────┤
│ 메뉴 권한 관리                                                               │
│ 역할·조직·사용자 단위로 대메뉴·중메뉴·화면 접근 여부를 조회하고 저장한다.    │
│                                                                              │
│ 검색조건                                                                      │
│ ┌──────────────────────────────────────────────────────────────────────────┐ │
│ │ 대상 유형 [ROLE ▼]  대상 식별자 [R09________________]                   │ │
│ │ accessAllowed [전체 ▼]                                                   │ │
│ │ [조회] [초기화]                                                          │ │
│ │ hint: 대상 유형 enum은 ROLE / ORGANIZATION / USER                        │ │
│ └──────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│ 접근권한 matrix                                           page 1 / size 10   │
│ ┌───────────┬──────────────┬──────────────┬────────────┬───────────────┐   │
│ │ 대메뉴    │ 중메뉴       │ 화면         │ accessAllowed │ 변경 상태   │   │
│ ├───────────┼──────────────┼──────────────┼────────────┼───────────────┤   │
│ │ 시스템 관리│ 역할·권한 관리│ 역할 관리     │ [ALLOW ▼] │ 저장됨        │   │
│ │ 시스템 관리│ 역할·권한 관리│ 사용자 역할 관리│ [ALLOW ▼] │ 저장됨      │   │
│ │ 시스템 관리│ 역할·권한 관리│ 메뉴 권한 관리 │ [ALLOW ▼] │ 저장됨       │   │
│ │ 시스템 관리│ 사용자·조직 관리│ 사용자 관리 │ [DENY ▼]  │ 편집 중       │   │
│ └───────────┴──────────────┴──────────────┴────────────┴───────────────┘   │
│                                                                              │
│ 선택 권한 상세/저장                                                           │
│ ┌──────────────────────────────────────────────────────────────────────────┐ │
│ │ targetType        [ROLE] readonly from 검색조건                          │ │
│ │ targetId          [R09] readonly from 검색조건                            │ │
│ │ menuId            [사용자 관리 menu_id] readonly from selected row        │ │
│ │ accessAllowed     [DENY ▼] enum ALLOW/DENY                                │ │
│ │ changeReason      [사용자 관리 메뉴 접근 제한_____________________]      │ │
│ │                                                                          │ │
│ │ [저장] → 확인 modal: ROLE R09의 사용자 관리 접근권한을 DENY로 저장합니까? │ │
│ │ [취소] → 선택 row의 마지막 조회값으로 복원                                │ │
│ │                                                                          │ │
│ │ success: 저장 후 listMenuPermissions 재조회, matrix에 최신 ALLOW/DENY 표시 │ │
│ │ error: ApiError.fields를 targetType, targetId, menuId, accessAllowed,      │ │
│ │        changeReason 필드 하단에 표시하고 기존 저장값은 유지               │ │
│ └──────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Screen States

| 화면ID | state | trigger | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|
| SCR-MENU-PERMISSION-MGMT | loading | `/admin/menu-permissions` 진입 또는 [조회] 클릭 후 listMenuPermissions 응답 대기 | matrix 영역에 skeleton row와 `메뉴 권한을 불러오는 중입니다` 메시지를 표시하고 [저장]은 비활성화한다. | listMenuPermissions | REQ-038 |
| SCR-MENU-PERMISSION-MGMT | empty | listMenuPermissions가 빈 목록을 반환 | matrix 아래에 `조회된 메뉴 권한이 없습니다`를 표시하고, targetType/targetId 검색조건과 [조회]는 유지한다. | listMenuPermissions | REQ-038, REQ-039, REQ-040 |
| SCR-MENU-PERMISSION-MGMT | error | listMenuPermissions 또는 saveMenuPermissions가 ApiError를 반환 | 화면 상단에 ApiError.message를 표시하고, saveMenuPermissions 400이면 ApiError.fields를 `targetType`, `targetId`, `menuId`, `accessAllowed`, `changeReason` 입력 아래에 연결한다. | listMenuPermissions / saveMenuPermissions | REQ-083 |
| SCR-MENU-PERMISSION-MGMT | permission | 인증되었으나 R09 권한이 없거나 서버가 403을 반환 | `메뉴 권한 관리 권한이 없습니다` 상태를 표시하고 검색조건·matrix·저장 CTA를 렌더링하지 않는다. | listMenuPermissions / saveMenuPermissions | REQ-083 |
| SCR-MENU-PERMISSION-MGMT | success | listMenuPermissions 200 또는 saveMenuPermissions 200 | 대상 유형과 대상 식별자에 맞는 대메뉴·중메뉴·화면 accessAllowed matrix를 표시하고, 저장 성공 시 `저장되었습니다` 메시지와 최신 ALLOW/DENY 값을 표시한다. | listMenuPermissions / saveMenuPermissions | REQ-036, REQ-038, REQ-039, REQ-040, REQ-083 |

#### Per-Screen UI Contract




#### Data Binding

##### Menu Permission Matrix Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-MENU-PERMISSION-MGMT | 검색조건 | 대상 유형 | listMenuPermissions query `targetType`; saveMenuPermissions payload `targetType` | menu_permissions.target_type | REQ-038, REQ-039, REQ-040 | enum ROLE/ORGANIZATION/USER이며 조회 범위를 결정한다. |
| SCR-MENU-PERMISSION-MGMT | 검색조건 | 대상 식별자 | listMenuPermissions query `targetId`; saveMenuPermissions payload `targetId` | menu_permissions.target_id | REQ-038, REQ-039, REQ-040 | targetType에 따라 role_code, organization_code, user_id 중 하나를 담는 식별자다. |
| SCR-MENU-PERMISSION-MGMT | matrix column | 대메뉴 | listMenuPermissions data[].topMenuName | menus.menu_name | REQ-038 | 대메뉴 접근권한 표시 단위다. |
| SCR-MENU-PERMISSION-MGMT | matrix column | 중메뉴 | listMenuPermissions data[].middleMenuName | menus.menu_name | REQ-038 | 중메뉴 접근권한 표시 단위다. |
| SCR-MENU-PERMISSION-MGMT | matrix column | 화면 | listMenuPermissions data[].screenMenuName / data[].screenId | menus.menu_name / menus.screen_id | REQ-038 | 화면 접근권한 표시 단위이며 route/API 보호 기준과 연결된다. |
| SCR-MENU-PERMISSION-MGMT | matrix edit cell | accessAllowed | listMenuPermissions data[].accessAllowed; saveMenuPermissions payload `accessAllowed` | menu_permissions.access_allowed | REQ-036 | enum ALLOW/DENY이며 명시적 DENY는 허용보다 우선한다는 API 계약을 따른다. |

##### Menu Permission Detail/Save Binding

| 화면ID | UI field | editable | API source/payload | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-MENU-PERMISSION-MGMT | targetType | yes in search, readonly after row select | MenuPermissionSaveRequest.targetType | menu_permissions.target_type | REQ-036 | ROLE/ORGANIZATION/USER 중 하나다. |
| SCR-MENU-PERMISSION-MGMT | targetId | yes in search, readonly after row select | MenuPermissionSaveRequest.targetId | menu_permissions.target_id | REQ-036 | role_code, organization_code, user_id 중 대상 식별자다. |
| SCR-MENU-PERMISSION-MGMT | menuId | no | selected row menuId; MenuPermissionSaveRequest.menuId | menu_permissions.menu_id | REQ-036 | matrix row identity이며 사용자가 임의 숫자를 입력하지 않는다. |
| SCR-MENU-PERMISSION-MGMT | accessAllowed | yes | MenuPermissionSaveRequest.accessAllowed | menu_permissions.access_allowed | REQ-036 | ALLOW 또는 DENY만 저장한다. |
| SCR-MENU-PERMISSION-MGMT | changeReason | yes | MenuPermissionSaveRequest.changeReason | menu_permissions.change_reason | REQ-036 | 변경 추적 공통 규칙을 위한 사유 입력이며 별도 감사로그 화면을 만들지 않는다. |

#### List Behavior

| 화면ID | filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-MENU-PERMISSION-MGMT | targetType(ROLE/ORGANIZATION/USER), targetId, accessAllowed 전체/ALLOW/DENY | 메뉴 계층 순서: 대메뉴 display_order → 중메뉴 display_order → 화면 display_order | listMenuPermissions의 page, size query parameter를 사용한다. | not applicable — 원문에 메뉴 권한 bulk action 없음. | not applicable — 파일/Excel 기능은 제외 범위이며 메뉴 권한 export 요구가 없다. | `조회된 메뉴 권한이 없습니다`를 표시하고 [저장]은 선택 row가 없으므로 비활성화한다. |

#### Role/State Transition Matrix

| 화면ID | UI state/action | server authorization | allowed UI behavior | blocked UI behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|---|
| SCR-MENU-PERMISSION-MGMT | R09 진입 | SessionCookie + R09 | listMenuPermissions 호출, matrix 표시, row 선택, saveMenuPermissions 저장 가능 | R09 외 사용자의 검색조건·matrix·저장 CTA 표시 금지 | listMenuPermissions / saveMenuPermissions | REQ-083 |
| SCR-MENU-PERMISSION-MGMT | 비인증 진입 | SessionCookie 없음 | protected route guard가 인증 필요 흐름으로 보낸다. | matrix와 권한 저장 CTA 렌더링 금지 | listMenuPermissions | REQ-083 |
| SCR-MENU-PERMISSION-MGMT | R09 외 인증 사용자 진입 | 403 또는 menu permission deny | permission 상태만 표시한다. | 검색조건·matrix·저장 CTA 렌더링 금지 | listMenuPermissions / saveMenuPermissions | REQ-083 |
| SCR-MENU-PERMISSION-MGMT | ROLE 대상 조회 | SessionCookie + R09 | targetType=ROLE로 listMenuPermissions 호출 후 역할별 대메뉴·중메뉴·화면 접근권한 표시 | ROLE 외 권한 우선순위 결과를 화면에서 임의 계산해 저장값처럼 표시 금지 | listMenuPermissions | REQ-038 |
| SCR-MENU-PERMISSION-MGMT | ORGANIZATION 대상 조회 | SessionCookie + R09 | targetType=ORGANIZATION으로 listMenuPermissions 호출 후 조직별 대메뉴·중메뉴·화면 접근권한 표시 | 조직 권한을 역할 권한 행에 덮어쓰기 금지 | listMenuPermissions | REQ-039 |
| SCR-MENU-PERMISSION-MGMT | USER 대상 조회 | SessionCookie + R09 | targetType=USER로 listMenuPermissions 호출 후 사용자별 대메뉴·중메뉴·화면 접근권한 표시 | 사용자 권한을 조직/역할 권한 행에 덮어쓰기 금지 | listMenuPermissions | REQ-040 |
| SCR-MENU-PERMISSION-MGMT | saveMenuPermissions 성공 | SessionCookie + R09 | matrix 재조회, 선택 row 최신 accessAllowed 반영, success message 표시 | 저장 성공 전 optimistic 확정 표시 금지 | saveMenuPermissions / listMenuPermissions | REQ-036 |
| SCR-MENU-PERMISSION-MGMT | saveMenuPermissions 검증 실패 | SessionCookie + R09 + server validation | ApiError.fields를 필드별 표시하고 편집값 수정 가능 | persisted 성공 message 표시 금지 | saveMenuPermissions | REQ-036 |

#### Navigation and Sequence Flow

```text
entry
  └─ 시스템 관리 > 역할·권한 관리 > 메뉴 권한 관리 클릭
      ├─ no session → /login 또는 인증 필요 상태
      ├─ no R09 permission → SCR-MENU-PERMISSION-MGMT permission state
      └─ R09 session → listMenuPermissions loading
           ├─ 200 with rows → permission matrix success → row select → detail/save panel
           │     ├─ accessAllowed ALLOW/DENY 변경 → 편집 중
           │     ├─ [취소] → selected row values restored, stay /admin/menu-permissions
           │     └─ [저장] → confirmation modal
           │           ├─ confirm → saveMenuPermissions
           │           │     ├─ 200 → success message → listMenuPermissions refresh → stay /admin/menu-permissions
           │           │     ├─ 400 → field errors shown → stay in detail/save panel
           │           │     └─ 403 → permission state
           │           └─ cancel modal → return to detail/save panel without API call
           ├─ 200 empty → empty state, save disabled
           └─ 4xx/5xx → error banner with retry [조회]
```



#### Code Agent Handoff

| 화면ID | route | ownership | read actions | mutation/local actions | required states/tests | OQ |
|---|---|---|---|---|---|---|
| SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | US-06 메뉴 권한 관리 vertical slice; frontend file target `frontend/src/pages/admin/SCR-MENU-PERMISSION-MGMT.tsx`, test target `frontend/src/pages/admin/SCR-MENU-PERMISSION-MGMT.test.tsx` | listMenuPermissions GET `/api/admin/menu-permissions` with targetType, targetId, page, size | saveMenuPermissions PUT `/api/admin/menu-permissions`; local reset, row select, accessAllowed edit, cancel, pagination | loading, empty, error, permission, success; T045 screen state test와 T044 API contract test가 먼저 실패해야 한다. | 없음 — 권한 충돌 우선순위는 REQ-043 API 계약으로 확정되어 화면은 서버 결과를 반영한다. |


## Feature — US-07

### UI Design Fragment F-07 — US-07 메뉴 구조 관리

#### Design Reference 적용 메모

- 레퍼런스 `ex`는 mini-sidebar, header profile area, light/dark theme, compact table/form density, responsive content container의 방향성만 적용한다.
- 기능 계약의 화면ID, route, role, menu_path, primary_entity, operationId는 승인된 `ui-design.md` Screen Skeleton Ledger와 `contracts/openapi.yaml`을 따른다.
- 이 fragment는 US-07 `메뉴 구조 관리`의 화면 상세만 작성하며 전역 route inventory, navigation map, common shell은 작성하지 않는다.
- 메뉴 구조 관리 화면은 대메뉴·중메뉴·소메뉴 계층을 직접 조정하는 `tree editor` archetype으로 설계한다. CRUD 일반 목록이나 메뉴 정보 관리 화면의 실행정보 편집 필드는 이 화면에 배치하지 않는다.



#### SCR-MENU-STRUCTURE-MGMT Wireframe

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar │ Header: 시스템 관리 / 메뉴 관리 / 메뉴 구조 관리        R09 관리자 │
├──────────────┴─────────────────────────────────────────────────────────────────────┤
│ 메뉴 구조 관리                                                                      │
│ 대메뉴·중메뉴·소메뉴의 부모-자식 관계와 동일 계층 표시 순서를 관리한다.             │
│                                                                                    │
│ ┌────────────────────────────── 메뉴 계층구조 ───────────────────────────────────┐ │
│ │ [새로고침] getMenuTree                                                         │ │
│ │                                                                              │ │
│ │ ▼ 시스템 관리                                      display_order: 1            │ │
│ │   ▼ 사용자·조직 관리                               display_order: 1            │ │
│ │     • 사용자 관리                                  display_order: 1            │ │
│ │     • 조직 관리                                    display_order: 2            │ │
│ │   ▼ 역할·권한 관리                                 display_order: 2            │ │
│ │     • 역할 관리                                    display_order: 1            │ │
│ │     • 사용자 역할 관리                             display_order: 2            │ │
│ │     • 메뉴 권한 관리                               display_order: 3            │ │
│ │   ▼ 메뉴 관리                                      display_order: 3            │ │
│ │     • 메뉴 구조 관리  ◀ selected                   display_order: 1            │ │
│ │     • 메뉴 정보 관리                               display_order: 2            │ │
│ │   ▼ 공통코드 관리                                  display_order: 4            │ │
│ │     • 코드그룹 관리                                display_order: 1            │ │
│ │     • 상세코드 관리                                display_order: 2            │ │
│ └────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                    │
│ ┌──────────────────── 선택 메뉴 부모 변경 ────────────────────┬─────────────────┐ │
│ │ 선택 menu_id        [OQ-UI-071: menu_id 표시 형식 미정]       │ 동일 계층 순서   │ │
│ │ 메뉴명              [메뉴 구조 관리] readonly                 │ parent_menu_id   │ │
│ │ 메뉴 유형           [소메뉴/화면] readonly                    │ [메뉴 관리 ▼]    │ │
│ │ 현재 부모메뉴        [메뉴 관리] readonly                     │                 │ │
│ │ 새 부모메뉴          [메뉴 관리 ▼]                            │ 1 메뉴 구조 관리 │ │
│ │ 변경 사유           [메뉴 계층 정비_____________________]     │ 2 메뉴 정보 관리 │ │
│ │                                                            │                 │ │
│ │ [부모메뉴 저장] → 확인: 선택 메뉴의 부모메뉴를 변경하시겠습니까?              │ │
│ │ 차단 표시: 자기 자신 또는 하위 메뉴는 부모메뉴로 선택할 수 없습니다.           │ │
│ ├──────────────────────────────────────────────────────────────┼─────────────────┤ │
│ │ 동일 계층 내 표시순서 재정렬: 위/아래 이동 또는 drag handle                  │ │
│ │ [순서 저장] → 확인: 현재 parent_menu_id의 orderedMenuIds 순서를 저장합니까?   │ │
│ │ [취소] → 마지막 getMenuTree 결과로 부모/순서 편집값 복원                     │ │
│ └────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                    │
│ success: 저장 후 getMenuTree 재조회, 변경된 parent_menu_id와 display_order 표시    │
│ error: ApiError.message와 ApiError.fields(parentMenuId, orderedMenuIds, changeReason) 표시 │
│ permission: `메뉴 구조 관리 권한이 없습니다`만 표시하고 tree/editor CTA 숨김        │
└────────────────────────────────────────────────────────────────────────────────────┘
```

#### Navigation / Sequence Flow

| flow_id | 화면ID | entry | success | cancel | error | permission-denied |
|---|---|---|---|---|---|---|
| FLOW-MENU-STRUCTURE-TREE | SCR-MENU-STRUCTURE-MGMT | R09가 sidebar `시스템 관리 > 메뉴 관리 > 메뉴 구조 관리`를 선택해 `/admin/menu-structure`로 진입하면 getMenuTree를 호출한다. | 부모메뉴 저장 또는 순서 저장 성공 후 같은 `/admin/menu-structure`에 머물며 getMenuTree를 재호출하고 tree에서 변경된 parent_menu_id/display_order를 강조한다. | [취소]를 누르면 서버 호출 없이 마지막 getMenuTree 응답 기준으로 선택 메뉴, 새 부모메뉴, orderedMenuIds 편집값을 복원한다. | getMenuTree/updateMenuParent/reorderMenus ApiError는 화면 상단 오류 banner와 필드별 오류로 표시하고 저장 전 tree는 확정 상태로 유지한다. | 403 또는 R09 권한 부재 시 tree와 편집 CTA를 렌더링하지 않고 `메뉴 구조 관리 권한이 없습니다` 상태를 표시한다. |

#### Screen States

| 화면ID | state | trigger | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|
| SCR-MENU-STRUCTURE-MGMT | loading | `/admin/menu-structure` 진입 또는 [새로고침] 클릭 후 getMenuTree 응답 대기 | 메뉴 계층구조 영역에 tree skeleton과 `메뉴 계층구조를 불러오는 중입니다` 메시지를 표시하고 부모메뉴 저장·순서 저장 CTA를 비활성화한다. | getMenuTree | REQ-046, REQ-084 |
| SCR-MENU-STRUCTURE-MGMT | empty | getMenuTree가 빈 계층을 반환 | tree 영역에 `표시할 메뉴가 없습니다`를 표시한다. 시드 메뉴 tree 부재는 seed/test fixture 결함으로 기록하고 이 화면에서 임의 메뉴 생성 CTA는 제공하지 않는다. | getMenuTree | REQ-046 |
| SCR-MENU-STRUCTURE-MGMT | error | getMenuTree, updateMenuParent, reorderMenus가 ApiError를 반환 | 화면 상단에 ApiError.message를 표시한다. updateMenuParent 400은 parentMenuId/changeReason 필드 아래에, reorderMenus 400은 orderedMenuIds 영역 아래에 ApiError.fields를 표시한다. | getMenuTree / updateMenuParent / reorderMenus | REQ-044, REQ-045, REQ-046 |
| SCR-MENU-STRUCTURE-MGMT | permission | 인증되었으나 R09 권한이 없거나 서버가 403을 반환 | `메뉴 구조 관리 권한이 없습니다` 상태만 표시하고 메뉴 tree, 부모 선택, 순서 조정, 저장 CTA를 렌더링하지 않는다. | getMenuTree / updateMenuParent / reorderMenus | REQ-084 |
| SCR-MENU-STRUCTURE-MGMT | success | getMenuTree 200, updateMenuParent 200, reorderMenus 200 | 대메뉴·중메뉴·소메뉴 tree와 각 노드의 display_order가 보인다. 저장 성공 시 `저장되었습니다` 메시지를 표시하고 getMenuTree 재조회 결과로 변경된 부모-자식 관계 또는 표시순서를 확인할 수 있다. | getMenuTree / updateMenuParent / reorderMenus | REQ-044, REQ-045, REQ-046, REQ-084 |

#### Per-Screen UI Contract




#### Data Binding

##### Menu Tree Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-MENU-STRUCTURE-MGMT | 메뉴 계층구조 node identity | menu_id | getMenuTree data[].menuId | menus.menu_id | REQ-044, REQ-046 | 부모 변경 path parameter와 tree node identity로 사용한다. 표시 형식은 OQ-UI-071로 보존한다. |
| SCR-MENU-STRUCTURE-MGMT | 메뉴 계층구조 indentation | parent_menu_id | getMenuTree data[].parentMenuId | menus.parent_menu_id | REQ-044, REQ-046 | 대메뉴·중메뉴·소메뉴 부모-자식 관계를 tree indentation으로 표현한다. |
| SCR-MENU-STRUCTURE-MGMT | 메뉴 계층구조 label | menu_name | getMenuTree data[].menuName | menus.menu_name | REQ-046 | 구조 조정 대상의 사람이 읽는 label이며 이 화면에서는 readonly다. |
| SCR-MENU-STRUCTURE-MGMT | 메뉴 유형 표시 | menu_type | getMenuTree data[].menuType | menus.menu_type | REQ-046 | MAIN/MIDDLE/SUB/SCREEN 값을 표시해 대메뉴·중메뉴·소메뉴/화면 구분을 보여준다. |
| SCR-MENU-STRUCTURE-MGMT | 동일 계층 정렬 | display_order | getMenuTree data[].displayOrder | menus.display_order | REQ-045, REQ-046 | 같은 parent_menu_id 아래 형제 메뉴의 표시 순서를 결정한다. |
| SCR-MENU-STRUCTURE-MGMT | 메뉴 사용/상태 badge | system_use_yn, status | getMenuTree data[].systemUseYn, data[].status | menus.system_use_yn, menus.status | REQ-084 | 화면에서 상태를 보여주되 비활성화/삭제성 처리는 이 story의 저장 CTA가 아니다. |

##### Parent Change Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-MENU-STRUCTURE-MGMT | 선택 메뉴 상세 | menu_id | selected getMenuTree node | menus.menu_id | REQ-044 | updateMenuParent의 `{menuId}` path parameter로 사용한다. |
| SCR-MENU-STRUCTURE-MGMT | 새 부모메뉴 select | parentMenuId | MenuParentUpdateRequest.parentMenuId | menus.parent_menu_id | REQ-044 | 저장 성공 후 선택 메뉴의 parent_menu_id가 변경되어야 한다. |
| SCR-MENU-STRUCTURE-MGMT | 변경 사유 입력 | changeReason | MenuParentUpdateRequest.changeReason | menus.change_reason | REQ-044 | 부모 관계 변경의 사유 추적 입력이다. |

##### Reorder Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-MENU-STRUCTURE-MGMT | 동일 계층 순서 편집 | parentMenuId | MenuReorderRequest.parentMenuId | menus.parent_menu_id | REQ-045 | null이면 최상위 계층, 값이 있으면 해당 부모 아래 형제 메뉴 순서다. |
| SCR-MENU-STRUCTURE-MGMT | 동일 계층 순서 편집 | orderedMenuIds | MenuReorderRequest.orderedMenuIds[] | menus.menu_id | REQ-045 | orderedMenuIds 배열 순서가 저장 후 display_order로 반영된다. |
| SCR-MENU-STRUCTURE-MGMT | 순서 결과 표시 | display_order | getMenuTree data[].displayOrder | menus.display_order | REQ-045 | reorderMenus 성공 후 getMenuTree 재조회로 확인한다. |

#### List/Tree Behavior Contract

| 화면ID | filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-MENU-STRUCTURE-MGMT | not applicable — 원문은 검색조건 없이 메뉴 계층구조 조회를 요구한다. | 동일 parent_menu_id 안에서 display_order 오름차순을 기본 표시 순서로 사용한다. | not applicable — 원문은 메뉴 tree 계층 조회이며 page/size collection 목록을 요구하지 않는다. | not applicable — 원문은 동일 계층 재정렬과 부모메뉴 변경만 요구하며 bulk processing을 요구하지 않는다. | not applicable — 원문은 export/download를 요구하지 않는다. | getMenuTree가 빈 계층을 반환하면 `표시할 메뉴가 없습니다`를 표시하고 메뉴 생성 CTA는 제공하지 않는다. |

#### Role / State Transition Matrix

| 화면ID | user/server condition | UI behavior | server authorization or validation | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|
| SCR-MENU-STRUCTURE-MGMT | R09 세션이 `/admin/menu-structure`에 진입 | tree/editor를 표시하고 getMenuTree 결과를 렌더링한다. | SessionCookie와 R09 role을 요구한다. | getMenuTree | REQ-046, REQ-084 |
| SCR-MENU-STRUCTURE-MGMT | 인증 세션 없음 | protected shell이 로그인 필요 상태로 전환하거나 API 401 오류를 표시한다. | 보호 API는 401 ApiError를 반환한다. | getMenuTree / updateMenuParent / reorderMenus | REQ-084 |
| SCR-MENU-STRUCTURE-MGMT | 인증되었으나 R09 권한 없음 | `메뉴 구조 관리 권한이 없습니다` 상태를 표시하고 모든 조정 CTA를 숨긴다. | 보호 API는 403 ApiError를 반환한다. | getMenuTree / updateMenuParent / reorderMenus | REQ-084 |
| SCR-MENU-STRUCTURE-MGMT | R09가 유효한 parentMenuId를 저장 | 저장 완료 메시지 후 getMenuTree 재조회로 선택 메뉴의 새 부모 위치를 표시한다. | updateMenuParent가 menus.parent_menu_id를 저장한다. | updateMenuParent | REQ-044 |
| SCR-MENU-STRUCTURE-MGMT | R09가 parentMenuId로 자기 menu_id를 제출 | field error를 parentMenuId 아래에 표시하고 선택 메뉴는 기존 부모 아래에 남는다. | updateMenuParent가 400 ApiError.fields.parentMenuId로 차단한다. | updateMenuParent | REQ-047 |
| SCR-MENU-STRUCTURE-MGMT | R09가 하위 메뉴를 parentMenuId로 제출 | field error를 parentMenuId 아래에 표시하고 tree 순환 구조를 렌더링하지 않는다. | updateMenuParent가 400 ApiError.fields.parentMenuId로 차단한다. | updateMenuParent | REQ-048 |
| SCR-MENU-STRUCTURE-MGMT | R09가 동일 계층 orderedMenuIds를 저장 | 저장 완료 메시지 후 같은 parent_menu_id 아래 메뉴가 새 display_order 순서로 보인다. | reorderMenus가 menus.display_order를 저장한다. | reorderMenus | REQ-045 |




## Feature — US-08

### F-08 UI Design Fragment — US-08 메뉴 정보 관리



#### SCR-MENU-INFO-MGMT Wireframe

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar: 시스템 관리 > 메뉴 관리 > 메뉴 정보 관리                                  Header: R09 관리자 │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 메뉴 정보 관리                                                                                             │
│ 메뉴와 실행 화면을 연결하기 위한 실행정보를 조회·등록·수정한다.                                            │
├───────────────────────────────┬────────────────────────────────────────────────────────────────────────────┤
│ 메뉴 선택 영역                 │ 실행정보 편집                                                            │
│ ┌───────────────────────────┐ │ ┌──────────────────────────────────────────────────────────────────────┐ │
│ │ 검색조건                  │ │ │ 선택 메뉴: [메뉴명 표시 영역]                                      │ │
│ │ 메뉴명 [______________]   │ │ │ 상태: loading / empty / error / permission / success message        │ │
│ │ URL   [______________]    │ │ └──────────────────────────────────────────────────────────────────────┘ │
│ │ [조회] [조건 초기화]      │ │ ┌──────────────────────────────────────────────────────────────────────┐ │
│ └───────────────────────────┘ │ │ 메뉴명*       [________________________________________]             │ │
│ ┌───────────────────────────┐ │ │ 화면ID*       [________________________________________]             │ │
│ │ 메뉴 목록                 │ │ │ URL*          [________________________________________]             │ │
│ │ 메뉴명 | 화면ID | URL     │ │ │ 아이콘        [________________________________________]             │ │
│ │ 시스템 관리 | ... | ...   │ │ │ 업무구분      [________________________________________]             │ │
│ │ 메뉴 관리   | ... | ...   │ │ │ 설명          [________________________________________]             │ │
│ │ 메뉴 정보 관리 | SCR...   │ │ │ 변경 사유     [________________________________________]             │ │
│ └───────────────────────────┘ │ └──────────────────────────────────────────────────────────────────────┘ │
│                               │ ┌──────────────────────────────────────────────────────────────────────┐ │
│                               │ │ [저장]  [취소]                                                       │ │
│                               │ │ 저장 확인: 선택 메뉴의 메뉴명·화면ID·URL·아이콘·업무구분·설명을     │ │
│                               │ │ updateMenuExecution으로 저장한다.                                  │ │
│                               │ └──────────────────────────────────────────────────────────────────────┘ │
├───────────────────────────────┴────────────────────────────────────────────────────────────────────────────┤
│ 성공 후: getMenuExecution 재조회 결과로 화면ID와 URL이 갱신되어 표시되고, 메뉴 클릭 시 해당 URL로 연결된다. │
└────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Screen States

| 화면ID | state | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|
| SCR-MENU-INFO-MGMT | loading | route `/admin/menu-info` 진입 또는 메뉴 행 선택 직후 실행정보 영역에 로딩 표시를 보여주고 편집 입력과 저장 CTA를 비활성화한다. | getMenuExecution | REQ-050 |
| SCR-MENU-INFO-MGMT | empty | 조회 가능한 메뉴 실행정보가 없으면 메뉴 목록 영역에 빈 결과 안내를 표시하고 편집 폼은 선택 전 상태로 둔다. | getMenuExecution | REQ-050 |
| SCR-MENU-INFO-MGMT | error | getMenuExecution 또는 updateMenuExecution 실패 시 오류 안내 영역에 ApiError.message와 field-level 오류를 표시하고 기존 입력값을 유지한다. | getMenuExecution / updateMenuExecution | REQ-051 |
| SCR-MENU-INFO-MGMT | permission | R09가 아니거나 메뉴 정보 관리 권한이 없으면 보호 화면 대신 권한 없음 상태를 표시하고 저장 CTA를 렌더링하지 않는다. | GET/PUT /api/admin/menus/{menuId}/execution | REQ-085 |
| SCR-MENU-INFO-MGMT | success | 저장 성공 후 성공 메시지를 표시하고 같은 menuId로 getMenuExecution을 재호출해 메뉴명·화면ID·URL·아이콘·업무구분·설명 최신값을 보여준다. | updateMenuExecution -> getMenuExecution | REQ-049, REQ-051 |

#### Per-Screen UI Contract




#### Data Binding

##### Query and Form Binding

| 화면ID | UI element | data source | bound field/path | readonly/editable | canonical_id |
|---|---|---|---|---|---|
| SCR-MENU-INFO-MGMT | 메뉴명 검색조건 | local state -> getMenuExecution selection support | menuName | editable filter | REQ-050 |
| SCR-MENU-INFO-MGMT | URL 검색조건 | local state -> getMenuExecution selection support | url | editable filter | REQ-050 |
| SCR-MENU-INFO-MGMT | 메뉴 목록 `메뉴명` column | getMenuExecution response / menu tree selection context | menuName | readonly list display | REQ-050 |
| SCR-MENU-INFO-MGMT | 메뉴 목록 `화면ID` column | getMenuExecution response | screenId | readonly list display | REQ-052 |
| SCR-MENU-INFO-MGMT | 메뉴 목록 `URL` column | getMenuExecution response | url | readonly list display | REQ-052 |
| SCR-MENU-INFO-MGMT | 편집 `메뉴명` input | MenuExecutionRequest | menuName | editable required | REQ-049, REQ-051 |
| SCR-MENU-INFO-MGMT | 편집 `화면ID` input | MenuExecutionRequest | screenId | editable required | REQ-049, REQ-052 |
| SCR-MENU-INFO-MGMT | 편집 `URL` input | MenuExecutionRequest | url | editable required | REQ-049, REQ-052 |
| SCR-MENU-INFO-MGMT | 편집 `아이콘` input | MenuExecutionRequest | icon | editable optional | REQ-051 |
| SCR-MENU-INFO-MGMT | 편집 `업무구분` input | MenuExecutionRequest | businessCategory | editable optional | REQ-051 |
| SCR-MENU-INFO-MGMT | 편집 `설명` input | MenuExecutionRequest | description | editable optional | REQ-051 |
| SCR-MENU-INFO-MGMT | 편집 `변경 사유` input | MenuExecutionRequest | changeReason | editable when mutation reason is required by server | REQ-051 |

##### List-Capable Screen Behavior

| 화면ID | real filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-MENU-INFO-MGMT | 메뉴명, URL | 원문은 정렬 기준을 명시하지 않으므로 OQ-UI-080: 메뉴 정보 관리 기본 정렬 기준 확인 필요 | OpenAPI getMenuExecution은 단건 path 기반이므로 페이지네이션은 not applicable; 메뉴 선택 목록의 페이지네이션 필요 여부는 OQ-UI-081 | not applicable | not applicable | 실행정보가 없으면 `등록할 메뉴 실행정보가 없습니다` 안내와 선택 전 편집 폼을 표시한다. |

#### Role and State Transition Matrix

| 화면ID | role/state | UI behavior | server authorization | canonical_id |
|---|---|---|---|---|
| SCR-MENU-INFO-MGMT | R09 + 접근 허용 | 메뉴 목록, 실행정보 편집 폼, 조회/저장/취소 CTA를 표시한다. | getMenuExecution / updateMenuExecution security SessionCookie + x-roles R09 허용 | REQ-085 |
| SCR-MENU-INFO-MGMT | 인증 없음 | `/login`으로 이동하거나 인증 필요 상태를 표시하고 실행정보 API를 호출하지 않는다. | 보호 API는 401 ApiError를 반환한다. | REQ-085 |
| SCR-MENU-INFO-MGMT | 인증됨 + 권한 없음 | permission 상태를 표시하고 저장 CTA를 숨긴다. | 보호 API는 403 ApiError를 반환한다. | REQ-085 |
| SCR-MENU-INFO-MGMT | 편집 중 | 필수 입력을 사용자가 수정할 수 있고, 저장 전에는 서버 상태가 바뀌지 않는다. | updateMenuExecution 호출 전까지 menu_execution_info 변경 없음 | REQ-051 |
| SCR-MENU-INFO-MGMT | 저장 성공 | success 메시지와 재조회된 실행정보를 표시한다. | updateMenuExecution이 menu_execution_info를 저장하고 getMenuExecution으로 확인 가능 | REQ-049, REQ-051 |
| SCR-MENU-INFO-MGMT | 저장 실패 | field-level 오류와 error 상태를 표시하고 입력값을 유지한다. | 400 ApiError.fields 또는 401/403 ApiError 반환 | REQ-051 |

#### Navigation and Sequence Flow

```text
[Sidebar: 시스템 관리 > 메뉴 관리 > 메뉴 정보 관리]
        |
        v
[/admin/menu-info 진입]
        |
        +-- 권한 없음 또는 인증 없음 --> [permission 상태 또는 /login 이동] --(권한 확보 후 재진입)--> [/admin/menu-info]
        |
        v
[loading: 메뉴 실행정보 조회 준비]
        |
        +-- 조회 오류 --> [error 상태: ApiError.message 표시] --(다시 조회)--> [loading]
        |
        +-- 실행정보 없음 --> [empty 상태: 등록 가능 안내]
        |
        v
[메뉴 행 선택 또는 조회]
        |
        v
[getMenuExecution 결과 표시: 메뉴명/화면ID/URL/아이콘/업무구분/설명]
        |
        +-- 취소 --> [마지막 조회값으로 복원]
        |
        +-- 저장 클릭 + 확인 --> [updateMenuExecution]
                                  |
                                  +-- 성공 --> [success 메시지] --> [getMenuExecution 재조회] --> [갱신된 실행정보 표시]
                                  |
                                  +-- 검증/권한 오류 --> [error 또는 permission 상태, 기존 입력 유지]
```




## Feature — US-09

### F-09 UI Design Fragment — US-09 코드그룹 관리



#### SCR-CODE-GROUP-MGMT Wireframe

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar: 시스템 관리 > 공통코드 관리 > 코드그룹 관리                              Header: R09 관리자 │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 코드그룹 관리                                                                                              │
│ 평가영역·처리상태·인증구분 등 코드 묶음의 그룹ID, 명칭, 설명, 관리부서를 조회·등록·수정한다.              │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 검색조건                                                                                                   │
│ 그룹ID [____________________]                                                               [조회] [초기화] │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 코드그룹 목록                                                                                 [신규 등록] │
│ ┌──────────────┬──────────────────────┬──────────────────────────────┬────────────────────┬─────────────┐ │
│ │ 그룹ID       │ 명칭                 │ 설명                         │ 관리부서           │ 상세코드    │ │
│ ├──────────────┼──────────────────────┼──────────────────────────────┼────────────────────┼─────────────┤ │
│ │ EVAL_AREA    │ 평가영역             │ 평가영역 코드 묶음            │ OQ-UI-091          │ [상세코드]  │ │
│ │ PROC_STATUS  │ 처리상태             │ 처리상태 코드 묶음            │ OQ-UI-091          │ [상세코드]  │ │
│ │ AUTH_TYPE    │ 인증구분             │ 인증구분 코드 묶음            │ OQ-UI-091          │ [상세코드]  │ │
│ └──────────────┴──────────────────────┴──────────────────────────────┴────────────────────┴─────────────┘ │
│ 목록 상태: loading spinner / empty 안내 / error ApiError.message / permission denied / success toast        │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 선택 코드그룹 상세·등록·수정                                                                              │
│ ┌───────────────────────────────────────────────┬────────────────────────────────────────────────────────┐ │
│ │ 그룹ID*                                      │ [____________________________] OQ-UI-090               │ │
│ │ 명칭*                                        │ [____________________________________________________] │ │
│ │ 설명                                         │ [____________________________________________________] │ │
│ │ 관리부서*                                    │ [____________________________________________________] │ │
│ │ 변경 사유                                    │ [____________________________________________________] │ │
│ └───────────────────────────────────────────────┴────────────────────────────────────────────────────────┘ │
│ [저장] [취소]                                                                                              │
│ 저장 확인: 입력한 그룹ID·명칭·설명·관리부서를 createCodeGroup 또는 updateCodeGroup으로 저장한다.           │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 성공 후: /admin/code-groups에 머물며 listCodeGroups 재조회 결과로 저장된 코드그룹 행이 표시된다.           │
│ 상세코드 이동: 목록 행의 [상세코드]를 누르면 groupId를 전달해 /admin/detail-codes의 해당 코드그룹 목록으로 이동한다. │
└────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Screen States

| 화면ID | state | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|
| SCR-CODE-GROUP-MGMT | loading | route `/admin/code-groups` 진입 또는 `조회` 클릭 직후 목록 영역에 로딩 표시를 보여주고 저장 CTA와 상세코드 이동 CTA를 비활성화한다. | listCodeGroups | REQ-054 |
| SCR-CODE-GROUP-MGMT | empty | 검색 결과 코드그룹이 없으면 목록에 `조회된 코드그룹이 없습니다` 안내를 표시하고 신규 등록 폼은 사용할 수 있게 둔다. | listCodeGroups | REQ-054 |
| SCR-CODE-GROUP-MGMT | error | listCodeGroups, createCodeGroup, updateCodeGroup 실패 시 ApiError.message와 ApiError.fields를 목록 상단 또는 해당 입력 아래에 표시하고 사용자가 입력한 값은 유지한다. | listCodeGroups / createCodeGroup / updateCodeGroup | REQ-053, REQ-054 |
| SCR-CODE-GROUP-MGMT | permission | R09가 아니거나 코드그룹 관리 권한이 없으면 화면 본문 대신 권한 없음 상태를 표시하고 조회·신규 등록·저장·상세코드 CTA를 렌더링하지 않는다. | GET/POST/PUT /api/admin/code-groups | REQ-086 |
| SCR-CODE-GROUP-MGMT | success | 등록 또는 수정 성공 후 성공 메시지를 표시하고 listCodeGroups를 재호출해 그룹ID·명칭·설명·관리부서가 반영된 최신 목록 행을 보여준다. | createCodeGroup/updateCodeGroup -> listCodeGroups | REQ-053, REQ-054 |

#### Per-Screen UI Contract




#### Data Binding

##### Query, List, and Form Binding

| 화면ID | UI element | data source | bound field/path | readonly/editable | canonical_id |
|---|---|---|---|---|---|
| SCR-CODE-GROUP-MGMT | 그룹ID 검색조건 | local state -> listCodeGroups query | groupIdFilter | editable filter | REQ-054 |
| SCR-CODE-GROUP-MGMT | 목록 `그룹ID` column | listCodeGroups response | groupId | readonly list display | REQ-054, REQ-055 |
| SCR-CODE-GROUP-MGMT | 목록 `명칭` column | listCodeGroups response | groupName | readonly list display | REQ-054 |
| SCR-CODE-GROUP-MGMT | 목록 `설명` column | listCodeGroups response | description | readonly list display | REQ-054 |
| SCR-CODE-GROUP-MGMT | 목록 `관리부서` column | listCodeGroups response | managingDepartment | readonly list display | REQ-054 |
| SCR-CODE-GROUP-MGMT | 목록 `상세코드` action | selected row local state | groupId -> `/admin/detail-codes` navigation parameter | readonly row identifier passed to navigation | REQ-055 |
| SCR-CODE-GROUP-MGMT | 폼 `그룹ID` input | CodeGroupRequest / selected row | groupId | editable on create; update mutability unresolved as OQ-UI-090 | REQ-053 |
| SCR-CODE-GROUP-MGMT | 폼 `명칭` input | CodeGroupRequest | groupName | editable required | REQ-053 |
| SCR-CODE-GROUP-MGMT | 폼 `설명` input | CodeGroupRequest | description | editable optional | REQ-053 |
| SCR-CODE-GROUP-MGMT | 폼 `관리부서` input | CodeGroupRequest | managingDepartment | editable required | REQ-053 |
| SCR-CODE-GROUP-MGMT | 폼 `변경 사유` input | CodeGroupRequest | changeReason | editable when server requires mutation reason | REQ-053 |

##### List-Capable Screen Behavior

| 화면ID | real filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-CODE-GROUP-MGMT | 그룹ID | 원문은 정렬 기준을 명시하지 않으므로 OQ-UI-093: 코드그룹 기본 정렬 기준 확인 필요 | listCodeGroups OpenAPI가 page, size query parameter를 제공하므로 페이지 번호와 페이지 크기를 사용한다. | not applicable | not applicable | 조건에 맞는 코드그룹이 없으면 `조회된 코드그룹이 없습니다`를 표시하고 `신규 등록` CTA는 유지한다. |

#### Role and State Transition Matrix

| 화면ID | role/state | UI behavior | server authorization | canonical_id |
|---|---|---|---|---|
| SCR-CODE-GROUP-MGMT | R09 + 접근 허용 | 검색조건, 코드그룹 목록, 신규 등록, 선택 행 수정, 상세코드 이동 CTA를 표시한다. | listCodeGroups / createCodeGroup / updateCodeGroup security SessionCookie + x-roles R09 허용 | REQ-086 |
| SCR-CODE-GROUP-MGMT | 인증 없음 | `/login`으로 이동하거나 인증 필요 상태를 표시하고 코드그룹 API를 호출하지 않는다. | 보호 API는 401 ApiError를 반환한다. | REQ-086 |
| SCR-CODE-GROUP-MGMT | 인증됨 + 권한 없음 | permission 상태를 표시하고 조회·저장·상세코드 CTA를 숨긴다. | 보호 API는 403 ApiError를 반환한다. | REQ-086 |
| SCR-CODE-GROUP-MGMT | 신규 등록 중 | groupId, groupName, description, managingDepartment, changeReason 입력을 표시하고 저장 전에는 code_groups row가 생성되지 않는다. | createCodeGroup 호출 전까지 code_groups 변경 없음 | REQ-053 |
| SCR-CODE-GROUP-MGMT | 수정 중 | 선택 행의 groupName, description, managingDepartment를 편집하며 groupId 수정 여부는 OQ-UI-090로 표시한다. | updateCodeGroup 호출 전까지 code_groups 변경 없음 | REQ-053 |
| SCR-CODE-GROUP-MGMT | 저장 성공 | success 메시지와 재조회된 목록 행을 표시한다. | createCodeGroup/updateCodeGroup이 code_groups를 저장하고 listCodeGroups로 확인 가능 | REQ-053, REQ-054 |
| SCR-CODE-GROUP-MGMT | 저장 실패 | field-level 오류와 error 상태를 표시하고 입력값을 유지한다. | 400 ApiError.fields 또는 401/403 ApiError 반환 | REQ-053 |
| SCR-CODE-GROUP-MGMT | 상세코드 이동 | 선택한 groupId를 이동 context로 유지하고 `/admin/detail-codes`로 이동한다. | 이 fragment에서는 API mutation 없이 navigation-only이며, 후속 상세코드 화면의 권한/API 검증을 따른다. | REQ-055 |

#### Navigation and Sequence Flow

```text
[Sidebar: 시스템 관리 > 공통코드 관리 > 코드그룹 관리]
        |
        v
[/admin/code-groups 진입]
        |
        +-- 권한 없음 또는 인증 없음 --> [permission 상태 또는 /login 이동] --(권한 확보 후 재진입)--> [/admin/code-groups]
        |
        v
[loading: listCodeGroups]
        |
        +-- 조회 오류 --> [error 상태: ApiError.message 표시] --(다시 조회)--> [loading]
        |
        +-- 결과 없음 --> [empty 상태: 신규 등록 CTA 유지]
        |
        v
[코드그룹 목록: 그룹ID/명칭/설명/관리부서/상세코드]
        |
        +-- 신규 등록 --> [빈 등록 폼] -- 저장 확인 --> [createCodeGroup]
        |                                             |
        |                                             +-- 성공 --> [success] --> [listCodeGroups 재조회]
        |                                             |
        |                                             +-- 검증/권한 오류 --> [error 또는 permission, 입력 유지]
        |
        +-- 행 선택 --> [수정 폼: 그룹ID OQ-UI-090, 명칭, 설명, 관리부서]
        |              |
        |              +-- 취소 --> [마지막 조회값으로 복원]
        |              |
        |              +-- 저장 확인 --> [updateCodeGroup]
        |                                   |
        |                                   +-- 성공 --> [success] --> [listCodeGroups 재조회]
        |                                   |
        |                                   +-- 검증/권한 오류 --> [error 또는 permission, 입력 유지]
        |
        +-- 상세코드 --> [groupId 전달] --> [/admin/detail-codes]
```




## Feature — US-10

### UI Design Fragment F-10 — US-10 상세코드 관리

#### Design Reference 적용 메모

- 레퍼런스 `ex`는 mini-sidebar, header profile area, light/dark theme, compact table/form density, responsive content container의 방향성만 적용한다.
- 기능 계약의 화면ID, route, role, menu_path, primary_entity, operationId는 승인된 `ui-design.md` Screen Skeleton Ledger와 `contracts/openapi.yaml`을 따른다.
- 이 fragment는 US-10 `상세코드 관리`의 화면 상세만 작성하며 전역 route inventory, navigation map, common shell은 작성하지 않는다.
- 상세코드 관리 화면은 코드그룹을 기준으로 상세코드 목록을 조회하고 코드값·코드명·상위코드·정렬순서·추가속성·사용여부·유효기간을 편집하는 `tree editor`와 `effective-period form` 결합 archetype으로 설계한다. 코드그룹 자체의 그룹ID·명칭·설명·관리부서 등록·수정은 US-09 `SCR-CODE-GROUP-MGMT` 소유다.



#### SCR-DETAIL-CODE-MGMT Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ mini sidebar: 시스템 관리 > 공통코드 관리 > 상세코드 관리                                Header: R09 관리자 │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 상세코드 관리                                                                                                │
│ 코드그룹별 상세코드를 조회하고, 화면 선택값과 연계 코드 매핑에 쓰이는 코드 계층과 속성을 관리한다.           │
├───────────────────────────────────────┬──────────────────────────────────────────────────────────────────────┤
│ 코드그룹 및 조회 조건                  │ 상세코드 등록/수정                                                   │
│ ┌───────────────────────────────────┐ │ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ groupId* [____________________]   │ │ │ mode: 신규 등록 / 선택 코드 수정                                │ │
│ │ route query groupId 또는 직접 입력│ │ │ 선택 코드: [code_value readonly in edit]                         │ │
│ │ [조회] listDetailCodes            │ │ └──────────────────────────────────────────────────────────────────┘ │
│ │ [조건 초기화]                     │ │ ┌──────────────────────────────────────────────────────────────────┐ │
│ └───────────────────────────────────┘ │ │ code_value*          [____________________]                       │ │
│ ┌───────────────────────────────────┐ │ │ code_name*           [______________________________]             │ │
│ │ 상세코드 목록                      │ │ │ parent_code_value    [상위 상세코드 선택 ▼ / 없음]                │ │
│ │ code_value | code_name | parent   │ │ │ sort_order*          [____]                                      │ │
│ │ sort_order | system_use_yn | 기간 │ │ │ additional_attributes [OQ-UI-100 구조·개수·형식 미확정]          │ │
│ │ ───────────────────────────────── │ │ │ system_use_yn        [Y ▼]                                      │ │
│ │ A     | 항목 A | -   | 1 | Y | 2026-01-01~ │ │ valid_start_date     [YYYY-MM-DD]                  │ │
│ │ A-1   | 항목 A-1 | A | 2 | Y | 2026-01-01~ │ │ valid_end_date       [YYYY-MM-DD 또는 빈 값]       │ │
│ │ 선택 행 클릭 → 편집 폼             │ │ │ changeReason         [________________________________]           │ │
│ └───────────────────────────────────┘ │ └──────────────────────────────────────────────────────────────────┘ │
│ ┌───────────────────────────────────┐ │ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ 상위코드 계층 미리보기             │ │ │ [신규] [저장] [취소]                                             │ │
│ │ groupId 기준 parent_code_value tree│ │ │ 저장 확인: 현재 groupId의 상세코드를 저장하시겠습니까?            │ │
│ │ - A                               │ │ │ 오류 표시: ApiError.fields(codeValue, codeName, sortOrder,       │ │
│ │   - A-1                           │ │ │ parentCodeValue, additionalAttributes, systemUseYn, dates)        │ │
│ └───────────────────────────────────┘ │ └──────────────────────────────────────────────────────────────────┘ │
├───────────────────────────────────────┴──────────────────────────────────────────────────────────────────────┤
│ success: createDetailCode/updateDetailCode 성공 후 listDetailCodes 재조회 결과로 code_value/code_name/계층 표시 │
│ permission: `상세코드 관리 권한이 없습니다`만 표시하고 조회·저장 CTA와 편집 폼을 숨긴다.                     │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Navigation / Sequence Flow

| flow_id | 화면ID | entry | success | cancel | error | permission-denied |
|---|---|---|---|---|---|---|
| FLOW-DETAIL-CODE-MGMT | SCR-DETAIL-CODE-MGMT | R09가 sidebar `시스템 관리 > 공통코드 관리 > 상세코드 관리`를 선택하거나 `SCR-CODE-GROUP-MGMT`의 상세코드 이동에서 groupId를 받아 `/admin/detail-codes`로 진입하면 groupId 입력 또는 route query groupId로 listDetailCodes를 호출한다. | createDetailCode 또는 updateDetailCode 성공 후 같은 `/admin/detail-codes`에 머물며 listDetailCodes를 재호출하고 저장된 code_value, code_name, parent_code_value, sort_order, additional_attributes, system_use_yn, valid_start_date, valid_end_date를 목록과 계층 미리보기에 표시한다. | [취소]를 누르면 서버 호출 없이 마지막 listDetailCodes 응답 기준으로 선택 행과 편집 폼을 복원하고 신규 등록 mode를 해제한다. | listDetailCodes/createDetailCode/updateDetailCode ApiError는 화면 상단 오류 banner와 필드별 오류로 표시하고 저장 전 detail_codes 값은 확정 상태로 유지한다. | 403 또는 R09 권한 부재 시 목록, 계층 미리보기, 등록/수정 폼, 저장 CTA를 렌더링하지 않고 `상세코드 관리 권한이 없습니다` 상태를 표시한다. |

#### Screen States

| 화면ID | state | trigger | browser-observable behavior | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|
| SCR-DETAIL-CODE-MGMT | loading | `/admin/detail-codes` 진입 후 groupId가 있거나 [조회] 클릭으로 listDetailCodes 응답 대기 | 상세코드 목록 영역에 loading skeleton과 `상세코드를 불러오는 중입니다` 메시지를 표시하고 [저장] CTA를 비활성화한다. | listDetailCodes | REQ-059, REQ-087 |
| SCR-DETAIL-CODE-MGMT | empty | 선택한 groupId의 listDetailCodes가 빈 목록을 반환 | `선택한 코드그룹에 등록된 상세코드가 없습니다` 안내와 [신규] CTA를 표시하며, 계층 미리보기에는 빈 tree 안내를 표시한다. | listDetailCodes | REQ-059 |
| SCR-DETAIL-CODE-MGMT | error | listDetailCodes, createDetailCode, updateDetailCode가 ApiError를 반환 | 화면 상단에 ApiError.message를 표시한다. 저장 400은 codeValue/codeName/sortOrder/parentCodeValue/additionalAttributes/systemUseYn/validStartDate/validEndDate/changeReason 필드 아래에 ApiError.fields를 표시한다. | listDetailCodes / createDetailCode / updateDetailCode | REQ-057, REQ-061 |
| SCR-DETAIL-CODE-MGMT | permission | 인증되었으나 R09 권한이 없거나 서버가 403을 반환 | `상세코드 관리 권한이 없습니다` 상태만 표시하고 코드그룹 조회 조건, 상세코드 목록, 계층 미리보기, 편집 폼, 저장 CTA를 렌더링하지 않는다. | listDetailCodes / createDetailCode / updateDetailCode | REQ-087 |
| SCR-DETAIL-CODE-MGMT | success | listDetailCodes 200, createDetailCode 200, updateDetailCode 200 | 지정한 groupId의 상세코드 목록이 sort_order 순서로 보이고 parent_code_value 기반 계층 미리보기가 표시된다. 저장 성공 시 `저장되었습니다` 메시지를 표시하고 listDetailCodes 재조회 결과로 변경값을 확인할 수 있다. | listDetailCodes / createDetailCode / updateDetailCode | REQ-057, REQ-058, REQ-059, REQ-060, REQ-061, REQ-087 |

#### Per-Screen UI Contract




#### Data Binding

##### Query and List Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-DETAIL-CODE-MGMT | 코드그룹 조회 입력 | groupId | listDetailCodes path `{groupId}` | detail_codes.group_id, code_groups.group_id | REQ-059 | 코드그룹별 상세코드 조회 기준이며 `SCR-CODE-GROUP-MGMT`에서 이동한 route query groupId 또는 사용자의 직접 입력을 사용한다. |
| SCR-DETAIL-CODE-MGMT | 목록 code_value column | code_value | listDetailCodes data[].codeValue | detail_codes.code_value | REQ-057, REQ-066 | 상세코드 식별자와 updateDetailCode path `{codeValue}`로 사용한다. |
| SCR-DETAIL-CODE-MGMT | 목록 code_name column | code_name | listDetailCodes data[].codeName | detail_codes.code_name | REQ-057, REQ-066 | 화면 선택값에 표시되는 사람이 읽는 코드명이다. |
| SCR-DETAIL-CODE-MGMT | 목록 parent_code_value column | parent_code_value | listDetailCodes data[].parentCodeValue | detail_codes.parent_code_value | REQ-060 | 상위코드 계층 미리보기와 parentCodeValue select option으로 사용한다. |
| SCR-DETAIL-CODE-MGMT | 목록 sort_order column | sort_order | listDetailCodes data[].sortOrder | detail_codes.sort_order | REQ-058, REQ-066 | 화면 선택값 표시 순서를 결정한다. |
| SCR-DETAIL-CODE-MGMT | 목록 system_use_yn column | system_use_yn | listDetailCodes data[].systemUseYn | detail_codes.system_use_yn | REQ-066 | 사용여부를 Y/N badge로 표시한다. |
| SCR-DETAIL-CODE-MGMT | 목록 유효기간 column | valid_start_date, valid_end_date | listDetailCodes data[].validStartDate/data[].validEndDate | detail_codes.valid_start_date, detail_codes.valid_end_date | REQ-066 | 공통코드 유효기간을 표시한다. |
| SCR-DETAIL-CODE-MGMT | 연계 매핑 속성 표시 | additional_attributes | listDetailCodes data[].additionalAttributes | detail_codes.additional_attributes | REQ-061 | 구조·개수·형식은 OQ-UI-100으로 보존하고 응답에 있는 확정 속성만 표시한다. |

##### Form Binding

| 화면ID | UI region | source field | API source | table/field | canonical_id | binding note |
|---|---|---|---|---|---|---|
| SCR-DETAIL-CODE-MGMT | 등록/수정 codeValue input | codeValue | DetailCodeRequest.codeValue and updateDetailCode path `{codeValue}` | detail_codes.code_value | REQ-057 | 신규 등록 시 editable required, 수정 시 선택 행의 code_value를 identity로 표시하고 path parameter와 일치시킨다. |
| SCR-DETAIL-CODE-MGMT | 등록/수정 codeName input | codeName | DetailCodeRequest.codeName | detail_codes.code_name | REQ-057, REQ-066 | 화면 선택값의 label로 저장하고 후속 listDetailCodes에서 확인한다. |
| SCR-DETAIL-CODE-MGMT | 등록/수정 parentCodeValue select | parentCodeValue | DetailCodeRequest.parentCodeValue | detail_codes.parent_code_value | REQ-060 | 같은 groupId의 상세코드를 상위코드 option으로 제공하거나 빈 값으로 root code를 만든다. |
| SCR-DETAIL-CODE-MGMT | 등록/수정 sortOrder input | sortOrder | DetailCodeRequest.sortOrder | detail_codes.sort_order | REQ-058, REQ-066 | 상세코드 목록과 선택값 정렬에 반영된다. |
| SCR-DETAIL-CODE-MGMT | 등록/수정 additionalAttributes input area | additionalAttributes | DetailCodeRequest.additionalAttributes | detail_codes.additional_attributes | REQ-061 | REQ-062 미확정으로 OQ-UI-100 표시; 확정 전에는 서버가 허용한 구조만 저장한다. |
| SCR-DETAIL-CODE-MGMT | 등록/수정 systemUseYn select | systemUseYn | DetailCodeRequest.systemUseYn | detail_codes.system_use_yn | REQ-066 | Y/N 값으로 공통코드 사용여부를 관리한다. |
| SCR-DETAIL-CODE-MGMT | 등록/수정 validStartDate input | validStartDate | DetailCodeRequest.validStartDate | detail_codes.valid_start_date | REQ-066 | 유효 시작일을 저장하고 목록 유효기간에 표시한다. |
| SCR-DETAIL-CODE-MGMT | 등록/수정 validEndDate input | validEndDate | DetailCodeRequest.validEndDate | detail_codes.valid_end_date | REQ-066 | 유효 종료일을 저장하거나 빈 값으로 현재 유효 상태를 나타낸다. |
| SCR-DETAIL-CODE-MGMT | 변경 사유 input | changeReason | DetailCodeRequest.changeReason | detail_codes.change_reason | REQ-057 | 등록·수정 처리 사유 추적 입력이며 서버 검증 오류를 필드 아래에 표시한다. |

#### List-Capable Screen Behavior

| 화면ID | real filters | default sort | pagination | bulk action | export | empty-result behavior |
|---|---|---|---|---|---|---|
| SCR-DETAIL-CODE-MGMT | groupId | sort_order 오름차순, 같은 sort_order에서는 code_value 표시 순서로 보조 정렬이 필요하면 OQ-UI-101로 확인한다. | listDetailCodes는 page/size query parameter를 제공하므로 page 0부터 요청하고 다음/이전 페이지 control을 표시한다. | not applicable — 원문은 상세코드 일괄 처리나 bulk update를 요구하지 않는다. | not applicable — 원문은 상세코드 export/download를 요구하지 않는다. | 선택한 groupId의 상세코드가 없으면 `선택한 코드그룹에 등록된 상세코드가 없습니다`와 [신규] CTA를 표시하고 계층 미리보기는 빈 tree로 둔다. |

#### Role / State Transition Matrix

| 화면ID | user/server condition | UI behavior | server authorization or validation | operationId 또는 path | canonical_id |
|---|---|---|---|---|---|
| SCR-DETAIL-CODE-MGMT | R09 세션이 `/admin/detail-codes`에 진입하고 groupId를 지정 | 조회 조건, 상세코드 목록, 계층 미리보기, 등록/수정 폼을 표시하고 listDetailCodes 결과를 렌더링한다. | SessionCookie와 R09 role을 요구한다. | listDetailCodes | REQ-059, REQ-087 |
| SCR-DETAIL-CODE-MGMT | 인증 세션 없음 | protected shell이 로그인 필요 상태로 전환하거나 API 401 오류를 표시한다. | 보호 API는 401 ApiError를 반환한다. | listDetailCodes / createDetailCode / updateDetailCode | REQ-087 |
| SCR-DETAIL-CODE-MGMT | 인증되었으나 R09 권한 없음 | `상세코드 관리 권한이 없습니다` 상태를 표시하고 조회·저장 CTA를 숨긴다. | 보호 API는 403 ApiError를 반환한다. | listDetailCodes / createDetailCode / updateDetailCode | REQ-087 |
| SCR-DETAIL-CODE-MGMT | R09가 유효한 신규 상세코드를 저장 | 저장 완료 메시지 후 listDetailCodes 재조회로 새 code_value가 목록과 계층 미리보기에 표시된다. | createDetailCode가 detail_codes row를 생성하고 변경 메타정보를 기록한다. | createDetailCode | REQ-057, REQ-061 |
| SCR-DETAIL-CODE-MGMT | R09가 선택 상세코드의 코드명·상위코드·정렬순서·추가속성·사용여부·유효기간을 수정 | 저장 완료 메시지 후 listDetailCodes 재조회로 선택 code_value의 변경값이 표시된다. | updateDetailCode가 detail_codes row를 갱신하고 변경 메타정보를 기록한다. | updateDetailCode | REQ-057, REQ-060, REQ-061, REQ-066 |
| SCR-DETAIL-CODE-MGMT | R09가 확정되지 않은 additionalAttributes 구조나 매핑 기준을 제출 | additionalAttributes 필드 아래 OQ-UI-100 안내와 서버 field-level 오류를 표시하고 임의 기본값을 생성하지 않는다. | createDetailCode/updateDetailCode가 400 ApiError.fields.additionalAttributes로 차단하고 기존 detail_codes row를 유지한다. | createDetailCode / updateDetailCode | REQ-061 |
| SCR-DETAIL-CODE-MGMT | R09가 systemUseYn을 N으로 바꾸거나 status 비활성화 성격의 수정을 저장 | 저장 후 목록에서 system_use_yn=N badge를 표시하고 해당 코드는 물리삭제되지 않는다. | updateDetailCode가 detail_codes.system_use_yn 또는 status 기반 비활성화를 저장한다. | updateDetailCode | REQ-066 |




## 화면별 UI 계약

| 화면ID | 사용자 목적 | 주요 데이터/입력 | 주요 CTA·확인 | 입력 검증 | 성공 후 이동 | 권한·기간·상태 제약 |
|---|---|---|---|---|---|---|
| SCR-LOGIN | 로컬 seed 관리자 또는 인증 사용자가 세션을 시작하고 현재 사용자 권한을 확인해 1차 목표 관리 메뉴 접근을 시작한다. | 입력: `loginId`, `password`; 안내 데이터: seed 계정 `admin`/`admin`; 확인 데이터: 현재 사용자, R09 시스템관리자 역할, session cookie 상태, `/api/health` 결과 | primary CTA: `[ 로그인 ]`은 `login` 호출 후 `getCurrentUser` 확인을 이어서 수행한다; support CTA: `[ 서비스 상태 확인 ]`은 `getHealth` 결과를 안내 panel에 표시한다; local CTA: README 또는 quickstart 확인 안내는 문서 위치 안내만 표시한다. | `loginId`와 `password`는 비어 있으면 서버 field-level `ApiError.fields`를 표시하고, 잘못된 credential은 401 인증 실패 메시지로 표시한다. | 성공 시 `/login`에서 protected system management shell의 최초 접근 가능 route로 이동한다. 최초 landing route는 skeleton에 고정되어 있지 않으므로 `OQ-UI-001`로 남기고, 브라우저 검증은 9개 route 접근 가능 여부로 수행한다. | 인증 전에는 anonymous만 입력 가능하다. 인증 후 R09 시스템관리자 역할 또는 1차 목표 메뉴 권한이 확인되지 않으면 protected shell 이동을 차단하고 permission 상태를 표시한다. 기간 제약은 not applicable이다. |
| SCR-USER-MGMT | R09 시스템관리자가 KORUS 원천 인사정보를 확인하면서 로컬 DB의 사용자 사용여부와 업무 역할만 관리한다. | 검색 입력: employeeNo, name, organizationCodeFilter, rankName, employmentStatus, roleCodeFilter, systemUseYn. 목록 표시: 교번, 성명, 소속, 직급, 재직상태, 역할, 사용여부, 보직, 퇴직일자, 최종 동기화일시. 상세 읽기 전용: employee_no, name, organization_code, rank_name, employment_status, position_name, retirement_date, last_synced_at. 편집 입력: systemUseYn, roleCodes, validStartDate(OQ-UI-201), validEndDate(OQ-UI-202), changeReason. | `조회`는 확인 없이 searchUsers를 호출한다. `시스템 사용여부 저장`은 선택 사용자와 systemUseYn, changeReason 확인 후 updateUserAccount를 호출한다. `업무 역할 저장`은 선택 사용자와 roleCodes, 유효기간, changeReason 확인 후 updateUserRoles를 호출한다. `취소`는 local-only로 마지막 조회 결과를 복원한다. | 서버 검증 결과 ApiError.fields를 각 입력 옆에 표시한다. KORUS 원천 필드(employee_no, name, organization_code, rank_name, employment_status, position_name, retirement_date, last_synced_at)는 편집 불가이며 payload에 포함하지 않는다. systemUseYn은 Y 또는 N, roleCodes는 R01~R09 중 선택한다. validStartDate/validEndDate의 필수 여부와 기간 규칙은 OQ-UI-201/OQ-UI-202로 보존한다. | `/admin/users`에 머물며 현재 검색조건으로 searchUsers를 재호출하고 선택 행 상세에 저장된 systemUseYn 또는 roleCodes를 표시한다. | R09 또는 사용자 관리 메뉴 권한이 없으면 화면은 permission 상태가 되고 저장 CTA가 보이지 않는다. KORUS 원천정보 수정 시도는 REQ-012에 따라 서버가 거부하고 화면은 원천 필드를 마지막 조회값으로 유지한다. |
| SCR-ORG-MGMT | R09 시스템관리자가 대학·대학원·단과대학·학과·부서를 조직코드 기준으로 찾고 계층 관계와 적용기간을 관리한다. | 검색 입력: organizationCodeFilter, organization_type. 표시 데이터: organization_code, organization_name, organization_type, system_use_yn, status, parent_organization_code, effective_start_date, effective_end_date. 저장 입력: parentOrganizationCode, effectiveStartDate, effectiveEndDate, changeReason. | `조회`는 searchOrganizations와 getOrganizationTree를 호출한다. `상위관계 저장`은 선택 조직, parentOrganizationCode, effectiveStartDate, effectiveEndDate, changeReason을 확인 dialog에 표시한 뒤 saveOrganizationParentRelation을 호출한다. `취소`는 form 변경값을 마지막 조회 상태로 되돌린다. | effectiveEndDate가 effectiveStartDate보다 빠르면 field-level error를 표시한다. 동일 조직의 상위조직 관계 적용기간 중복은 서버 ApiError를 표시하고 기존 관계를 유지한다. organizationCode는 선택 row/path parameter 원천이므로 편집하지 않는다. | `/admin/organizations` 같은 화면에서 선택 조직 상세와 조직 계층을 재조회하고 `조직 관계가 저장되었습니다` success message를 표시한다. | R09 또는 해당 leaf menu 권한이 없으면 permission state로 차단한다. effectiveEndDate < effectiveStartDate 또는 기간 중복이면 저장이 차단되고 relation/history 상태는 변경되지 않는다. |
| SCR-ROLE-MGMT | R09 시스템관리자가 `/admin/roles`에서 R01~R09 역할 목록과 역할별 목적을 확인하고 선택 역할의 기준정보를 변경한다. | 표시 데이터: role_code, role_name, purpose. 편집 입력: role_name, purpose, assignment_criteria, default_data_scope, change_reason. role_code는 readonly로 표시한다. | [조회]는 listRoles를 호출한다. 역할 row 선택은 local 상세 표시다. [저장]은 확인 modal `선택 역할 {roleCode}의 기준정보를 저장하시겠습니까?` 후 updateRole을 호출한다. [취소]는 선택 직후 상세값으로 복원한다. | roleName, purpose, assignmentCriteria, defaultDataScope, changeReason는 updateRole ApiError.fields를 필드별로 표시한다. roleCode는 path parameter로만 사용하고 입력 필드로 수정하지 않는다. | updateRole 성공 후 `/admin/roles`에 머물며 listRoles를 재호출하고 선택한 roleCode 행과 상세 패널에 최신 역할명·목적·부여 기준·데이터 범위 기본값을 표시한다. | R09가 아니거나 403이면 permission 상태로 전환하고 목록·상세·저장 CTA를 숨긴다. R01~R09 외 신규 roleCode 추가는 REQ-029 out-of-scope로 차단하며 신규 등록 버튼을 제공하지 않는다. |
| SCR-USER-ROLE-MGMT | R09 시스템관리자가 `/admin/user-roles`에서 사용자별 현재 역할과 유효기간을 조회하고 역할을 부여·변경·회수한다. | 표시 데이터: assignment_id, user_id, role_code, assignment_type, valid_start_date, valid_end_date, status, approver_user_id. 입력: userId, roleCode(R01~R09), validStartDate, validEndDate, changeReason. 승인자/처리자는 로그인 R09 관리자로 자동 기록되어 readonly 안내만 표시한다. | [조회]는 listUserRoleAssignments를 호출한다. [현재 역할 조회]는 userId로 listCurrentUserRoles를 호출한다. [역할 부여]는 확인 modal 후 assignUserRole을 호출한다. [변경 저장]은 assignmentId 선택 후 확인 modal을 거쳐 updateUserRole을 호출한다. [역할 회수]는 위험 확인 modal 후 revokeUserRole을 호출한다. [취소]는 마지막 조회/선택값으로 복원한다. | userId, roleCode, validStartDate, validEndDate, changeReason는 서버 ApiError.fields를 필드별로 표시한다. roleCode는 R01~R09 선택값만 표시하고, assignmentId는 변경·회수 path parameter로만 사용한다. OQ-UI-051 사용자 식별/검색 방식이 확정되기 전에는 userId 직접 입력 또는 목록 row 선택 중 구현자가 임의 확정하지 않는다. | assignUserRole/updateUserRole/revokeUserRole 성공 후 `/admin/user-roles`에 머물며 listUserRoleAssignments와 선택 userId의 listCurrentUserRoles를 재호출하고 최신 role_code, assignment_type, valid_start_date, valid_end_date, status를 표시한다. | R09가 아니거나 403이면 permission 상태로 전환하고 목록·폼·저장/회수 CTA를 숨긴다. revokeUserRole 성공 후 해당 row는 status=REVOKED로 표시하거나 현재 역할 목록에서 제외되어야 하며, POSITION 역할은 source-backed 구분 표시 대상이므로 수동 부여 폼에서 임의로 POSITION 생성 여부를 확정하지 않는다. |
| SCR-MENU-PERMISSION-MGMT | R09 시스템관리자가 `/admin/menu-permissions`에서 ROLE/ORGANIZATION/USER 단위의 대메뉴·중메뉴·화면 접근권한을 조회하고 선택 권한의 접근 허용 여부를 저장한다. | 검색 입력: targetType(ROLE/ORGANIZATION/USER), targetId, accessAllowed 전체/ALLOW/DENY. 표시 데이터: 대메뉴, 중메뉴, 화면, accessAllowed, 변경 상태. 저장 입력: targetType, targetId, menuId, accessAllowed(ALLOW/DENY), changeReason. | [조회]는 listMenuPermissions를 호출한다. matrix row 선택은 local 상세 표시다. [저장]은 확인 modal `대상 {targetType} {targetId}의 {menuName} 접근권한을 {accessAllowed}로 저장합니까?` 후 saveMenuPermissions를 호출한다. [취소]는 선택 row의 마지막 조회값으로 복원한다. | targetType은 ROLE/ORGANIZATION/USER만 허용하고 accessAllowed는 ALLOW/DENY만 허용한다. targetId, menuId, accessAllowed는 saveMenuPermissions 필수 입력이며 서버 ApiError.fields를 필드별로 표시한다. | saveMenuPermissions 성공 후 `/admin/menu-permissions`에 머물며 listMenuPermissions를 재호출하고 선택한 targetType/targetId의 matrix에 최신 accessAllowed와 `저장되었습니다` 메시지를 표시한다. | R09가 아니거나 403이면 permission 상태로 전환하고 검색조건·matrix·저장 CTA를 숨긴다. 권한 충돌은 USER > ORGANIZATION > ROLE, 같은 우선순위에서는 DENY가 ALLOW보다 우선한다는 서버 authorization 결과를 화면 노출과 API 접근통제에 동일하게 적용한다. |
| SCR-MENU-STRUCTURE-MGMT | R09 시스템관리자가 `/admin/menu-structure`에서 대메뉴·중메뉴·소메뉴 계층구조를 보고, 선택 메뉴의 parent_menu_id와 동일 계층 display_order를 조정한다. | 표시 데이터: menu_id(OQ-UI-071 표시 형식), menu_name, menu_type, parent_menu_id, display_order, system_use_yn/status. 입력: 새 parentMenuId, orderedMenuIds, changeReason. 메뉴명·screen_id·url·icon·business_category·description은 이 화면에서 편집하지 않는다. | [새로고침]은 getMenuTree를 호출한다. tree node 선택은 local 상세 표시다. [부모메뉴 저장]은 확인 modal `선택 메뉴의 부모메뉴를 변경하시겠습니까?` 후 updateMenuParent를 호출한다. [순서 저장]은 확인 modal `현재 parent_menu_id의 표시순서를 저장하시겠습니까?` 후 reorderMenus를 호출한다. [취소]는 마지막 조회값으로 복원한다. | parentMenuId가 선택 menu_id와 같거나 선택 메뉴의 하위 메뉴이면 updateMenuParent ApiError.fields.parentMenuId를 표시하고 저장 전 parent_menu_id를 유지한다. orderedMenuIds 누락 또는 중복은 reorderMenus ApiError.fields.orderedMenuIds를 표시한다. changeReason 누락은 updateMenuParent ApiError.fields.changeReason을 표시한다. | updateMenuParent 또는 reorderMenus 성공 후 `/admin/menu-structure`에 머물며 getMenuTree를 재호출하고 변경된 부모-자식 관계 또는 동일 계층 display_order를 tree에서 강조한다. | R09가 아니거나 403이면 permission 상태로 전환하고 tree/editor/저장 CTA를 숨긴다. 자기 자신 또는 하위 메뉴 부모 지정은 서버가 400으로 차단하며, 사용 중 메뉴 삭제나 비활성화는 이 화면의 CTA가 아니라 data/API 계약의 제약으로만 참조한다. |
| SCR-MENU-INFO-MGMT | 메뉴와 실행 화면을 연결하기 위해 메뉴별 실행정보를 조회하고 메뉴명·화면ID·URL·아이콘·업무구분·설명을 등록·수정한다. | 메뉴 목록 표시: 메뉴명, 화면ID, URL. 편집 입력: menuName, screenId, url, icon, businessCategory, description, changeReason. | 주요 CTA `조회`는 선택 메뉴의 getMenuExecution을 호출한다. 주요 CTA `저장`은 저장 확인 메시지 후 updateMenuExecution을 호출한다. `취소`는 API 호출 없이 마지막 조회값으로 폼을 되돌린다. | menuName, screenId, url은 MenuExecutionRequest required 필드이므로 비어 있으면 field-level 오류를 표시한다. icon, businessCategory, description, changeReason은 원문상 관리/추적 입력이며, 서버 ApiError.fields가 오면 해당 필드 아래에 표시한다. | `/admin/menu-info`에 머물며 같은 menuId의 getMenuExecution 재조회 결과를 실행정보 편집 영역과 메뉴 목록 행에 반영한다. | R09 권한 또는 메뉴 정보 관리 접근권한이 없으면 permission 상태로 차단한다. 사용 중 메뉴의 물리삭제 기능은 제공하지 않으며, 이 화면은 메뉴 실행정보 조회·등록·수정만 수행한다. |
| SCR-CODE-GROUP-MGMT | 평가영역·처리상태·인증구분 등 코드 묶음을 코드그룹 단위로 조회하고 그룹ID·명칭·설명·관리부서를 등록·수정하며, 선택한 코드그룹의 상세코드 목록으로 이동한다. | 검색 입력: groupIdFilter. 목록 표시: groupId, groupName, description, managingDepartment. 등록·수정 입력: groupId, groupName, description, managingDepartment, changeReason. | `조회`는 listCodeGroups를 호출한다. `신규 등록`은 빈 등록 폼을 연다. `저장`은 저장 확인 후 신규 모드에서는 createCodeGroup, 선택 행 수정 모드에서는 updateCodeGroup을 호출한다. `상세코드`는 선택 행의 groupId를 전달해 `/admin/detail-codes`로 이동한다. `취소`는 API 호출 없이 마지막 선택 또는 빈 신규 폼 상태를 해제한다. | CodeGroupRequest required 필드인 groupId, groupName, managingDepartment가 비어 있으면 field-level 오류를 표시한다. groupId 수정 가능 여부는 OQ-UI-090으로 보존하여 등록 후 수정 모드에서 임의로 변경 허용/금지를 확정하지 않고 서버 ApiError.fields를 우선 표시한다. | 저장 후 `/admin/code-groups`에 머물며 listCodeGroups 재조회 결과로 저장된 groupId 행과 groupName, description, managingDepartment를 표시한다. 상세코드 이동 성공 시 `/admin/detail-codes`에서 전달된 groupId 기준 상세코드 목록 journey로 진입한다. | R09 권한 또는 코드그룹 관리 접근권한이 없으면 permission 상태로 차단한다. 그룹ID 등록 후 수정 가능 여부가 미확정이면 OQ-UI-090 상태를 표시하고 updateCodeGroup payload는 OpenAPI CodeGroupRequest와 서버 검증 결과를 따른다. |
| SCR-DETAIL-CODE-MGMT | R09 시스템관리자가 `/admin/detail-codes`에서 코드그룹별 상세코드를 조회하고, 화면 선택값과 연계 코드 매핑에 쓰이는 상세코드의 코드값·코드명·상위코드·정렬순서·추가속성·사용여부·유효기간을 등록·수정한다. | 조회 입력: groupId. 목록/계층 표시: code_value, code_name, parent_code_value, sort_order, additional_attributes, system_use_yn, valid_start_date, valid_end_date. 편집 입력: codeValue, codeName, parentCodeValue, sortOrder, additionalAttributes(OQ-UI-100), systemUseYn, validStartDate, validEndDate, changeReason. 수정 mode에서는 path parameter codeValue가 선택 행의 code_value에서 온다. | [조회]는 groupId로 listDetailCodes를 호출한다. [신규]는 빈 등록 폼을 연다. [저장]은 확인 modal `현재 코드그룹의 상세코드를 저장하시겠습니까?` 승인 후 신규 mode에서는 createDetailCode, 선택 행 수정 mode에서는 updateDetailCode를 호출한다. [취소]는 마지막 조회값으로 복원한다. | groupId, codeValue, codeName, sortOrder는 필수다. parentCodeValue는 같은 groupId의 code_value 중에서 선택하거나 비워 둘 수 있다. additionalAttributes는 REQ-062가 미확정이므로 OQ-UI-100으로 표시하고 확정 전 임의 key/value 기본값을 만들지 않는다. validEndDate가 validStartDate보다 빠른 경우 서버 ApiError.fields가 오면 기간 필드 아래에 표시한다. | 저장 성공 후 `/admin/detail-codes`에 머물며 listDetailCodes를 재호출하고 저장된 code_value/code_name/parent_code_value/sort_order/additional_attributes/system_use_yn/valid_start_date/valid_end_date를 목록과 계층 미리보기에 반영한다. | R09가 아니거나 403이면 permission 상태로 전환하고 조회·저장 CTA를 숨긴다. 사용 중인 상세코드의 물리삭제 CTA는 제공하지 않으며 system_use_yn 또는 status 기반 비활성화 제약은 서버 authorization/validation과 data-model.md의 detail_codes 정책을 따른다. |

## Requirement-to-Screen Trace

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| null | REQ-071 | CMN-COM-AC-03 | SCR-LOGIN | /login | login / getCurrentUser | 시드 관리자가 `admin`/`admin`으로 로그인하면 현재 사용자 확인 후 R09 시스템관리자 권한이 보이고 1차 목표 9개 route 접근 검증 흐름으로 이동할 수 있다. |
| null | REQ-075 | EAC-001, EAC-002 | SCR-LOGIN | /login | login / getCurrentUser / logout | 사용자가 `loginId`와 `password`를 제출하면 로그인 성공 시 session cookie가 생기고 현재 사용자 정보가 표시되며, 세션 종료 요청 후에는 다시 인증 전 화면으로 돌아온다. |
| null | REQ-078 | LoginPage screen contract | SCR-LOGIN | /login | login | 브라우저에서 `/login`에 진입하면 사용자 ID 입력, 비밀번호 입력, `[ 로그인 ]`, 오류/성공 메시지 영역, seed 관리자 안내가 화면에 보인다. |
| null | REQ-093 | DEC-011 | SCR-LOGIN | /login | login / getCurrentUser | `admin`/`admin` 로그인 성공 후 브라우저 세션은 HttpOnly SameSite=Lax session cookie로 유지되고 `getCurrentUser` 요청이 같은 세션으로 성공한다. |
| null | REQ-094 | Seed admin account | SCR-LOGIN | /login | login | 로그인 화면의 로컬 검증 안내에서 seed 관리자 계정을 확인하고 해당 계정으로 로그인을 시도할 수 있다. |
| null | REQ-096 | Seed admin role | SCR-LOGIN | /login | getCurrentUser | 로그인 성공 후 현재 사용자 확인 결과에 R09 시스템관리자 역할이 보이고 R09가 아니면 permission 상태가 표시된다. |
| null | REQ-097 | Documentation credential notice | SCR-LOGIN | /login | N/A — README 또는 quickstart 안내 local behavior | 로그인 화면 하단 도움말에서 README 또는 quickstart에 실행 방법과 로그인 계정이 명시된다는 안내를 확인할 수 있다. |
| null | REQ-098 | CMN-COM-AC-02 | SCR-LOGIN | /login | login | 사용자가 사용자 ID `admin`과 비밀번호 `admin`을 입력하고 `[ 로그인 ]`을 누르면 인증 성공 메시지와 후속 현재 사용자 확인 결과가 보인다. |
| null | REQ-101 | CMN-COM-AC-05 | SCR-LOGIN | /login | getHealth 또는 GET /api/health | 사용자가 `[ 서비스 상태 확인 ]`을 누르면 `/api/health` 200 상태가 로컬 검증 안내 panel에 표시되고 실패 시 상태 오류가 같은 panel에 표시된다. |
| null | REQ-102 | CMN-DEL-AC-03 | SCR-LOGIN | /login | N/A — documentation reference local behavior | 로그인 화면의 README 또는 quickstart 안내를 통해 실행 방법, 로그인 계정, 주요 화면 검증 방법이 별도 문서에서 확인되어야 함을 사용자가 볼 수 있다. |
| CMN-FR-001 | REQ-010 | CMN-FR-001-AC-01 | SCR-USER-MGMT | /admin/users | searchUsers | R09 관리자가 교번·성명·소속·직급·재직상태·역할·사용여부 조건을 입력하고 `조회`를 누르면, 조건에 맞는 사용자 목록이 화면에 표시된다. |
| CMN-FR-001 | REQ-011 | CMN-FR-001-AC-02 | SCR-USER-MGMT | /admin/users | searchUsers | 사용자 목록이 표시될 때 각 행에서 보직, 퇴직일자, 최종 동기화일시 열을 확인할 수 있다. |
| CMN-FR-001 | REQ-013 | CMN-FR-001-AC-03 | SCR-USER-MGMT | /admin/users | updateUserAccount | R09 관리자가 선택 사용자 상세에서 시스템 사용여부와 변경 사유를 입력하고 `시스템 사용여부 저장`을 누르면, 성공 메시지 후 재조회된 목록과 상세에 변경된 systemUseYn이 보인다. |
| CMN-FR-001 | REQ-014 | CMN-FR-001-AC-04 | SCR-USER-MGMT | /admin/users | updateUserRoles | R09 관리자가 선택 사용자 상세에서 R01~R09 업무 역할을 선택하고 `업무 역할 저장`을 누르면, 성공 메시지 후 선택 행의 역할 표시와 상세 roleCodes가 저장값으로 갱신된다. |
| CMN-FR-001 | REQ-015 | screen-state | SCR-USER-MGMT | /admin/users | searchUsers / updateUserAccount / updateUserRoles | 브라우저 사용자가 `/admin/users`에 진입하면 로딩, 빈 결과, 오류, 권한 없음, 저장 성공 상태가 각각 사용자 관리 화면의 검색조건·목록·상세/편집 영역에서 관찰된다. |
| null | REQ-079 | frontend-minimum-screen | SCR-USER-MGMT | /admin/users | searchUsers | R09 관리자가 시스템 관리 > 사용자·조직 관리 > 사용자 관리 메뉴를 선택하면 사용자 관리 화면이 열리고 검색조건, 목록, 선택 행 상세/편집 폼, 저장/취소 버튼이 보인다. |
| CMN-FR-002 | REQ-016 | CMN-FR-002-AC-01 | SCR-ORG-MGMT | /admin/organizations | searchOrganizations / GET /api/admin/organizations | 관리자가 조직코드 검색조건으로 조회하면 대학·대학원·단과대학·학과·부서 구분과 organization_code가 목록에 보인다. |
| CMN-FR-002 | REQ-019 | CMN-FR-002-AC-01 | SCR-ORG-MGMT | /admin/organizations | searchOrganizations / GET /api/admin/organizations | 관리자가 organizationCodeFilter에 조직코드를 입력하고 조회하면 해당 조직코드에 맞는 조직명과 조직유형이 목록에 표시된다. |
| CMN-FR-002 | REQ-020 | CMN-FR-002-AC-02 | SCR-ORG-MGMT | /admin/organizations | getOrganizationTree / GET /api/admin/organizations/tree | 관리자가 조직 관리 화면에 진입하거나 다시 조회를 누르면 조직의 상위·하위 관계가 tree node 들로 표시된다. |
| CMN-FR-002 | REQ-021 | CMN-FR-002-AC-03 | SCR-ORG-MGMT | /admin/organizations | saveOrganizationParentRelation / PUT /api/admin/organizations/{organizationCode}/parent-relations | 관리자가 조직을 선택하고 parentOrganizationCode, effectiveStartDate, effectiveEndDate, changeReason을 입력해 저장하면 성공 메시지와 갱신된 tree에서 새 상위조직 관계가 보인다. |
| null | REQ-080 | 화면 최소 요구 | SCR-ORG-MGMT | /admin/organizations | searchOrganizations / getOrganizationTree / saveOrganizationParentRelation | R09 관리자가 `/admin/organizations` route를 열면 검색조건, 조직 목록, 조직 계층, 선택 조직 관계 편집 form, 저장/취소, loading/empty/error/permission/success 상태가 화면에 보인다. |
| CMN-FR-005 | REQ-026 | CMN-FR-005-AC-01 | SCR-ROLE-MGMT | /admin/roles | listRoles | R09 관리자가 `/admin/roles`에 진입하거나 [조회]를 누르면 R01~R09 역할 목록이 표시되고 각 행에서 역할별 목적을 읽을 수 있다. |
| null | REQ-081 | SCR-ROLE-MGMT-state-AC-01 | SCR-ROLE-MGMT | /admin/roles | listRoles / updateRole | R09 관리자가 역할 관리 화면을 열면 loading 후 목록·상세/편집 폼·저장/취소 버튼이 보이고, updateRole 성공 시 `저장되었습니다`가 표시되며, ApiError와 403 응답은 각각 오류 안내와 권한 없음 상태로 표시된다. |
| CMN-FR-006 | REQ-030 | CMN-FR-006-AC-02 | SCR-USER-ROLE-MGMT | /admin/user-roles | assignUserRole | R09 관리자가 대상 user_id와 R01~R09 roleCode, validStartDate, validEndDate, changeReason를 입력하고 [역할 부여] 확인을 승인하면 목록과 현재 역할 재조회에서 새 role_code와 유효기간이 보이고 승인자/처리자는 로그인 관리자 자동 기록 안내로 표시된다. |
| CMN-FR-006 | REQ-031 | CMN-FR-006-AC-03 | SCR-USER-ROLE-MGMT | /admin/user-roles | updateUserRole | R09 관리자가 기존 assignment_id row를 선택해 role_code 또는 유효기간을 수정하고 [변경 저장] 확인을 승인하면 재조회된 목록에서 해당 assignment_id의 변경된 역할 정보와 유효기간이 표시된다. |
| CMN-FR-006 | REQ-032 | CMN-FR-006-AC-04 | SCR-USER-ROLE-MGMT | /admin/user-roles | revokeUserRole | R09 관리자가 기존 assignment_id row를 선택해 [역할 회수] 위험 확인을 승인하면 재조회 결과에서 해당 row가 status=REVOKED로 보이거나 현재 역할 목록에서 제외되고 `회수 처리되었습니다` 메시지가 표시된다. |
| CMN-FR-006 | REQ-033 | CMN-FR-006-AC-05 | SCR-USER-ROLE-MGMT | /admin/user-roles | listUserRoleAssignments / listCurrentUserRoles | R09 관리자가 사용자 역할 목록 또는 선택 user_id의 현재 역할을 조회하면 각 역할 행에 assignment_type이 POSITION 또는 MANUAL badge로 표시되어 보직 기반 역할과 수동 부여 역할을 구분할 수 있다. |
| CMN-FR-006 | REQ-034 | CMN-FR-006-AC-01 | SCR-USER-ROLE-MGMT | /admin/user-roles | listCurrentUserRoles 또는 GET /api/admin/users/{userId}/roles | R09 관리자가 user_id를 기준으로 [현재 역할 조회]를 누르면 선택 사용자에게 현재 적용되는 role_code와 valid_start_date, valid_end_date가 현재 역할 영역에 표시된다. |
| null | REQ-082 | SCR-USER-ROLE-MGMT-state-AC-01 | SCR-USER-ROLE-MGMT | /admin/user-roles | listUserRoleAssignments / assignUserRole / updateUserRole / revokeUserRole | R09 관리자가 사용자 역할 관리 화면을 열면 loading 후 검색조건·목록·현재 역할·부여/변경/회수 폼·저장/취소 버튼이 보이고, 성공 시 `처리되었습니다`, ApiError 시 필드 오류, 403 시 권한 없음 상태가 표시된다. |
| CMN-FR-007 | REQ-038 | CMN-FR-007-AC-01 | SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | listMenuPermissions | R09 관리자가 targetType=ROLE과 targetId=R09로 [조회]를 누르면 역할별 대메뉴·중메뉴·화면 접근 여부가 matrix에 표시된다. |
| CMN-FR-007 | REQ-039 | CMN-FR-007-AC-02 | SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | listMenuPermissions | R09 관리자가 targetType=ORGANIZATION과 조직 식별자로 [조회]를 누르면 조직별 대메뉴·중메뉴·화면 접근 여부가 matrix에 표시된다. |
| CMN-FR-007 | REQ-040 | CMN-FR-007-AC-03 | SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | listMenuPermissions | R09 관리자가 targetType=USER와 사용자 식별자로 [조회]를 누르면 사용자별 대메뉴·중메뉴·화면 접근 여부가 matrix에 표시된다. |
| CMN-FR-007 | REQ-036 | CMN-FR-007-AC-04 | SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | saveMenuPermissions | R09 관리자가 matrix에서 대상 메뉴의 accessAllowed를 ALLOW 또는 DENY로 변경하고 확인 modal에서 저장하면 해당 targetType, targetId, menuId의 접근 권한 설정값이 저장되고 재조회된 matrix에 최신 값이 표시된다. |
| null | REQ-083 | SCR-MENU-PERMISSION-MGMT-state-AC-01 | SCR-MENU-PERMISSION-MGMT | /admin/menu-permissions | listMenuPermissions / saveMenuPermissions | R09 관리자가 메뉴 권한 관리 화면을 열면 loading 후 검색조건·matrix·상세/저장 패널·저장/취소 버튼이 보이고, saveMenuPermissions 성공 시 `저장되었습니다`가 표시되며, ApiError와 403 응답은 각각 오류 안내와 권한 없음 상태로 표시된다. |
| CMN-FR-013 | REQ-044 | CMN-FR-013-AC-02 | SCR-MENU-STRUCTURE-MGMT | /admin/menu-structure | updateMenuParent | 관리자가 tree에서 `메뉴 구조 관리` node를 선택하고 새 부모메뉴를 지정한 뒤 [부모메뉴 저장] 확인을 승인하면, 저장 후 재조회된 tree에서 선택 메뉴가 지정한 부모 아래에 표시된다. |
| CMN-FR-013 | REQ-045 | CMN-FR-013-AC-03 | SCR-MENU-STRUCTURE-MGMT | /admin/menu-structure | reorderMenus | 관리자가 동일 parent_menu_id 아래 형제 메뉴의 순서를 위/아래 이동 또는 drag handle로 바꾸고 [순서 저장] 확인을 승인하면, 저장 후 재조회된 tree에서 해당 형제 메뉴가 변경된 display_order 순서로 보인다. |
| CMN-FR-013 | REQ-046 | CMN-FR-013-AC-01 | SCR-MENU-STRUCTURE-MGMT | /admin/menu-structure | getMenuTree | 관리자가 `/admin/menu-structure`에 진입하거나 [새로고침]을 누르면, 대메뉴·중메뉴·소메뉴가 부모-자식 indentation과 동일 계층 display_order 순서로 표시된다. |
| null | REQ-084 | CMN-SCREEN-MENU-STRUCTURE | SCR-MENU-STRUCTURE-MGMT | /admin/menu-structure | getMenuTree / updateMenuParent / reorderMenus | R09 관리자가 sidebar의 `시스템 관리 > 메뉴 관리 > 메뉴 구조 관리`를 선택하면 `/admin/menu-structure` 화면이 열리고 loading/empty/error/permission/success 상태와 tree editor CTA가 화면에서 확인된다. |
| CMN-FR-014 | REQ-049 | CMN-FR-014-AC-02/03 | SCR-MENU-INFO-MGMT | /admin/menu-info | getMenuExecution / updateMenuExecution | R09 관리자가 메뉴 정보 관리 화면에서 메뉴명·화면ID·URL·아이콘·업무구분·설명을 입력하고 저장하면 성공 메시지 후 재조회된 실행정보 편집 영역에 같은 값이 보인다. |
| CMN-FR-014 | REQ-050 | CMN-FR-014-AC-01 | SCR-MENU-INFO-MGMT | /admin/menu-info | getMenuExecution | R09 관리자가 메뉴 행을 선택해 실행정보 조회를 요청하면 메뉴명·화면ID·URL·아이콘·업무구분·설명이 편집 폼과 메뉴 목록 행에 표시된다. |
| CMN-FR-014 | REQ-051 | CMN-FR-014-AC-02/03 | SCR-MENU-INFO-MGMT | /admin/menu-info | updateMenuExecution | R09 관리자가 메뉴명·화면ID·URL 필수값과 아이콘·업무구분·설명 수정값을 저장하면 updateMenuExecution 성공 후 같은 menuId 재조회에서 수정값이 유지되어 보인다. |
| CMN-FR-014 | REQ-052 | CMN-FR-014-AC-04 | SCR-MENU-INFO-MGMT | /admin/menu-info | getMenuExecution / updateMenuExecution | R09 관리자가 저장된 화면ID와 URL을 확인한 뒤 메뉴 클릭 동작을 실행하면 연결된 화면ID와 URL이 일치하는 이동 대상 또는 실행 식별 결과가 보인다. |
| null | REQ-085 | 화면 최소 요구: 메뉴 정보 관리 화면 | SCR-MENU-INFO-MGMT | /admin/menu-info | getMenuExecution / updateMenuExecution | R09 관리자가 `/admin/menu-info`에 접근하면 메뉴 정보 관리 화면의 검색조건, 목록 테이블, 선택 행 편집 폼, 저장/취소 버튼, 성공/오류/권한 없음/로딩 상태를 브라우저에서 확인할 수 있다. |
| CMN-FR-016 | REQ-053 | CMN-FR-016-AC-02/03 | SCR-CODE-GROUP-MGMT | /admin/code-groups | createCodeGroup / updateCodeGroup | R09 관리자가 코드그룹 관리 화면에서 그룹ID·명칭·설명·관리부서를 입력하고 저장하면 성공 메시지 후 목록 재조회에서 같은 groupId 행의 명칭·설명·관리부서가 표시된다. |
| CMN-FR-016 | REQ-054 | CMN-FR-016-AC-01 | SCR-CODE-GROUP-MGMT | /admin/code-groups | listCodeGroups | R09 관리자가 코드그룹 목록 조회를 요청하면 목록에 그룹ID·명칭·설명·관리부서가 행 단위로 표시되고 결과가 없을 때는 빈 결과 안내가 보인다. |
| CMN-FR-016 | REQ-055 | CMN-FR-016-AC-04 | SCR-CODE-GROUP-MGMT | /admin/code-groups | listCodeGroups | R09 관리자가 코드그룹 목록 행의 `상세코드`를 클릭하면 선택한 groupId가 이동 context로 유지되고 `/admin/detail-codes` 상세코드 목록 journey로 이동한다. |
| null | REQ-086 | 화면 최소 요구: 코드그룹 관리 화면 | SCR-CODE-GROUP-MGMT | /admin/code-groups | listCodeGroups / createCodeGroup / updateCodeGroup | R09 관리자가 `/admin/code-groups`에 접근하면 코드그룹 관리 화면의 검색조건, 목록 테이블, 선택 행 등록·수정 폼, 저장/취소 버튼, 성공/오류/권한 없음/로딩 상태를 브라우저에서 확인할 수 있다. |
| CMN-FR-017 | REQ-057 | CMN-FR-017-AC-02/03 | SCR-DETAIL-CODE-MGMT | /admin/detail-codes | createDetailCode / updateDetailCode | R09 관리자가 groupId를 지정하고 codeValue, codeName, parentCodeValue, sortOrder, additionalAttributes, systemUseYn, validStartDate, validEndDate를 입력해 저장하면 성공 메시지 후 재조회된 상세코드 목록에 같은 code_value와 code_name 및 저장 필드가 보인다. |
| CMN-FR-017 | REQ-058 | CMN-FR-017-AC-01/04 | SCR-DETAIL-CODE-MGMT | /admin/detail-codes | listDetailCodes | R09 관리자가 groupId로 상세코드 조회를 실행하면 화면 선택값으로 사용할 code_value, code_name, sort_order와 연계 매핑 속성 표시 영역이 목록에 보인다. |
| CMN-FR-017 | REQ-059 | CMN-FR-017-AC-01 | SCR-DETAIL-CODE-MGMT | /admin/detail-codes | listDetailCodes | R09 관리자가 `/admin/detail-codes`에서 groupId를 입력하고 [조회]를 누르면 해당 코드그룹의 상세코드 목록이 code_value, code_name, parent_code_value, sort_order, system_use_yn, 유효기간 columns로 표시된다. |
| CMN-FR-017 | REQ-060 | CMN-FR-017-AC-04 | SCR-DETAIL-CODE-MGMT | /admin/detail-codes | listDetailCodes / createDetailCode / updateDetailCode | R09 관리자가 parentCodeValue를 지정해 상세코드를 저장하면 성공 후 재조회된 상위코드 계층 미리보기에서 저장한 code_value가 지정한 parent_code_value 아래에 표시된다. |
| CMN-FR-017 | REQ-061 | CMN-FR-017-AC-02/03/04 | SCR-DETAIL-CODE-MGMT | /admin/detail-codes | createDetailCode / updateDetailCode / listDetailCodes | R09 관리자가 서버가 허용한 additionalAttributes를 포함해 상세코드를 저장하면 성공 후 listDetailCodes 재조회에서 연계 코드 매핑 속성 표시 영역에 저장된 additional_attributes가 보이고, 미확정 구조는 field-level 오류로 차단된다. |
| null | REQ-087 | 화면 최소 요구: 상세코드 관리 화면 | SCR-DETAIL-CODE-MGMT | /admin/detail-codes | listDetailCodes / createDetailCode / updateDetailCode | R09 관리자가 sidebar의 `시스템 관리 > 공통코드 관리 > 상세코드 관리`를 선택하면 `/admin/detail-codes` 화면이 열리고 검색조건, 목록 테이블, 선택 행 등록/수정 폼, 저장/취소 버튼, 성공/오류/권한 없음/로딩 상태를 브라우저에서 확인할 수 있다. |

## 요구 회계

| canonical_id | status | evidence |
|---|---|---|
| REQ-001 | non-functional | required_outputs 구조는 plan.md와 후속 architecture/quickstart에서 검증하고 UI skeleton은 route naming만 참조한다. |
| REQ-002 | covered | searchUsers |
| REQ-003 | data-constraint | users/roles/menus/menu_permissions/code_groups/detail_codes local DB 관리 |
| REQ-004 | out-of-scope | 1차 범위 밖 화면/API는 업무 기능으로 노출하지 않고 placeholder 여부만 보존한다. |
| REQ-005 | out-of-scope | 교수업적평가·학술지원금 종속 업무 화면과 API는 이번 UI skeleton route inventory에 넣지 않는다. |
| REQ-006 | out-of-scope | 파일·Excel·개인정보·접속기록·감사로그·배치 메뉴는 Menu Information Architecture에서 제외한다. |
| REQ-007 | out-of-scope | 범위 밖 placeholder 기준은 확인 필요이므로 이 skeleton은 9개 실제 업무 leaf만 확정한다. |
| REQ-008 | out-of-scope | 감사로그 제외와 변경 추적 경계가 미확정이므로 별도 감사로그 화면을 만들지 않는다. |
| REQ-009 | out-of-scope | 조직 변경 이력 조회 보존 단위가 미확정이므로 조직 관리 screen skeleton에만 OQ로 연결한다. |
| REQ-012 | covered | updateUserAccount / updateUserRoles |
| REQ-017 | data-constraint | organization_relations.effectiveStartDate/effectiveEndDate |
| REQ-018 | data-constraint | organization_relation_history |
| REQ-022 | covered | saveOrganizationParentRelation |
| REQ-023 | covered | saveOrganizationParentRelation |
| REQ-024 | data-constraint | seed-data.md의 R01~R09 role seed fixture가 역할 정의를 검증한다 |
| REQ-025 | data-constraint | roles.purpose/assignmentCriteria/defaultDataScope |
| REQ-027 | data-constraint | roles assignmentCriteria/defaultDataScope update fields |
| REQ-028 | covered | updateRole |
| REQ-029 | out-of-scope | R01~R09 외 신규 역할코드 추가 screen이나 CTA는 skeleton에 추가하지 않는다. |
| REQ-035 | data-constraint | user_roles.approverUserId processor metadata |
| REQ-037 | covered | saveMenuPermissions |
| REQ-041 | covered | listMenuPermissions / saveMenuPermissions |
| REQ-042 | covered | saveMenuPermissions |
| REQ-043 | covered | listMenuPermissions / saveMenuPermissions |
| REQ-047 | covered | updateMenuParent |
| REQ-048 | covered | updateMenuParent |
| REQ-056 | out-of-scope | groupId 수정 가능 여부가 미확정이므로 코드그룹 화면 상세 계약에서 후속 OQ로 처리한다. |
| REQ-062 | out-of-scope | additionalAttributes 구조가 미확정이므로 상세코드 화면 상세 계약에서 임의 필드를 만들지 않는다. |
| REQ-063 | covered | Permission-Specific Behavior가 역할·조직·사용자 단위 menu 권한과 server authorization을 화면/API 양쪽에 적용한다. |
| REQ-065 | covered | 공통 shell의 후속 화면 계약은 변경성 저장 CTA와 변경 사유를 함께 전달하고, API 오류 상태를 화면에 연결한다. |
| REQ-066 | covered | Route Inventory와 Menu Information Architecture가 코드그룹·상세코드 관리 화면을 포함하고 코드값 관리 영역을 분리한다. |
| REQ-067 | covered | Permission-Specific Behavior와 공통 관리 화면 범위는 사용 중지/비활성화 중심의 상태 관리 정책을 후속 화면 계약에 전달한다. |
| REQ-068 | covered | Common Shell이 검색조건·목록·상세/편집·message·loading/empty/error/permission/success 상태를 수용하는 반응형 content region을 정의한다. |
| REQ-064 | data-constraint | users/roles/menus/menu_permissions/code_groups/detail_codes write tables |
| REQ-069 | covered | mutating operations use ApiError field validation |
| REQ-070 | covered | frontend fetch contract uses relative /api paths |
| REQ-072 | data-constraint | Entity Registry physical table inventory |
| REQ-073 | data-constraint | createdAt/updatedAt/use/status common fields |
| REQ-074 | data-constraint | status/inactivation logical delete policy |
| REQ-076 | covered | all protected operations declare SessionCookie |
| REQ-077 | covered | protected operations return 401/403 ApiError |
| REQ-088 | non-functional | Backend Java 17/Spring Boot/MyBatis/PostgreSQL 계약은 UI skeleton의 공통 계약 참조에만 기록한다. |
| REQ-089 | non-functional | React 18/TypeScript/Vite/nginx 계약은 Design Reference와 공통 계약 참조에 반영한다. |
| REQ-090 | non-functional | Docker Compose와 Flyway 계약은 UI 문서가 아닌 quickstart/architecture 검증 대상으로 남긴다. |
| REQ-091 | non-functional | 외부 KORUS/SSO/API 미접속 조건은 shell 오류/연계 실패 표시 원칙으로만 참조한다. |
| REQ-092 | non-functional | AuthenticationPort/PersonnelInformationPort 교체 가능성은 UI route나 menu 이름을 바꾸지 않는다. |
| REQ-095 | non-functional | Compose 직후 로그인 가능 조건은 login route validation에서 후속 quickstart가 검증한다. |
| REQ-099 | data-constraint | seed-data admin/R01~R09/menu/permission/example rows |
| REQ-100 | non-functional | backend/frontend/database 실행 여부는 UI skeleton이 아니라 Docker Compose smoke 증거로 검증한다. |
| REQ-071 | covered | SCR-LOGIN Requirement-to-Screen Trace에서 `admin` 로그인 후 R09 현재 사용자 확인과 1차 목표 9개 route 접근 검증 흐름을 명시한다. |
| REQ-075 | covered | SCR-LOGIN action mapping `ACT-LOGIN-001`, `ACT-LOGIN-002`, `ACT-LOGIN-005`가 `login`, `getCurrentUser`, `logout` 인증 흐름을 화면 동작으로 연결한다. |
| REQ-078 | covered | SCR-LOGIN Screen Inventory와 Wireframe이 `/login` 인증 화면의 입력, CTA, 메시지, seed 안내 영역을 정의한다. |
| REQ-093 | covered | SCR-LOGIN Data Binding의 session cookie row가 HttpOnly SameSite=Lax와 local/운영 Secure 차이를 표시한다. |
| REQ-094 | covered | SCR-LOGIN Wireframe의 로컬 검증 안내 panel이 seed 관리자 계정 제공을 사용자에게 노출한다. |
| REQ-096 | covered | SCR-LOGIN Role/State Transition Matrix가 R09 시스템관리자 확인 성공과 비R09 permission 차단을 분리한다. |
| REQ-097 | covered | SCR-LOGIN action mapping `ACT-LOGIN-004`와 Data Binding의 README 또는 quickstart 안내 row가 계정 정보 문서 명시를 화면 도움말로 연결한다. |
| REQ-098 | covered | SCR-LOGIN Requirement-to-Screen Trace의 REQ-098 row가 사용자 ID `admin`과 비밀번호 `admin` 제출 후 인증 성공 표시를 브라우저 검증으로 지정한다. |
| REQ-101 | covered | SCR-LOGIN action mapping `ACT-LOGIN-003`이 `[ 서비스 상태 확인 ]`과 `getHealth` 또는 `GET /api/health` 200 표시를 연결한다. |
| REQ-102 | covered | SCR-LOGIN Requirement-to-Screen Trace의 REQ-102 row가 README 또는 quickstart의 실행 방법·로그인 계정·주요 화면 검증 방법 안내를 화면 도움말 검증으로 지정한다. |
| REQ-010 | covered | SCR-USER-MGMT Screen Inventory, ACT-USER-SEARCH, Requirement-to-Screen Trace CMN-FR-001-AC-01이 searchUsers 사용자 조건 조회를 다룬다. |
| REQ-011 | covered | SCR-USER-MGMT Wireframe의 사용자 목록 열과 Data Binding의 position_name/retirement_date/last_synced_at 행이 목록 표시 항목을 다룬다. |
| REQ-013 | covered | ACT-USER-SAVE-ACCOUNT와 UI States success 행이 updateUserAccount 후 systemUseYn 재조회 표시를 다룬다. |
| REQ-014 | covered | ACT-USER-SAVE-ROLES와 Data Binding의 roleCodes/validStartDate/validEndDate 행이 updateUserRoles 업무 역할 변경을 다룬다. |
| REQ-015 | covered | UI States: SCR-USER-MGMT와 Per-Screen UI Contract가 검색조건, 목록, 상세/편집, 저장/취소, 성공/오류, 권한 없음, 로딩 상태를 다룬다. |
| REQ-079 | covered | SCR-USER-MGMT Screen Inventory와 Requirement-to-Screen Trace frontend-minimum-screen 행이 사용자 관리 화면 route `/admin/users`를 다룬다. |
| REQ-016 | covered | SCR-ORG-MGMT screen inventory, 조직 목록 organization_code/organization_type binding, ACT-ORG-SEARCH |
| REQ-019 | covered | SCR-ORG-MGMT organizationCodeFilter 조회 flow와 Requirement-to-Screen Trace CMN-FR-002-AC-01 |
| REQ-020 | covered | SCR-ORG-MGMT 조직 계층 wireframe 및 ACT-ORG-TREE-LOAD |
| REQ-021 | covered | SCR-ORG-MGMT 관계 편집 form과 ACT-ORG-SAVE-RELATION |
| REQ-080 | covered | SCR-ORG-MGMT per-screen UI contract와 loading/empty/error/permission/success state table |
| REQ-026 | covered | SCR-ROLE-MGMT screen inventory, ACT-ROLE-LIST-QUERY, listRoles, Requirement-to-Screen Trace CMN-FR-005-AC-01이 역할 목록과 목적 조회를 검증한다. |
| REQ-081 | covered | SCR-ROLE-MGMT Screen States, Per-Screen UI Contract, ACT-ROLE-SAVE, SCR-ROLE-MGMT-state-AC-01이 역할 관리 화면의 loading/empty/error/permission/success와 저장/취소 동작을 검증한다. |
| REQ-030 | covered | SCR-USER-ROLE-MGMT screen inventory, ACT-USER-ROLE-ASSIGN, assignUserRole, Requirement-to-Screen Trace CMN-FR-006-AC-02가 사용자 역할 부여와 유효기간 기록을 검증한다. |
| REQ-031 | covered | SCR-USER-ROLE-MGMT form binding, ACT-USER-ROLE-UPDATE, updateUserRole, Requirement-to-Screen Trace CMN-FR-006-AC-03이 부여된 역할 변경과 유효기간 갱신을 검증한다. |
| REQ-032 | covered | Role/State Transition Matrix, ACT-USER-ROLE-REVOKE, revokeUserRole, Requirement-to-Screen Trace CMN-FR-006-AC-04가 사용자 역할 회수와 REVOKED 표시를 검증한다. |
| REQ-033 | covered | 사용자 역할 목록 binding, POSITION/MANUAL badge wireframe, listUserRoleAssignments/listCurrentUserRoles trace가 보직 기반 역할과 수동 역할 구분 표시를 검증한다. |
| REQ-034 | covered | ACT-USER-ROLE-CURRENT-QUERY, listCurrentUserRoles, 현재 역할 영역 wireframe, Requirement-to-Screen Trace CMN-FR-006-AC-01이 사용자별 현재 역할과 유효기간 조회를 검증한다. |
| REQ-082 | covered | SCR-USER-ROLE-MGMT Screen States, Per-Screen UI Contract, Navigation and Sequence Flow, SCR-USER-ROLE-MGMT-state-AC-01이 사용자 역할 관리 화면의 loading/empty/error/permission/success와 저장/취소 동작을 검증한다. |
| REQ-036 | covered | SCR-MENU-PERMISSION-MGMT screen inventory, ACT-MENU-PERMISSION-SAVE, saveMenuPermissions, Requirement-to-Screen Trace CMN-FR-007-AC-04가 target/menu accessAllowed 저장과 재조회 표시를 검증한다. |
| REQ-038 | covered | SCR-MENU-PERMISSION-MGMT Screen States, ACT-MENU-PERMISSION-QUERY, listMenuPermissions, Requirement-to-Screen Trace CMN-FR-007-AC-01이 ROLE 대상 메뉴 권한 조회를 검증한다. |
| REQ-039 | covered | SCR-MENU-PERMISSION-MGMT Per-Screen UI Contract와 Requirement-to-Screen Trace CMN-FR-007-AC-02가 ORGANIZATION 대상 메뉴 권한 matrix 조회를 검증한다. |
| REQ-040 | covered | SCR-MENU-PERMISSION-MGMT Role/State Transition Matrix와 Requirement-to-Screen Trace CMN-FR-007-AC-03이 USER 대상 메뉴 권한 matrix 조회를 검증한다. |
| REQ-083 | covered | SCR-MENU-PERMISSION-MGMT Screen States, Per-Screen UI Contract, SCR-MENU-PERMISSION-MGMT-state-AC-01이 메뉴 권한 관리 화면의 loading/empty/error/permission/success와 저장/취소 동작을 검증한다. |
| REQ-044 | covered | SCR-MENU-STRUCTURE-MGMT Per-Screen UI Contract와 ACT-MENU-STRUCTURE-PARENT-SAVE가 updateMenuParent로 parent_menu_id 변경을 처리한다. |
| REQ-045 | covered | SCR-MENU-STRUCTURE-MGMT List/Tree Behavior Contract와 ACT-MENU-STRUCTURE-REORDER-SAVE가 reorderMenus로 display_order 재정렬을 처리한다. |
| REQ-046 | covered | SCR-MENU-STRUCTURE-MGMT Screen States와 ACT-MENU-STRUCTURE-LOAD가 getMenuTree로 대메뉴·중메뉴·소메뉴 계층을 표시한다. |
| REQ-084 | covered | Screen Inventory의 SCR-MENU-STRUCTURE-MGMT `/admin/menu-structure` row와 Requirement-to-Screen Trace가 메뉴 구조 관리 화면 자체를 정의한다. |
| REQ-049 | covered | SCR-MENU-INFO-MGMT 편집 폼과 ACT-MENU-INFO-003 updateMenuExecution 저장 흐름이 메뉴명·화면ID·URL·아이콘·업무구분·설명 관리 요구를 다룬다. |
| REQ-050 | covered | ACT-MENU-INFO-001/002 getMenuExecution 조회와 Data Binding의 메뉴명·화면ID·URL 표시가 메뉴별 실행정보 조회를 검증한다. |
| REQ-051 | covered | Per-Screen UI Contract의 필수/선택 입력과 ACT-MENU-INFO-003 updateMenuExecution 성공·오류 상태가 등록·수정 요구를 검증한다. |
| REQ-052 | covered | ACT-MENU-INFO-005 navigation-only 확인과 Requirement-to-Screen Trace의 저장된 screenId/url 표시가 메뉴와 실행 화면 연결을 검증한다. |
| REQ-085 | covered | Screen Inventory, Wireframe, Screen States, Per-Screen UI Contract가 `/admin/menu-info` 메뉴 정보 관리 화면 자체를 정의한다. |
| REQ-053 | covered | SCR-CODE-GROUP-MGMT 등록·수정 폼과 ACT-CODE-GROUP-004/005 createCodeGroup/updateCodeGroup 저장 흐름이 그룹ID·명칭·설명·관리부서 저장 요구를 다룬다. |
| REQ-054 | covered | ACT-CODE-GROUP-001 listCodeGroups 조회와 Data Binding의 그룹ID·명칭·설명·관리부서 목록 표시가 코드그룹 조회를 검증한다. |
| REQ-055 | covered | ACT-CODE-GROUP-006 navigation-only 상세코드 이동과 Requirement-to-Screen Trace의 groupId 전달이 코드그룹별 상세코드 목록 연결을 검증한다. |
| REQ-086 | covered | Screen Inventory, Wireframe, Screen States, Per-Screen UI Contract가 `/admin/code-groups` 코드그룹 관리 화면 자체를 정의한다. |
| REQ-057 | covered | SCR-DETAIL-CODE-MGMT Per-Screen UI Contract와 ACT-DETAIL-CODE-CREATE/UPDATE가 createDetailCode/updateDetailCode로 code_value, code_name, parent_code_value, sort_order, additional_attributes 관리 흐름을 다룬다. |
| REQ-058 | covered | Query and List Binding의 sort_order 및 additional_attributes 표시와 ACT-DETAIL-CODE-LOAD가 listDetailCodes 조회 결과를 화면 선택값과 연계 코드 매핑 표시로 제공한다. |
| REQ-059 | covered | SCR-DETAIL-CODE-MGMT Screen States와 ACT-DETAIL-CODE-LOAD가 groupId 기준 listDetailCodes 조회 목록을 정의한다. |
| REQ-060 | covered | Wireframe의 상위코드 계층 미리보기와 parentCodeValue Form Binding이 parent_code_value 기반 계층 관리를 화면에서 검증한다. |
| REQ-061 | covered | Form Binding의 additionalAttributes 영역과 Role / State Transition Matrix의 OQ-UI-100 차단 행이 연계 코드 매핑용 속성 저장·반환 처리를 검증한다. |
| REQ-087 | covered | Screen Inventory의 SCR-DETAIL-CODE-MGMT `/admin/detail-codes` row와 Requirement-to-Screen Trace가 상세코드 관리 화면 자체를 정의한다. |


## BASIC-14 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-R2A-01 | SCR-FUNCTION-PERMISSION-MGMT 권한 matrix | ACT-FUNCTION-PERMISSION-LIST | REQ-148 | 기능 권한 목록 조회 |
| US-R2A-01 | SCR-FUNCTION-PERMISSION-MGMT 저장 확인 | ACT-FUNCTION-PERMISSION-SAVE | REQ-149 | 기능 권한 저장 후 재조회 |
| US-R2A-01 | SCR-FUNCTION-PERMISSION-MGMT 차단 상태 | ACT-FUNCTION-PERMISSION-EVALUATE | REQ-150, REQ-151 | UI/직접 API 차단 |
| US-R2A-02 | SCR-PERIOD-PERMISSION-MGMT 기간 상태 영역 | ACT-PERIOD-PERMISSION-LIST | REQ-154, REQ-155, REQ-156 | 기간 전/중/후 effective 상태 표시 |
| US-R2A-02 | SCR-PERIOD-PERMISSION-MGMT 연결 저장 | ACT-PERIOD-PERMISSION-SAVE | REQ-153, REQ-169, REQ-170 | 기간 연결 저장과 처리 시점 차단 |
| US-R2A-03 | SCR-TEMPORARY-PERMISSION-MGMT 임시 권한 폼 | ACT-TEMPORARY-PERMISSION-CREATE | REQ-157, REQ-158, REQ-160 | 지정 기능 임시 권한 저장과 기본 역할 불변 |
| US-R2A-03 | SCR-TEMPORARY-PERMISSION-MGMT 만료 상태 | ACT-TEMPORARY-PERMISSION-LIST | REQ-159 | 만료 자동 회수 표시 |
| US-R2A-04 | SCR-PERMISSION-HISTORY 이력 목록 | ACT-PERMISSION-HISTORY-LIST | REQ-161, REQ-162, REQ-163, REQ-164, REQ-171 | 변경 전후 값·처리자·사유 검색과 읽기 전용 확인 |

## BASIC-14 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story |
|---|---|---|---|---|---|---|
| /admin/function-permissions | SCR-FUNCTION-PERMISSION-MGMT | R09 | 시스템 관리 > 역할·권한 관리 > 기능 권한 관리 | listFunctionPermissions / saveFunctionPermissions / evaluateFunctionPermission | function_permissions | US-R2A-01 |
| /admin/period-permissions | SCR-PERIOD-PERMISSION-MGMT | R09 | 시스템 관리 > 역할·권한 관리 > 기간별 권한 관리 | listPeriodPermissions / savePeriodPermissions | period_permission_links | US-R2A-02 |
| /admin/temporary-permissions | SCR-TEMPORARY-PERMISSION-MGMT | R09 | 시스템 관리 > 역할·권한 관리 > 임시 권한 관리 | listTemporaryPermissions / createTemporaryPermission | temporary_permissions | US-R2A-03 |
| /admin/permission-history | SCR-PERMISSION-HISTORY | R09 | 시스템 관리 > 역할·권한 관리 > 권한 변경 이력 조회 | listPermissionChangeHistory | permission_change_history | US-R2A-04 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| ACT-FUNCTION-PERMISSION-LIST | SCR-FUNCTION-PERMISSION-MGMT | [조회] 클릭 | listFunctionPermissions | GET | 화면×역할×기능구분 권한 matrix 표시 | 401/403 permission, 400 ApiError 표시 |
| ACT-FUNCTION-PERMISSION-SAVE | SCR-FUNCTION-PERMISSION-MGMT | [저장] 확인 | saveFunctionPermissions /api/admin/function-permissions-save | PUT | function_permissions 저장 후 재조회 | 기능구분 누락/권한 없음이면 기존 matrix 유지 |
| ACT-FUNCTION-PERMISSION-EVALUATE | SCR-FUNCTION-PERMISSION-MGMT | 금지 기능 CTA 또는 직접 API 검증 | evaluateFunctionPermission | POST | 허용이면 대상 기능 진행, DENY면 CTA 비활성/403 표시 | 평가확정 또는 기간 밖이면 대상 row 무변경 |
| ACT-PERIOD-PERMISSION-LIST | SCR-PERIOD-PERMISSION-MGMT | [조회] 클릭 | listPeriodPermissions | GET | 기간 전/중/후 상태와 권한 effective 표시 | 401/403 permission, ApiError 표시 |
| ACT-PERIOD-PERMISSION-SAVE | SCR-PERIOD-PERMISSION-MGMT | [연결 저장] 확인 | savePeriodPermissions /api/admin/period-permissions-save | PUT | period_permission_links 저장 후 재조회 | 기간 기준 누락 또는 종료 후 처리 시점이면 저장 차단 |
| ACT-TEMPORARY-PERMISSION-LIST | SCR-TEMPORARY-PERMISSION-MGMT | [조회] 클릭 | listTemporaryPermissions | GET | 임시 권한과 ACTIVE/EXPIRED 상태 표시 | 401/403 permission, ApiError 표시 |
| ACT-TEMPORARY-PERMISSION-CREATE | SCR-TEMPORARY-PERMISSION-MGMT | [임시 권한 부여] 확인 | createTemporaryPermission /api/admin/temporary-permissions-create | POST | temporary_permissions 저장 후 목록 재조회 | 기간 밖 또는 필수 대상 누락이면 user_roles 무변경 |
| ACT-PERMISSION-HISTORY-LIST | SCR-PERMISSION-HISTORY | [검색] 클릭 | listPermissionChangeHistory | GET | 변경 전후 값·처리자·사유·변경일시 목록 표시 | 401/403 permission, ApiError 표시; 수정/삭제 CTA 없음 |

### SCR-FUNCTION-PERMISSION-MGMT Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ 시스템 관리 > 역할·권한 관리 > 기능 권한 관리                         R09 │
│ 검색조건: 화면ID [SCR-FUNCTION-PERMISSION-MGMT] 역할 [R09 ▼] [조회]          │
│ 권한 matrix: 화면 | 역할 | 기능구분(READ/CREATE/UPDATE/DELETE/EXECUTE) | 허용 │
│ <function permission row from API>                                            │
│ 상세: screen_id readonly, role_code, function_type, permission_allowed, 사유   │
│ [저장] [취소]  차단 미리보기: DENY 기능 CTA 비활성 / 직접 API 403             │
│ states: loading / empty / error / permission / success                        │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<function permission row from API>` | SCR-FUNCTION-PERMISSION-MGMT matrix | API row placeholder | layout-only-sample | API response placeholder only | DB/API 응답으로만 렌더링하고 seed로 하드코딩하지 않는다. |
| `R09` | 역할 selector | 기존 시스템관리자 역할 | source-backed-enum | SPEC-REQUEST.md line 27 | 기존 role seed를 참조만 한다. |

### SCR-PERIOD-PERMISSION-MGMT Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ 시스템 관리 > 역할·권한 관리 > 기간별 권한 관리                       R09 │
│ 검색조건: 업무기간ID [businessPeriodId____] 상태 [BEFORE/ACTIVE/AFTER] [조회]│
│ 목록: 업무기간 | 화면 | 역할 | 기능구분 | 시작 | 종료 | effective 상태       │
│ <period permission row from API>                                              │
│ 연결 편집: business_period_id, function_permission_id, effective_start/end    │
│ [연결 저장] [취소]  기간 밖 변경성 요청은 처리 시점 기준 차단                 │
│ states: loading / empty / error / permission / success                        │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<period permission row from API>` | SCR-PERIOD-PERMISSION-MGMT list | API row placeholder | layout-only-sample | API response placeholder only | DB/API 응답으로만 렌더링하고 seed로 하드코딩하지 않는다. |
| `BEFORE/ACTIVE/AFTER` | 상태 selector | 기간 상태 표시 | source-backed-enum | SPEC-REQUEST.md line 68 | enum/display 값으로 사용 가능하다. |

### SCR-TEMPORARY-PERMISSION-MGMT Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ 시스템 관리 > 역할·권한 관리 > 임시 권한 관리                         R09 │
│ 검색조건: user_id [____] 기능구분 [UPDATE ▼] 상태 [ACTIVE/EXPIRED] [조회]    │
│ 목록: 사용자 | 업무자료 | 기능 | 유효 시작 | 유효 종료 | 상태                │
│ <temporary permission row from API>                                           │
│ 부여 폼: user_id, work_data_ref, function_type, valid_start/end, 사유          │
│ [임시 권한 부여] [취소]  안내: 기본 역할/user_roles는 변경하지 않음           │
│ states: loading / empty / error / permission / success                        │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<temporary permission row from API>` | SCR-TEMPORARY-PERMISSION-MGMT list | API row placeholder | layout-only-sample | API response placeholder only | DB/API 응답으로만 렌더링하고 seed로 하드코딩하지 않는다. |
| `ACTIVE/EXPIRED` | 상태 selector | 임시 권한 상태 표시 | source-backed-enum | SPEC-REQUEST.md line 69 | enum/display 값으로 사용 가능하다. |

### SCR-PERMISSION-HISTORY Wireframe

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ 시스템 관리 > 역할·권한 관리 > 권한 변경 이력 조회                    R09 │
│ 검색조건: 유형 [ROLE/MENU/FUNCTION/DATA_SCOPE/TEMPORARY] 대상ID [____] [검색] │
│ 이력 목록: 유형 | 대상 | 변경 전 값 | 변경 후 값 | 처리자 | 사유 | 변경일시    │
│ <permission history row from API>                                             │
│ 읽기 전용 안내: 이 화면에는 권한 변경, 이력 수정, 이력 삭제 CTA가 없다.       │
│ states: loading / empty / error / permission / success                        │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<permission history row from API>` | SCR-PERMISSION-HISTORY list | API row placeholder | layout-only-sample | API response placeholder only | DB/API 응답으로만 렌더링하고 seed로 하드코딩하지 않는다. |
| `ROLE/MENU/FUNCTION/DATA_SCOPE/TEMPORARY` | 유형 selector | 이력 검색 범위 표시 | source-backed-enum | SPEC-REQUEST.md line 70 | enum/display 값으로 사용 가능하다. |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-008 | REQ-148 | AC-B14-008-01 | SCR-FUNCTION-PERMISSION-MGMT | /admin/function-permissions | listFunctionPermissions | [조회] 후 기능 권한 matrix 표시 |
| CMN-FR-008 | REQ-149 | AC-B14-008-02 | SCR-FUNCTION-PERMISSION-MGMT | /admin/function-permissions | saveFunctionPermissions | [저장] 후 같은 기능구분 재조회 |
| CMN-FR-008 | REQ-150 | AC-B14-008-03 | SCR-FUNCTION-PERMISSION-MGMT | /admin/function-permissions | evaluateFunctionPermission | DENY CTA 비활성 표시 |
| CMN-FR-010 | REQ-153 | AC-B14-010-01 | SCR-PERIOD-PERMISSION-MGMT | /admin/period-permissions | savePeriodPermissions | 연결 저장 후 목록 재조회 |
| CMN-FR-011 | REQ-157 | AC-B14-011-01 | SCR-TEMPORARY-PERMISSION-MGMT | /admin/temporary-permissions | createTemporaryPermission | 임시 권한 저장 후 user_roles 불변 안내 확인 |
| CMN-FR-012 | REQ-161 | AC-B14-012-01 | SCR-PERMISSION-HISTORY | /admin/permission-history | listPermissionChangeHistory | 변경 전후 값 이력 목록 표시 |


## BASIC-22 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-01 | SCR-MESSAGE-MGMT 목록/폼 | ACT-MESSAGE-SAVE | REQ-242 | 저장 후 같은 message_code 최신 문구 재조회 |
| US-02 | SCR-NOTICE-MGMT 목록/폼/첨부 | ACT-NOTICE-SAVE | REQ-264 | 저장 후 대상/기간 조건 목록에 표시 |
| US-03 | SCR-HELP-MGMT 목록/폼 | ACT-HELP-SAVE | REQ-304 | 같은 screen_id 도움말만 반환 |
| US-04 | SCR-MANUAL-MGMT 목록/등록/다운로드 | ACT-MANUAL-CREATE | REQ-331 | 최신 시행일 매뉴얼과 원본 파일 다운로드 확인 |

## BASIC-22 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | menu_path | role | canonical_id |
|---|---|---|---|---|
| /admin/messages | SCR-MESSAGE-MGMT | 시스템 관리 > 시스템 환경설정 > 메시지 관리 | R09 | REQ-240 |
| /admin/notices | SCR-NOTICE-MGMT | 시스템 관리 > 공지·도움말 관리 > 공지사항 관리 | R09 | REQ-263 |
| /admin/help-contents | SCR-HELP-MGMT | 시스템 관리 > 공지·도움말 관리 > 도움말 관리 | R09 | REQ-303 |
| /admin/manuals | SCR-MANUAL-MGMT | 시스템 관리 > 공지·도움말 관리 > 매뉴얼 관리 | R09 | REQ-329 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| ACT-MESSAGE-LIST | SCR-MESSAGE-MGMT | [조회] 클릭 | listMessages | GET | 메시지코드·유형·사용자 문구 목록 표시 | 401/403 permission, 400 ApiError banner |
| ACT-MESSAGE-SAVE | SCR-MESSAGE-MGMT | [저장] 클릭 후 확인 | saveMessage | PUT | 저장 안내 후 listMessages 재조회 | 필수값 누락은 fields 표시, 업무 상태 변경 없음 |
| ACT-NOTICE-LIST | SCR-NOTICE-MGMT | [조회] 클릭 | listNotices | GET | 대상/기간 조건에 맞는 공지 목록 표시 | 기간/권한 불일치 공지는 미표시 |
| ACT-NOTICE-SAVE | SCR-NOTICE-MGMT | [저장] 클릭 후 확인 | createNotice 또는 saveNotice | POST/PUT | 저장 안내 후 공지 목록 재조회 | 대상 역할/조직 누락은 저장 차단 |
| ACT-NOTICE-DOWNLOAD | SCR-NOTICE-MGMT | 첨부파일 [다운로드] 클릭 | downloadNoticeAttachment | GET | 원본 파일명으로 다운로드 | 권한 없음은 403, 내부 경로/실제 파일명 미노출 |
| ACT-HELP-LIST | SCR-HELP-MGMT | [조회] 클릭 | listHelpContents | GET | screen_id별 도움말 목록 표시 | 401/403 permission, 400 ApiError banner |
| ACT-HELP-SAVE | SCR-HELP-MGMT | [저장] 클릭 후 확인 | saveHelpContent | PUT | 같은 screen_id 최신 도움말 표시 | 중복 screen_id는 현재 항목으로 갱신 |
| ACT-MANUAL-LIST | SCR-MANUAL-MGMT | [조회] 클릭 | listManuals | GET | 매뉴얼 유형·버전·대상·시행일 목록 표시 | 401/403 permission, 400 ApiError banner |
| ACT-MANUAL-CREATE | SCR-MANUAL-MGMT | [등록] 클릭 후 확인 | createManual | POST | 등록 안내 후 최신 매뉴얼 목록 재조회 | 동일 유형·대상·버전 중복은 저장 차단 |
| ACT-MANUAL-DOWNLOAD | SCR-MANUAL-MGMT | 파일 [다운로드] 클릭 | downloadManualFile | GET | 원본 파일 내용 다운로드 | 권한 없음은 403, 파일 내용 자동 수정 없음 |

### SCR-MESSAGE-MGMT Wireframe

```text
┌─ 메시지 관리 /admin/messages ─────────────────────────────┐
│ 검색: 메시지 유형 [전체 v] 메시지코드 [________] [조회]     │
│ 메시지코드 | 유형 | 사용자 문구 | 수정일시                  │
│ <message code row from API>                                │
│ 메시지코드* [________] 유형* [SAVE v]                       │
│ 사용자 문구* [________________________________________]      │
│ [저장] [취소]  상태: loading/empty/error/permission/success │
└─────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<message code row from API>` | SCR-MESSAGE-MGMT 목록 row | API 응답 row 배치 | layout-only-sample | SPEC-REQUEST.md CMN-FR-022 | seed/fixture로 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |
| `SAVE` | SCR-MESSAGE-MGMT 유형 selector | 저장 유형 예시 | source-backed-enum | SPEC-REQUEST.md line 121 | 메시지 유형 enum/seed 후보로 사용할 수 있다. |

### SCR-NOTICE-MGMT Wireframe

```text
┌─ 공지사항 관리 /admin/notices ─────────────────────────────┐
│ 검색: 게시기간 [시작____]~[종료____] 대상역할 [R09 v] [조회]│
│ 제목 | 게시기간 | 대상 역할 | 대상 조직 | 중요 | 첨부        │
│ <notice row from API>                                       │
│ 제목* [________________] 중요여부 [Y/N]                    │
│ 게시 시작일* [____] 게시 종료일* [____]                    │
│ 대상 역할* [R01~R09 선택] 대상 조직* [조직 선택]            │
│ 첨부파일 [파일 선택] 원본 파일명 표시 [저장] [취소]          │
└─────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<notice row from API>` | SCR-NOTICE-MGMT 목록 row | API 응답 row 배치 | layout-only-sample | 구체 공지 instance 미제공 | 하드코딩 금지, API/DB 응답에서 렌더링한다. |
| `R01~R09` | 대상 역할 selector | 기존 역할 범위 표시 | source-backed-enum | SPEC-REQUEST.md line 19 | 기존 role seed/enum을 참조한다. |
| `Y/N` | 중요여부 field | boolean 선택 표시 | source-backed-enum | 중요여부 관리 요구 | enum/field 값으로 사용할 수 있다. |

### SCR-HELP-MGMT Wireframe

```text
┌─ 도움말 관리 /admin/help-contents ─────────────────────────┐
│ 검색: 화면ID [________] [조회]                              │
│ 화면ID | 업무 설명 | 입력 기준 | FAQ | 연락처                │
│ <help content row from API>                                 │
│ 화면ID* [________] 업무 설명* [________________________]     │
│ 입력 기준* [________________] FAQ [____________________]     │
│ 연락처 [________________] [저장] [취소]                      │
└─────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<help content row from API>` | SCR-HELP-MGMT 목록 row | API 응답 row 배치 | layout-only-sample | 구체 도움말 instance 미제공 | 하드코딩 금지, API/DB 응답에서 렌더링한다. |

### SCR-MANUAL-MGMT Wireframe

```text
┌─ 매뉴얼 관리 /admin/manuals ────────────────────────────────┐
│ 검색: 유형 [USER/ADMIN v] 대상 사용자 [____] [조회]          │
│ 유형 | 버전 | 대상 사용자 | 시행일 | 파일 원본명              │
│ <manual row from API>                                       │
│ 매뉴얼 유형* [USER v] 버전* [____] 대상 사용자* [____]       │
│ 시행일* [____] 매뉴얼 파일* [파일 선택] [등록] [취소]        │
└─────────────────────────────────────────────────────────────┘
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<manual row from API>` | SCR-MANUAL-MGMT 목록 row | API 응답 row 배치 | layout-only-sample | 구체 매뉴얼 instance 미제공 | 하드코딩 금지, API/DB 응답에서 렌더링한다. |
| `USER/ADMIN` | 매뉴얼 유형 selector | 사용자/관리자 매뉴얼 유형 표시 | source-backed-enum | SPEC-REQUEST.md line 285 | manual_type enum/seed 후보로 사용할 수 있다. |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-022 | REQ-242 | AC-CMN-FR-022-01 | SCR-MESSAGE-MGMT | /admin/messages | saveMessage / listMessages | 저장 후 같은 메시지코드 최신 문구 표시 |
| CMN-FR-023 | REQ-264 | AC-CMN-FR-023-01 | SCR-NOTICE-MGMT | /admin/notices | createNotice / listNotices | 저장 후 제목·게시기간·대상·첨부 목록 표시 |
| CMN-FR-024 | REQ-304 | AC-CMN-FR-024-01 | SCR-HELP-MGMT | /admin/help-contents | saveHelpContent / listHelpContents | 저장 후 같은 screenId 도움말 표시 |
| CMN-FR-025 | REQ-331 | AC-CMN-FR-025-01 | SCR-MANUAL-MGMT | /admin/manuals | createManual / listManuals | 버전·대상·시행일 등록 후 목록 표시 |


## BASIC-23 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-B23-01 | SCR-BATCH-DEFINITION-MGMT 목록/편집 영역 | B14-013 | REQ-409 | 배치 정의 목록 기본 20건 조회 |
| US-B23-01 | SCR-BATCH-DEFINITION-MGMT 저장 확인 modal | B14-014 | REQ-410 | 배치 정의 저장 후 재조회 |
| US-B23-02 | SCR-BATCH-EXECUTION-MGMT 실행 영역 | B14-015 | REQ-427 | 수동실행 후 상태·사유 재조회 |
| US-B23-02 | SCR-BATCH-EXECUTION-MGMT 중지/재실행 modal | B14-016 | REQ-428 | 실행중 배치 중지와 상태 변경 |
| US-B23-03 | SCR-BATCH-RESULT-MGMT 결과/로그 영역 | B14-017 | REQ-449 | 실행ID별 결과와 로그 조회 |
| US-B23-04 | SCR-BATCH-RETRY-MGMT 재처리 영역 | B14-018 | REQ-468 | 실패 대상 재처리 결과 별도 보존 |

## BASIC-23 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story | slice_id |
|---|---|---|---|---|---|---|---|
| /admin/batch-definitions | SCR-BATCH-DEFINITION-MGMT | R09 | 시스템 운영 관리 > 배치작업 관리 > 배치 정의 관리 | listBatchDefinitions / saveBatchDefinition | batch_definitions | US-B23-01 | CUS-001 |
| /admin/batch-executions | SCR-BATCH-EXECUTION-MGMT | R09 | 시스템 운영 관리 > 배치작업 관리 > 배치 실행 관리 | listBatchExecutions / createBatchExecution / updateBatchExecutionStatus / createBatchRerun | batch_executions | US-B23-02 | CUS-002 |
| /admin/batch-results | SCR-BATCH-RESULT-MGMT | R09 | 시스템 운영 관리 > 배치작업 관리 > 배치 결과 조회 | listBatchResults / getBatchResultLog | batch_execution_results | US-B23-03 | CUS-003 |
| /admin/batch-retries | SCR-BATCH-RETRY-MGMT | R09 | 시스템 운영 관리 > 배치작업 관리 > 배치 오류 재처리 | listBatchRetryTargets / createBatchRetry | batch_retry_targets | US-B23-04 | CUS-004 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| B14-013 | SCR-BATCH-DEFINITION-MGMT | 검색/페이지 크기 변경 | listBatchDefinitions | GET | 목록 갱신, 기본 20건 | 4xx ApiError 표시 |
| B14-014 | SCR-BATCH-DEFINITION-MGMT | 저장 확인 후 제출 | saveBatchDefinition | POST | 저장 안내, 목록 재조회 | 필수값 ApiError.fields 표시 |
| B14-015 | SCR-BATCH-EXECUTION-MGMT | 수동실행 확인 후 제출 | createBatchExecution | POST | RUNNING 상태 표시, 사유 기록 | 권한/필수값 오류 표시 |
| B14-016 | SCR-BATCH-EXECUTION-MGMT | 중지/재실행 확인 후 제출 | updateBatchExecutionStatus / createBatchRerun | PATCH/POST | 상태 또는 신규 실행 연결 표시 | 권한/상태 오류 표시 |
| B14-017 | SCR-BATCH-RESULT-MGMT | 실행ID 검색/로그 보기 | listBatchResults / getBatchResultLog | GET | 결과와 로그 조회 | 조회 오류 표시 |
| B14-018 | SCR-BATCH-RETRY-MGMT | 실패 대상 선택 후 재처리 | createBatchRetry | POST | 재처리 실행ID와 원실행ID 연결 표시 | 사유 누락/비실패 대상 오류 표시 |

### SCR-BATCH-DEFINITION-MGMT Wireframe

```text
+ 시스템 운영 관리 > 배치작업 관리 > 배치 정의 관리 ----------------------+
| [검색 batchId] [업무유형] [표시건수 20 v] [조회] [엑셀다운로드]       |
|-----------------------------------------------------------------------|
| batchId | 업무유형 | 실행주기 | 선행/후행 | 최대실행시간 | 담당자 | 선택 |
| <API batch definition row>                                            |
|-----------------------------------------------------------------------|
| 배치ID* [________] 업무유형* [____] 실행주기* [____] 담당자* [____]   |
| 실행 파라미터 [ JSON textarea ] 최대실행시간 [____초]                 |
| 선행 배치 [____] 후행 배치 [____]                                     |
| [저장] [취소]                                                         |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API batch definition row>` | SCR-BATCH-DEFINITION-MGMT 목록 | API 응답 행 배치 | layout-only-sample | seed-data 계약이 구체 값을 선언하지 않음 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `20` | 표시건수 selector | 기본 표시 건수 | source-backed-enum | CMN-702 | 20/50/100 option으로 구현 가능 |

### SCR-BATCH-EXECUTION-MGMT Wireframe

```text
+ 시스템 운영 관리 > 배치작업 관리 > 배치 실행 관리 ----------------------+
| [실행상태] [batchId] [표시건수 20 v] [조회]                            |
| batchId | 현재상태 | 마지막실행 | 담당자 | [수동실행] [중지] [재실행]       |
| <API batch execution row>                                             |
|-----------------------------------------------------------------------|
| 선택 배치ID [readonly] 실행 파라미터 [ JSON textarea ] 사유* [____]    |
| [실행 확인] [중지 확인] [재실행 확인]                                  |
| 진행상황: 10초 이상 작업 시 progress + 완료 알림                       |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API batch execution row>` | SCR-BATCH-EXECUTION-MGMT 목록 | API 응답 행 배치 | layout-only-sample | seed-data 계약이 구체 값을 선언하지 않음 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `10초` | 진행상황 영역 | 장시간 처리 기준 표시 | source-backed-enum | CMN-706 | UI progress 조건으로 구현 가능 |

### SCR-BATCH-RESULT-MGMT Wireframe

```text
+ 시스템 운영 관리 > 배치작업 관리 > 배치 결과 조회 ----------------------+
| [executionId] [batchId] [성공/실패] [표시건수 20 v] [조회]             |
| executionId | 시작 | 종료 | 처리 | 성공 | 실패 | 제외 | 소요시간 | 로그 |
| <API batch result row>                                                |
|-----------------------------------------------------------------------|
| 로그파일 조회 영역: 선택 executionId의 로그 내용을 읽기 전용 표시      |
| 재실행/결과수정/로그삭제 CTA 없음                                     |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API batch result row>` | SCR-BATCH-RESULT-MGMT 목록 | API 응답 행 배치 | layout-only-sample | seed-data 계약이 구체 값을 선언하지 않음 | DB/API 응답만 렌더링하고 하드코딩 금지 |

### SCR-BATCH-RETRY-MGMT Wireframe

```text
+ 시스템 운영 관리 > 배치작업 관리 > 배치 오류 재처리 --------------------+
| [원실행ID] [실패 대상] [표시건수 20 v] [조회]                          |
| 선택 | 원실행ID | 실패 건 | 실패 사유 | [재처리]                         |
| <API batch retry target row>                                          |
|-----------------------------------------------------------------------|
| 재처리 사유* [________________________________________]                |
| [재처리 실행] [취소]                                                   |
| 결과: retryExecutionId / originalExecutionId 연결 표시                 |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API batch retry target row>` | SCR-BATCH-RETRY-MGMT 목록 | API 응답 행 배치 | layout-only-sample | seed-data 계약이 구체 값을 선언하지 않음 | DB/API 응답만 렌더링하고 하드코딩 금지 |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-079 | REQ-409 | AC-CMN-FR-079-01 | SCR-BATCH-DEFINITION-MGMT | /admin/batch-definitions | listBatchDefinitions | 목록 기본 20건과 batchId 열 확인 |
| CMN-FR-079 | REQ-410 | AC-CMN-FR-079-01 | SCR-BATCH-DEFINITION-MGMT | /admin/batch-definitions | saveBatchDefinition | 저장 후 목록 재조회에서 같은 batchId 확인 |
| CMN-FR-080 | REQ-427 | AC-CMN-FR-080-01 | SCR-BATCH-EXECUTION-MGMT | /admin/batch-executions | createBatchExecution | 사유 입력 후 RUNNING 상태 표시 |
| CMN-FR-080 | REQ-438 | AC-CMN-FR-080-02 | SCR-BATCH-EXECUTION-MGMT | /admin/batch-executions | createBatchExecution / updateBatchExecutionStatus / createBatchRerun | 권한 없음 상태에서 403 오류 표시 |
| CMN-FR-081 | REQ-449 | AC-CMN-FR-081-01 | SCR-BATCH-RESULT-MGMT | /admin/batch-results | listBatchResults | 실행ID 검색 후 건수/시간 표시 |
| CMN-FR-081 | REQ-457 | AC-CMN-FR-081-03 | SCR-BATCH-RESULT-MGMT | /admin/batch-results | getBatchResultLog | 로그 보기 후 읽기 전용 로그 표시 |
| CMN-FR-082 | REQ-466 | AC-CMN-FR-082-01 | SCR-BATCH-RETRY-MGMT | /admin/batch-retries | listBatchRetryTargets | 실패 대상만 목록 표시 |
| CMN-FR-082 | REQ-468 | AC-CMN-FR-082-03 | SCR-BATCH-RETRY-MGMT | /admin/batch-retries | createBatchRetry | 재처리 후 retryExecutionId와 originalExecutionId 표시 |


## BASIC-26 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-B26-01 | SCR-UPLOAD-TEMPLATE-MGMT 목록/저장/다운로드 | B14-019 / B14-020 / B14-021 | REQ-595, REQ-596, REQ-597 | 양식 버전 저장·재조회·파일 다운로드 |
| US-B26-02 | SCR-EXCEL-UPLOAD-MGMT 업로드/검증/반영 | B14-022 / B14-023 | REQ-601, REQ-602, REQ-603, REQ-604 | 정상 파일 전체 반영과 오류 파일 전체 차단 |
| US-B26-03 | SCR-UPLOAD-HISTORY-MGMT 이력 목록 | B14-024 | REQ-608, REQ-609 | 작업별 건수·처리시간 조회 |
| US-B26-04 | SCR-UPLOAD-ERROR-MGMT 오류 목록/다운로드 | B14-025 / B14-026 | REQ-612, REQ-613, REQ-614 | upload_id별 오류 상세와 동일 오류목록 다운로드 |
| US-B26-05 | SCR-EXCEL-DOWNLOAD-MGMT 생성/다운로드 | B14-027 | REQ-617, REQ-618, REQ-619 | 조회조건·데이터범위 기반 Excel 결과 생성 |

## BASIC-26 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story | slice_id |
|---|---|---|---|---|---|---|---|
| /admin/excel-upload-templates | SCR-UPLOAD-TEMPLATE-MGMT | R09 | 파일·데이터 관리 > 엑셀 관리 > 업로드 양식 관리 | listUploadTemplates / saveUploadTemplate / downloadUploadTemplate | excel_upload_templates | US-B26-01 | CUS-005 |
| /admin/excel-uploads | SCR-EXCEL-UPLOAD-MGMT | R09 | 파일·데이터 관리 > 엑셀 관리 > 엑셀 업로드 | createExcelUpload / commitExcelUpload | excel_upload_files | US-B26-02 | CUS-006 |
| /admin/excel-upload-histories | SCR-UPLOAD-HISTORY-MGMT | R09 | 파일·데이터 관리 > 엑셀 관리 > 업로드 이력 | listExcelUploadHistories | excel_upload_histories | US-B26-03 | CUS-007 |
| /admin/excel-upload-errors | SCR-UPLOAD-ERROR-MGMT | R09 | 파일·데이터 관리 > 엑셀 관리 > 업로드 오류 관리 | listExcelUploadErrors / downloadExcelUploadErrors | excel_upload_errors | US-B26-04 | CUS-008 |
| /admin/excel-downloads | SCR-EXCEL-DOWNLOAD-MGMT | R09 | 파일·데이터 관리 > 엑셀 관리 > 엑셀 다운로드 | createExcelDownload | excel_download_jobs | US-B26-05 | CUS-009 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| B14-019 | SCR-UPLOAD-TEMPLATE-MGMT | 업무구분/기준일/표시건수 조회 | listUploadTemplates | GET | 양식 버전 목록과 규칙 표시 | 4xx ApiError 표시 |
| B14-020 | SCR-UPLOAD-TEMPLATE-MGMT | 저장 확인 후 제출 | saveUploadTemplate | POST | 저장 안내, 목록 재조회 | 필수값 ApiError.fields 표시 |
| B14-021 | SCR-UPLOAD-TEMPLATE-MGMT | 버전별 파일 다운로드 | downloadUploadTemplate | GET | 원본 파일명으로 다운로드 | 권한 없음/파일 없음 오류 표시 |
| B14-022 | SCR-EXCEL-UPLOAD-MGMT | 파일 선택 후 업로드 | createExcelUpload | POST multipart | 검증결과 정상/오류/제외 건수 표시 | 첨부정책/검증 오류 표시 |
| B14-023 | SCR-EXCEL-UPLOAD-MGMT | 반영 확인 후 실행 | commitExcelUpload | POST | 저장건수와 이력 링크 표시 | 오류 존재 409 또는 권한 오류 표시 |
| B14-024 | SCR-UPLOAD-HISTORY-MGMT | upload_id/파일명/표시건수 조회 | listExcelUploadHistories | GET | 작업별 건수·처리시간 표시 | 조회 오류 표시 |
| B14-025 | SCR-UPLOAD-ERROR-MGMT | upload_id 조회 | listExcelUploadErrors | GET | 오류행 상세 표시 | 필수 upload_id 누락 오류 표시 |
| B14-026 | SCR-UPLOAD-ERROR-MGMT | 오류목록 다운로드 | downloadExcelUploadErrors | GET | 화면 동일 오류 파일 다운로드 | 권한 없음/파일 생성 오류 표시 |
| B14-027 | SCR-EXCEL-DOWNLOAD-MGMT | 출력유형 선택 후 생성 | createExcelDownload | POST | file_token 및 다운로드 안내 표시 | 권한 밖/출력권한 오류 표시 |

### SCR-UPLOAD-TEMPLATE-MGMT Wireframe

```text
+ 파일·데이터 관리 > 엑셀 관리 > 업로드 양식 관리 ----------------------+
| [업무구분] [기준일] [표시건수 20 v] [조회] [새 양식 저장]              |
| templateVersion | 시행일 | 필수열 | 열순서 | 코드값규칙 | 파일 | 다운로드 |
| <API upload template row>                                             |
|-----------------------------------------------------------------------|
| 업무구분* [____] 양식 버전* [____] 시행일* [____]                     |
| 필수 열* [multi input] 열 순서* [ordered list]                         |
| 코드값 규칙* [기존 코드기준 참조] 다운로드 파일* [file token]          |
| [저장] [취소]                                                         |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API upload template row>` | SCR-UPLOAD-TEMPLATE-MGMT 목록 | API 응답 행 배치 | layout-only-sample | seed-data 계약이 구체 업무값을 선언하지 않음 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `20` | 표시건수 selector | 기본 표시 건수 | source-backed-enum | CMN-702 | 20/50/100 option으로 구현 가능 |

### SCR-EXCEL-UPLOAD-MGMT Wireframe

```text
+ 파일·데이터 관리 > 엑셀 관리 > 엑셀 업로드 ----------------------------+
| [업무구분] [양식 버전] [템플릿 다운로드]                              |
| 업로드 파일* [파일 선택] [업로드 및 검증]                              |
| 검증결과: 정상 [count] 오류 [count] 제외 [count] 저장 [count]           |
| rowNumber | 판정 | 오류코드 | 오류사유 | 수정안내                     |
| <API excel validation row>                                             |
| [오류목록 다운로드] [반영 실행]                                        |
| 진행상황: 10초 이상 작업 시 progress + 완료 알림                       |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API excel validation row>` | SCR-EXCEL-UPLOAD-MGMT 검증결과 | API 응답 행 배치 | layout-only-sample | 실제 업로드 파일 내용은 source 미제공 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `10초` | 진행상황 영역 | 장시간 처리 기준 표시 | source-backed-enum | CMN-706 | progress 조건으로 구현 가능 |

### SCR-UPLOAD-HISTORY-MGMT Wireframe

```text
+ 파일·데이터 관리 > 엑셀 관리 > 업로드 이력 ----------------------------+
| [uploadId] [파일명] [실행일시] [표시건수 20 v] [조회]                  |
| uploadId | 원본파일명 | 업로더 | 일시 | 총 | 정상 | 오류 | 제외 | 저장 | 처리시간 |
| <API excel upload history row>                                        |
| 재업로드/업무자료 변경 CTA 없음                                       |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API excel upload history row>` | SCR-UPLOAD-HISTORY-MGMT 목록 | API 응답 행 배치 | layout-only-sample | seed-data 계약이 구체 파일값을 선언하지 않음 | DB/API 응답만 렌더링하고 하드코딩 금지 |

### SCR-UPLOAD-ERROR-MGMT Wireframe

```text
+ 파일·데이터 관리 > 엑셀 관리 > 업로드 오류 관리 ------------------------+
| uploadId* [____________] [표시건수 20 v] [조회] [오류목록 다운로드]     |
| rowNumber | columnName | inputValue | errorCode | errorReason | correctionGuide |
| <API excel upload error row>                                          |
| 오류 입력값 자동수정/저장 CTA 없음                                    |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API excel upload error row>` | SCR-UPLOAD-ERROR-MGMT 목록 | API 응답 행 배치 | layout-only-sample | 오류 row 값은 업로드 검증 결과에서 생성 | DB/API 응답만 렌더링하고 하드코딩 금지 |

### SCR-EXCEL-DOWNLOAD-MGMT Wireframe

```text
+ 파일·데이터 관리 > 엑셀 관리 > 엑셀 다운로드 --------------------------+
| 현재 조회조건 [readonly summary]                                      |
| 출력유형* [평가대상자 v] 사용자 데이터 범위 [server applied]           |
| [엑셀 생성]                                                           |
| 결과: fileToken / originalFileName                                    |
| 권한 밖 자료는 파일에 포함되지 않음                                   |
+-----------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `평가대상자` | 출력유형 selector | 요청된 출력유형 표시 | source-backed-enum | CMN-FR-059 입력 항목 | TARGET/STATUS/ERROR mapping 중 TARGET label로 구현 가능 |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-055 | REQ-595 | AC-CMN-FR-055-03 | SCR-UPLOAD-TEMPLATE-MGMT | /admin/excel-upload-templates | listUploadTemplates | 업무/기준일 조회 후 버전과 시행일 표시 |
| CMN-FR-055 | REQ-596 | AC-CMN-FR-055-01 | SCR-UPLOAD-TEMPLATE-MGMT | /admin/excel-upload-templates | saveUploadTemplate | 저장 후 필수열·열순서·코드값규칙 재조회 |
| CMN-FR-055 | REQ-597 | AC-CMN-FR-055-02 | SCR-UPLOAD-TEMPLATE-MGMT | /admin/excel-upload-templates | downloadUploadTemplate | 대상 업무/시행일 양식 파일 다운로드 |
| CMN-FR-056 | REQ-601 | AC-CMN-FR-056-01 | SCR-EXCEL-UPLOAD-MGMT | /admin/excel-uploads | createExcelUpload | 파일 업로드 후 검증결과 영역 표시 |
| CMN-FR-056 | REQ-603 | AC-CMN-FR-056-03 | SCR-EXCEL-UPLOAD-MGMT | /admin/excel-uploads | commitExcelUpload | 정상 파일 반영 후 저장건수 표시 |
| CMN-FR-056 | REQ-604 | AC-CMN-FR-056-02 | SCR-EXCEL-UPLOAD-MGMT | /admin/excel-uploads | commitExcelUpload | 오류 파일 반영 시 409 오류와 업무자료 불변 표시 |
| CMN-FR-057 | REQ-608 | AC-CMN-FR-057-01 | SCR-UPLOAD-HISTORY-MGMT | /admin/excel-upload-histories | listExcelUploadHistories | upload_id 조건으로 작업 이력 표시 |
| CMN-FR-058 | REQ-612 | AC-CMN-FR-058-01 | SCR-UPLOAD-ERROR-MGMT | /admin/excel-upload-errors | listExcelUploadErrors | upload_id별 오류행만 표시 |
| CMN-FR-058 | REQ-614 | AC-CMN-FR-058-03 | SCR-UPLOAD-ERROR-MGMT | /admin/excel-upload-errors | downloadExcelUploadErrors | 화면 동일 오류목록 다운로드 |
| CMN-FR-059 | REQ-618 | AC-CMN-FR-059-02 | SCR-EXCEL-DOWNLOAD-MGMT | /admin/excel-downloads | createExcelDownload | 권한 범위 밖 자료 제외 후 파일 생성 |


## BASIC-29 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-B29-01 | SCR-ACTIVE-SESSION-STATUS 목록/강제종료 | B14-028 / B14-029 | REQ-665, REQ-667, REQ-668 | 활성 세션 조회 후 사유 입력 강제종료와 접근 차단 확인 |
| US-B29-02 | SCR-SESSION-TERMINATION-HISTORY 목록 | B14-030 | REQ-677, REQ-678, REQ-679 | 사용자·기간별 종료유형과 사유 조회 |
| US-B29-03 | SCR-BUSINESS-PROCESS-LOG 목록 | B14-031 | REQ-684, REQ-685, REQ-687 | 행위별 전후상태와 성공/실패 표시 |
| US-B29-04 | SCR-SENSITIVE-INFO-ACCESS-LOG 목록 | B14-032 | REQ-701, REQ-702, REQ-705 | 중요정보 대상범위·조회목적 표시와 원문 비노출 |
| US-B29-05 | SCR-PERMISSION-CHANGE-LOG 목록 | B14-033 | REQ-702, REQ-703, REQ-704 | 권한 변경 전후값과 사유 표시, 현재 권한 불변 |

## BASIC-29 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story | slice_id |
|---|---|---|---|---|---|---|---|
| /admin/security/active-sessions | SCR-ACTIVE-SESSION-STATUS | R09 | 보안·감사 관리 > 접속기록 관리 > 접속현황 조회 | listActiveSessions / terminateActiveSession | sessions | US-B29-01 | CUS-010 |
| /admin/security/session-termination-histories | SCR-SESSION-TERMINATION-HISTORY | R09 | 보안·감사 관리 > 접속기록 관리 > 로그아웃·만료 이력 | listSessionTerminationHistories | session_termination_history | US-B29-02 | CUS-011 |
| /admin/audit/business-process-logs | SCR-BUSINESS-PROCESS-LOG | R09 | 보안·감사 관리 > 감사로그 관리 > 업무처리 로그 | listBusinessProcessLogs | business_process_audit_logs | US-B29-03 | CUS-012 |
| /admin/audit/sensitive-information-access-logs | SCR-SENSITIVE-INFO-ACCESS-LOG | R09 | 보안·감사 관리 > 감사로그 관리 > 중요정보 조회 로그 | listSensitiveInformationAccessLogs | sensitive_information_access_logs | US-B29-04 | CUS-013 |
| /admin/audit/permission-change-logs | SCR-PERMISSION-CHANGE-LOG | R09 | 보안·감사 관리 > 감사로그 관리 > 권한변경 로그 | listPermissionChangeLogs | permission_change_history | US-B29-05 | CUS-014 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| B14-028 | SCR-ACTIVE-SESSION-STATUS | 사용자/IP/표시건수 조회 | listActiveSessions | GET | ACTIVE 세션 목록 표시, 종료·만료 세션 제외 | 401/403/400 ApiError banner |
| B14-029 | SCR-ACTIVE-SESSION-STATUS | 사유 입력 후 강제종료 확인 | terminateActiveSession | POST | 대상 세션 접근 차단, 목록 재조회, 감사 기록 안내 | 사유 누락 fields.reason, 권한 없음 403, 종료 세션 409 |
| B14-030 | SCR-SESSION-TERMINATION-HISTORY | 사용자/기간/종료유형 조회 | listSessionTerminationHistories | GET | 종료유형·종료일시·사유 목록 표시 | 401/403/400 ApiError banner |
| B14-031 | SCR-BUSINESS-PROCESS-LOG | 행위유형/대상키/처리자/기간 조회 | listBusinessProcessLogs | GET | 전후상태·처리결과 표시 | 조회 오류 표시, 재실행/취소 CTA 없음 |
| B14-032 | SCR-SENSITIVE-INFO-ACCESS-LOG | 정보유형/조회자/기간 조회 | listSensitiveInformationAccessLogs | GET | 대상범위·조회목적 표시, 원문값 비노출 | 권한 없음 표시, 원문 확인 CTA 없음 |
| B14-033 | SCR-PERMISSION-CHANGE-LOG | 권한유형/대상/승인자/처리자/기간 조회 | listPermissionChangeLogs | GET | 변경 전후값·사유 표시 | 권한 변경 CTA 없음, 4xx ApiError 표시 |

### SCR-ACTIVE-SESSION-STATUS Wireframe

```text
+ 보안·감사 관리 > 접속기록 관리 > 접속현황 조회 -------------------------+
| [사용자] [IP] [표시건수 20 v] [조회] [엑셀다운로드]                    |
| 선택 | 사용자 | 로그인시각 | 최종활동시각 | IP | 세션상태 | 강제종료     |
| <API active session row>                                               |
|------------------------------------------------------------------------|
| 선택 sessionId [readonly] 강제종료 사유* [________________________]     |
| [강제종료] [취소]                                                       |
| 상태: loading/empty/error/permission/success; 종료·만료 세션 미표시     |
+------------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API active session row>` | SCR-ACTIVE-SESSION-STATUS 목록 | API 응답 행 배치 | layout-only-sample | 구체 세션 instance 미제공 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `20` | 표시건수 selector | 기본 표시 건수 | source-backed-enum | CMN-702 | 20/50/100 option으로 구현 가능 |

### SCR-SESSION-TERMINATION-HISTORY Wireframe

```text
+ 보안·감사 관리 > 접속기록 관리 > 로그아웃·만료 이력 -------------------+
| [사용자] [기간 시작]~[기간 종료] [종료유형 v] [표시건수 20 v] [조회]   |
| 사용자 | 세션 | 종료유형 | 종료일시 | 종료사유                         |
| <API session termination history row>                                  |
| 수정/삭제/세션종료 CTA 없음                                            |
+------------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API session termination history row>` | SCR-SESSION-TERMINATION-HISTORY 목록 | API 응답 행 배치 | layout-only-sample | 구체 종료이력 instance 미제공 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `20` | 표시건수 selector | 기본 표시 건수 | source-backed-enum | CMN-702 | 20/50/100 option으로 구현 가능 |

### SCR-BUSINESS-PROCESS-LOG Wireframe

```text
+ 보안·감사 관리 > 감사로그 관리 > 업무처리 로그 ------------------------+
| [행위유형] [대상키] [처리자] [기간] [표시건수 20 v] [조회]             |
| 행위유형 | 대상키 | 처리 전 상태 | 처리 후 상태 | 처리자 | 일시 | 결과       |
| <API business process audit row>                                       |
| 원업무 재실행/취소/로그삭제 CTA 없음                                   |
+------------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API business process audit row>` | SCR-BUSINESS-PROCESS-LOG 목록 | API 응답 행 배치 | layout-only-sample | 구체 업무처리 log instance 미제공 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `20` | 표시건수 selector | 기본 표시 건수 | source-backed-enum | CMN-702 | 20/50/100 option으로 구현 가능 |

### SCR-SENSITIVE-INFO-ACCESS-LOG Wireframe

```text
+ 보안·감사 관리 > 감사로그 관리 > 중요정보 조회 로그 -------------------+
| [정보유형] [조회자] [기간] [표시건수 20 v] [조회]                      |
| 정보유형 | 조회자 | 대상범위 | 조회목적 | 조회일시 | 조회결과               |
| <API sensitive information access row>                                 |
| 중요정보 원문/계좌 원문/평가결과 원문 표시 영역 없음                   |
+------------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API sensitive information access row>` | SCR-SENSITIVE-INFO-ACCESS-LOG 목록 | API 응답 행 배치 | layout-only-sample | 구체 중요정보 조회 log instance 미제공 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `20` | 표시건수 selector | 기본 표시 건수 | source-backed-enum | CMN-702 | 20/50/100 option으로 구현 가능 |

### SCR-PERMISSION-CHANGE-LOG Wireframe

```text
+ 보안·감사 관리 > 감사로그 관리 > 권한변경 로그 ------------------------+
| [권한유형] [변경대상] [승인자] [처리자] [기간] [표시건수 20 v] [조회] |
| 권한유형 | 변경대상 | 변경 전 값 | 변경 후 값 | 승인자 | 처리자 | 사유 | 일시 |
| <API permission change audit row>                                      |
| 권한 부여/변경/회수 CTA 없음, 현재 권한값 변경 없음                    |
+------------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<API permission change audit row>` | SCR-PERMISSION-CHANGE-LOG 목록 | API 응답 행 배치 | layout-only-sample | 구체 권한변경 log instance 미제공 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `20` | 표시건수 selector | 기본 표시 건수 | source-backed-enum | CMN-702 | 20/50/100 option으로 구현 가능 |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-074 | REQ-665 | AC-CMN-FR-074-01 | SCR-ACTIVE-SESSION-STATUS | /admin/security/active-sessions | listActiveSessions | 활성 세션의 사용자·시각·IP·상태 표시 |
| CMN-FR-074 | REQ-667 | AC-CMN-FR-074-03 | SCR-ACTIVE-SESSION-STATUS | /admin/security/active-sessions | terminateActiveSession | 사유 입력 강제종료 후 접근 차단 표시 |
| CMN-FR-075 | REQ-677 | AC-CMN-FR-075-01 | SCR-SESSION-TERMINATION-HISTORY | /admin/security/session-termination-histories | listSessionTerminationHistories | 사용자/기간으로 종료이력 표시 |
| CMN-FR-076 | REQ-684 | AC-CMN-FR-076-01 | SCR-BUSINESS-PROCESS-LOG | /admin/audit/business-process-logs | listBusinessProcessLogs | 행위유형·대상키 검색 후 전후상태 표시 |
| CMN-FR-077 | REQ-701 | AC-CMN-FR-077-01 | SCR-SENSITIVE-INFO-ACCESS-LOG | /admin/audit/sensitive-information-access-logs | listSensitiveInformationAccessLogs | 중요정보 대상범위·목적 표시, 원문 미표시 |
| CMN-FR-078 | REQ-702 | AC-CMN-FR-078-01 | SCR-PERMISSION-CHANGE-LOG | /admin/audit/permission-change-logs | listPermissionChangeLogs | 권한 변경 전후값과 사유 표시 |


## BASIC-32 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-B32-01 | SCR-EVALUATION-ORG-MAPPING list/form | B32-A01/B32-A02 | REQ-768, REQ-769 | 평가조직 매핑 조회 후 업무 권한 연결 저장 결과를 재조회한다. |
| US-B32-02 | SCR-BUSINESS-STATUS-CODE list/form | B32-A03/B32-A04 | REQ-771, REQ-772, REQ-773 | 상태코드 조회·저장·확정 코드 변경 차단을 확인한다. |
| US-B32-03 | SCR-BUSINESS-STATUS-TRANSITION list/form | B32-A05/B32-A06 | REQ-774, REQ-775, REQ-776 | 전이규칙 조회·저장·필수조건 차단을 확인한다. |
| US-B32-04 | SCR-REJECTION-REASON list/form | B32-A07/B32-A08 | REQ-777, REQ-778, REQ-779 | 반려사유 조회·저장·추가의견 허용을 확인한다. |
| US-B32-05 | SCR-DATA-CHANGE-HISTORY search/list | B32-A09 | REQ-780, REQ-781, REQ-782 | 변경이력 조건 검색과 필드별 전후값 표시를 확인한다. |
| US-B32-06 | SCR-DELETED-BUSINESS-DATA search/list | B32-A10 | REQ-783, REQ-784 | 삭제자료 조건 검색과 삭제정보 표시, 복구/물리삭제 CTA 부재를 확인한다. |

## BASIC-32 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story | slice_id |
|---|---|---|---|---|---|---|---|
| /admin/evaluation-organization-mappings | SCR-EVALUATION-ORG-MAPPING | R09 | 업무 운영 관리 > 업무권한 관리 > 평가조직 매핑 | listEvaluationOrganizationMappings / saveEvaluationOrganizationMapping | evaluation_organization_mappings | US-B32-01 | CUS-015 |
| /admin/business-status-codes | SCR-BUSINESS-STATUS-CODE | R09 | 업무 운영 관리 > 업무상태 관리 > 상태코드 관리 | listBusinessStatusCodes / saveBusinessStatusCode | business_status_codes | US-B32-02 | CUS-016 |
| /admin/business-status-transitions | SCR-BUSINESS-STATUS-TRANSITION | R09 | 업무 운영 관리 > 업무상태 관리 > 상태 전이 관리 | listBusinessStatusTransitions / saveBusinessStatusTransition | business_status_transitions | US-B32-03 | CUS-017 |
| /admin/rejection-reasons | SCR-REJECTION-REASON | R09 | 업무 운영 관리 > 의견·반려 관리 > 반려사유 관리 | listRejectionReasons / saveRejectionReason | rejection_reasons | US-B32-04 | CUS-018 |
| /admin/data-change-histories | SCR-DATA-CHANGE-HISTORY | R09 | 파일·데이터 관리 > 데이터 이력 관리 > 데이터 변경 이력 | listDataChangeHistories | data_change_histories | US-B32-05 | CUS-019 |
| /admin/deleted-business-data | SCR-DELETED-BUSINESS-DATA | R09 | 파일·데이터 관리 > 데이터 이력 관리 > 삭제자료 관리 | listDeletedBusinessData | deleted_business_data | US-B32-06 | CUS-020 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| B32-A01 | SCR-EVALUATION-ORG-MAPPING | 조회 | listEvaluationOrganizationMappings | GET | mapping 목록 표시 | 401/403/ApiError banner |
| B32-A02 | SCR-EVALUATION-ORG-MAPPING | 저장 확인 후 저장 | saveEvaluationOrganizationMapping | POST | 저장 후 재조회 | field-level ApiError |
| B32-A03 | SCR-BUSINESS-STATUS-CODE | 조회 | listBusinessStatusCodes | GET | 상태코드 목록 표시 | 401/403/ApiError banner |
| B32-A04 | SCR-BUSINESS-STATUS-CODE | 저장 확인 후 저장 | saveBusinessStatusCode | POST | 작성중 버전 저장 후 재조회 | 확정 코드 409/field-level ApiError |
| B32-A05 | SCR-BUSINESS-STATUS-TRANSITION | 조회 | listBusinessStatusTransitions | GET | 전이규칙 목록 표시 | 401/403/ApiError banner |
| B32-A06 | SCR-BUSINESS-STATUS-TRANSITION | 저장 확인 후 저장 | saveBusinessStatusTransition | POST | 전이규칙 저장 후 재조회 | 필수조건 400/업무규칙 409 |
| B32-A07 | SCR-REJECTION-REASON | 조회 | listRejectionReasons | GET | 반려사유 목록 표시 | 401/403/ApiError banner |
| B32-A08 | SCR-REJECTION-REASON | 저장 확인 후 저장 | saveRejectionReason | POST | 반려사유 저장 후 재조회 | field-level ApiError |
| B32-A09 | SCR-DATA-CHANGE-HISTORY | 검색 | listDataChangeHistories | GET | 변경이력과 전후값 표시 | 401/403/ApiError banner |
| B32-A10 | SCR-DELETED-BUSINESS-DATA | 검색 | listDeletedBusinessData | GET | 삭제정보 표시 | 401/403/ApiError banner |

### SCR-BUSINESS-STATUS-CODE Wireframe

```text
+ 업무 운영 관리 > 업무상태 관리 > 상태코드 관리
[업무유형: FACULTY_ACHIEVEMENT v] [상태정의 버전] [조회]
----------------------------------------------------------------
| 상태코드 | 상태 표시명 | 버전상태 | 사용여부 |
| <business status code row from API>                         |
----------------------------------------------------------------
[상태코드] [상태 표시명] [저장]
확정된 기술 상태코드는 수정·삭제하거나 다른 의미로 재사용할 수 없습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| FACULTY_ACHIEVEMENT | SCR-BUSINESS-STATUS-CODE 업무유형 selector | 교수업적평가 업무유형 예시 | source-backed-enum | SPEC-REQUEST.md Clarification 업무유형 범위 | 업무유형 enum/seed로 사용 가능하되 표시명은 source/local code에서 렌더링한다. |
| <business status code row from API> | SCR-BUSINESS-STATUS-CODE table row | API 응답 기반 행 자리 | layout-only-sample | 구체 row 값은 seed-data 계약에만 일부 존재 | UI/seed/test에 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |

### SCR-BUSINESS-STATUS-TRANSITION Wireframe

```text
+ 업무 운영 관리 > 업무상태 관리 > 상태 전이 관리
[업무유형] [현재 상태] [조회]
| 현재 상태 | 다음 상태 | 실행 역할 | 필수의견 | 필수첨부 | 취소가능 |
| <business transition row from API>                                      |
[다음 상태] [실행 역할 R01~R09] [필수의견 Y/N] [필수첨부 Y/N] [취소가능 Y/N] [저장]
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| R01~R09 | SCR-BUSINESS-STATUS-TRANSITION 실행 역할 selector | 기존 역할코드 선택 범위 | source-backed-enum | SPEC-REQUEST.md 역할 참조 | 기존 roles seed를 재사용하고 새 역할 seed를 만들지 않는다. |
| Y/N | SCR-BUSINESS-STATUS-TRANSITION 필수조건 selector | boolean flag 표시 | source-backed-enum | canonical data model char(1) convention | Y/N enum으로 저장 가능하다. |
| <business transition row from API> | SCR-BUSINESS-STATUS-TRANSITION table row | API 응답 기반 행 자리 | layout-only-sample | 구체 row 값은 업무유형 상태 seed에 따름 | UI/seed/test에 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |

### SCR-REJECTION-REASON Wireframe

```text
+ 업무 운영 관리 > 의견·반려 관리 > 반려사유 관리
[업무유형] [조회]
| 반려사유 코드 | 표준 문구 | 추가 의견 허용 |
| <rejection reason row from API>              |
[반려사유 코드] [표준 문구] [추가 의견 허용 Y/N] [저장]
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <rejection reason row from API> | SCR-REJECTION-REASON table row | API 응답 기반 행 자리 | layout-only-sample | 구체 사유 코드는 요청에 없음 | 임의 사유 코드를 하드코딩하지 말고 seed/API 응답에서 렌더링한다. |
| Y/N | SCR-REJECTION-REASON 추가 의견 허용 selector | 추가 의견 허용 여부 | source-backed-enum | SPEC-REQUEST.md 추가 의견 허용 여부 | Y/N flag로 사용 가능하다. |

### SCR-DATA-CHANGE-HISTORY Wireframe

```text
+ 파일·데이터 관리 > 데이터 이력 관리 > 데이터 변경 이력
[대상 업무·기준정보] [대상 식별정보] [처리자] [변경일시 from~to] [검색]
| 대상 | 원본키 | 처리유형 | 변경 필드 | 변경 전 값 | 변경 후 값 | 처리자 | 변경일시 |
| <data change history row from API>                                           |
조회 전용: 원본자료와 변경이력은 이 화면에서 수정·삭제할 수 없습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <data change history row from API> | SCR-DATA-CHANGE-HISTORY table row | API 응답 기반 행 자리 | layout-only-sample | 구체 변경 값은 런타임 이력 | 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |

### SCR-DELETED-BUSINESS-DATA Wireframe

```text
+ 파일·데이터 관리 > 데이터 이력 관리 > 삭제자료 관리
[삭제된 업무자료] [원본키] [삭제자] [삭제일시 from~to] [검색]
| 업무자료 | 원본키 | 삭제자 | 삭제일시 | 삭제사유 | 복구가능여부 |
| <deleted business data row from API>                               |
복구/물리삭제 기능은 제공하지 않습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <deleted business data row from API> | SCR-DELETED-BUSINESS-DATA table row | API 응답 기반 행 자리 | layout-only-sample | 구체 삭제자료 값은 런타임 논리삭제 이력 | 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| FR-001 | REQ-768 | AC-REQ-768 | SCR-EVALUATION-ORG-MAPPING | /admin/evaluation-organization-mappings | listEvaluationOrganizationMappings /api/business/evaluation-organization-mappings | R09 세션으로 평가조직 mapping 목록과 데이터 범위 badge를 확인한다. |
| CMN-FR-040 | REQ-771 | AC-REQ-771 | SCR-BUSINESS-STATUS-CODE | /admin/business-status-codes | listBusinessStatusCodes /api/admin/business-status-codes | 업무유형별 상태코드 목록을 조회한다. |
| CMN-FR-041 | REQ-774 | AC-REQ-774 | SCR-BUSINESS-STATUS-TRANSITION | /admin/business-status-transitions | listBusinessStatusTransitions /api/admin/business-status-transitions | 현재 상태별 다음 상태·실행 역할·필수조건을 조회한다. |
| CMN-FR-045 | REQ-777 | AC-REQ-777 | SCR-REJECTION-REASON | /admin/rejection-reasons | listRejectionReasons /api/admin/rejection-reasons | 업무유형별 표준 반려사유 목록을 조회한다. |
| CMN-FR-060 | REQ-780 | AC-REQ-780 | SCR-DATA-CHANGE-HISTORY | /admin/data-change-histories | listDataChangeHistories /api/admin/data-change-histories | 대상·처리자·변경일시 조건으로 변경이력을 검색한다. |
| CMN-FR-062 | REQ-783 | AC-REQ-783 | SCR-DELETED-BUSINESS-DATA | /admin/deleted-business-data | listDeletedBusinessData /api/admin/deleted-business-data | 논리삭제 자료의 삭제정보와 조회 전용 상태를 확인한다. |


## BASIC-33 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-B33-01 | SCR-EVALUATION-AREA-MGMT list/form | B33-A01/B33-A02 | REQ-864, REQ-865, REQ-866, REQ-867 | 평가영역 조회 후 작성중 version 저장 결과와 차단을 확인한다. |
| US-B33-02 | SCR-EVALUATION-ITEM-MGMT list/form | B33-A03/B33-A04 | REQ-870, REQ-871, REQ-872, REQ-873 | 평가항목 조회 후 작성중 version 저장 결과와 차단을 확인한다. |
| US-B33-03 | SCR-EVALUATION-ELEMENT-MGMT list/form | B33-A05/B33-A06 | REQ-876, REQ-877, REQ-878, REQ-879 | 평가요소 조회 후 작성중 version 저장 결과와 차단을 확인한다. |
| US-B33-04 | SCR-EVALUATION-MANAGEMENT-ITEM-MGMT list/form | B33-A07/B33-A08 | REQ-882, REQ-883, REQ-884, REQ-885 | 관리항목 조회 후 작성중 version 저장 결과와 차단을 확인한다. |
| US-B33-05 | SCR-AREA-ELEMENT-SYSTEM-MGMT list/form | B33-A09/B33-A10 | REQ-890, REQ-891, REQ-892, REQ-893 | 영역별 평가요소 체계 조회 후 작성중 version 저장 결과와 차단을 확인한다. |

## BASIC-33 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story | slice_id |
|---|---|---|---|---|---|---|---|
| /admin/evaluation-areas | SCR-EVALUATION-AREA-MGMT | R04/R09 | 평가 기준 관리 > 평가 기준정보 관리 > 평가영역 관리 | listEvaluationAreas / saveEvaluationArea | evaluation_areas | US-B33-01 | CUS-021 |
| /admin/evaluation-items | SCR-EVALUATION-ITEM-MGMT | R04/R09 | 평가 기준 관리 > 평가 기준정보 관리 > 평가항목 관리 | listEvaluationItems / saveEvaluationItem | evaluation_items | US-B33-02 | CUS-022 |
| /admin/evaluation-elements | SCR-EVALUATION-ELEMENT-MGMT | R04/R09 | 평가 기준 관리 > 평가 기준정보 관리 > 평가요소 관리 | listEvaluationElements / saveEvaluationElement | evaluation_elements | US-B33-03 | CUS-023 |
| /admin/evaluation-management-items | SCR-EVALUATION-MANAGEMENT-ITEM-MGMT | R04/R09 | 평가 기준 관리 > 평가 기준정보 관리 > 관리항목 관리 | listEvaluationManagementItems / saveEvaluationManagementItem | evaluation_management_items | US-B33-04 | CUS-024 |
| /admin/area-element-systems | SCR-AREA-ELEMENT-SYSTEM-MGMT | R04/R09 | 평가 기준 관리 > 평가 기준정보 관리 > 영역별 평가요소 체계 관리 | listAreaElementSystems / saveAreaElementSystem | area_element_system_settings | US-B33-05 | CUS-025 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| B33-A01 | SCR-EVALUATION-AREA-MGMT | 조회 | listEvaluationAreas | GET | 평가영역 목록 표시 | 401/403/ApiError banner |
| B33-A02 | SCR-EVALUATION-AREA-MGMT | 저장 확인 후 저장 | saveEvaluationArea | POST | 저장 후 재조회 | 필수값 400/확정 version 409 |
| B33-A03 | SCR-EVALUATION-ITEM-MGMT | 조회 | listEvaluationItems | GET | 평가항목 목록 표시 | 401/403/ApiError banner |
| B33-A04 | SCR-EVALUATION-ITEM-MGMT | 저장 확인 후 저장 | saveEvaluationItem | POST | 저장 후 재조회 | 필수값 400/확정 version 409 |
| B33-A05 | SCR-EVALUATION-ELEMENT-MGMT | 조회 | listEvaluationElements | GET | 평가요소 목록 표시 | 401/403/ApiError banner |
| B33-A06 | SCR-EVALUATION-ELEMENT-MGMT | 저장 확인 후 저장 | saveEvaluationElement | POST | 저장 후 재조회 | 필수값 400/확정 version 409 |
| B33-A07 | SCR-EVALUATION-MANAGEMENT-ITEM-MGMT | 조회 | listEvaluationManagementItems | GET | 관리항목 목록 표시 | 401/403/ApiError banner |
| B33-A08 | SCR-EVALUATION-MANAGEMENT-ITEM-MGMT | 저장 확인 후 저장 | saveEvaluationManagementItem | POST | 저장 후 재조회 | 필수값 400/확정 version 409 |
| B33-A09 | SCR-AREA-ELEMENT-SYSTEM-MGMT | 조회 | listAreaElementSystems | GET | 영역별 평가요소 체계 목록 표시 | 401/403/ApiError banner |
| B33-A10 | SCR-AREA-ELEMENT-SYSTEM-MGMT | 저장 확인 후 저장 | saveAreaElementSystem | POST | 저장 후 재조회 | 필수값 400/확정 version 409 |

### SCR-EVALUATION-AREA-MGMT Wireframe

```text
+ 평가 기준 관리 > 평가 기준정보 관리 > 평가영역 관리
[규정버전] [조회조건] [조회]
----------------------------------------------------------------
| 평가영역 주요 항목 | 정렬순서 | 사용여부 | 적용/상위 조건 |
| <evaluation_areas row from API>                                      |
----------------------------------------------------------------
[코드/명칭] [정렬순서] [사용여부 Y/N] [저장]
작성중 규정버전에서만 저장할 수 있으며 확정 규정버전은 수정·삭제할 수 없습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <evaluation_areas row from API> | SCR-EVALUATION-AREA-MGMT table row | API 응답 기반 행 자리 | layout-only-sample | 구체 하위 기준정보 값은 운영자가 작성중 규정버전에서 등록 | UI/seed/test에 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |
| Y/N | SCR-EVALUATION-AREA-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | SPEC-REQUEST.md CMN-302 및 각 입력 항목 | Y/N flag로 저장 가능하다. |

### SCR-EVALUATION-ITEM-MGMT Wireframe

```text
+ 평가 기준 관리 > 평가 기준정보 관리 > 평가항목 관리
[규정버전] [조회조건] [조회]
----------------------------------------------------------------
| 평가항목 주요 항목 | 정렬순서 | 사용여부 | 적용/상위 조건 |
| <evaluation_items row from API>                                      |
----------------------------------------------------------------
[코드/명칭] [정렬순서] [사용여부 Y/N] [저장]
작성중 규정버전에서만 저장할 수 있으며 확정 규정버전은 수정·삭제할 수 없습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <evaluation_items row from API> | SCR-EVALUATION-ITEM-MGMT table row | API 응답 기반 행 자리 | layout-only-sample | 구체 하위 기준정보 값은 운영자가 작성중 규정버전에서 등록 | UI/seed/test에 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |
| Y/N | SCR-EVALUATION-ITEM-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | SPEC-REQUEST.md CMN-302 및 각 입력 항목 | Y/N flag로 저장 가능하다. |

### SCR-EVALUATION-ELEMENT-MGMT Wireframe

```text
+ 평가 기준 관리 > 평가 기준정보 관리 > 평가요소 관리
[규정버전] [조회조건] [조회]
----------------------------------------------------------------
| 평가요소 주요 항목 | 정렬순서 | 사용여부 | 적용/상위 조건 |
| <evaluation_elements row from API>                                      |
----------------------------------------------------------------
[코드/명칭] [정렬순서] [사용여부 Y/N] [저장]
작성중 규정버전에서만 저장할 수 있으며 확정 규정버전은 수정·삭제할 수 없습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <evaluation_elements row from API> | SCR-EVALUATION-ELEMENT-MGMT table row | API 응답 기반 행 자리 | layout-only-sample | 구체 하위 기준정보 값은 운영자가 작성중 규정버전에서 등록 | UI/seed/test에 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |
| Y/N | SCR-EVALUATION-ELEMENT-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | SPEC-REQUEST.md CMN-302 및 각 입력 항목 | Y/N flag로 저장 가능하다. |

### SCR-EVALUATION-MANAGEMENT-ITEM-MGMT Wireframe

```text
+ 평가 기준 관리 > 평가 기준정보 관리 > 관리항목 관리
[규정버전] [조회조건] [조회]
----------------------------------------------------------------
| 관리항목 주요 항목 | 정렬순서 | 사용여부 | 적용/상위 조건 |
| <evaluation_management_items row from API>                                      |
----------------------------------------------------------------
[코드/명칭] [정렬순서] [사용여부 Y/N] [저장]
작성중 규정버전에서만 저장할 수 있으며 확정 규정버전은 수정·삭제할 수 없습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <evaluation_management_items row from API> | SCR-EVALUATION-MANAGEMENT-ITEM-MGMT table row | API 응답 기반 행 자리 | layout-only-sample | 구체 하위 기준정보 값은 운영자가 작성중 규정버전에서 등록 | UI/seed/test에 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |
| Y/N | SCR-EVALUATION-MANAGEMENT-ITEM-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | SPEC-REQUEST.md CMN-302 및 각 입력 항목 | Y/N flag로 저장 가능하다. |

### SCR-AREA-ELEMENT-SYSTEM-MGMT Wireframe

```text
+ 평가 기준 관리 > 평가 기준정보 관리 > 영역별 평가요소 체계 관리
[규정버전] [조회조건] [조회]
----------------------------------------------------------------
| 영역별 평가요소 체계 주요 항목 | 정렬순서 | 사용여부 | 적용/상위 조건 |
| <area_element_system_settings row from API>                                      |
----------------------------------------------------------------
[코드/명칭] [정렬순서] [사용여부 Y/N] [저장]
작성중 규정버전에서만 저장할 수 있으며 확정 규정버전은 수정·삭제할 수 없습니다.
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| <area_element_system_settings row from API> | SCR-AREA-ELEMENT-SYSTEM-MGMT table row | API 응답 기반 행 자리 | layout-only-sample | 구체 하위 기준정보 값은 운영자가 작성중 규정버전에서 등록 | UI/seed/test에 하드코딩하지 말고 API/DB 응답에서 렌더링한다. |
| Y/N | SCR-AREA-ELEMENT-SYSTEM-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | SPEC-REQUEST.md CMN-302 및 각 입력 항목 | Y/N flag로 저장 가능하다. |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-026 | REQ-864 | AC-REQ-864 | SCR-EVALUATION-AREA-MGMT | /admin/evaluation-areas | listEvaluationAreas /api/admin/evaluation-areas | 평가영역 목록을 조회한다. |
| CMN-FR-027 | REQ-870 | AC-REQ-870 | SCR-EVALUATION-ITEM-MGMT | /admin/evaluation-items | listEvaluationItems /api/admin/evaluation-items | 평가항목 목록을 조회한다. |
| CMN-FR-028 | REQ-876 | AC-REQ-876 | SCR-EVALUATION-ELEMENT-MGMT | /admin/evaluation-elements | listEvaluationElements /api/admin/evaluation-elements | 평가요소 목록을 조회한다. |
| CMN-FR-029 | REQ-882 | AC-REQ-882 | SCR-EVALUATION-MANAGEMENT-ITEM-MGMT | /admin/evaluation-management-items | listEvaluationManagementItems /api/admin/evaluation-management-items | 관리항목 목록을 조회한다. |
| FR-017 | REQ-890 | AC-REQ-890 | SCR-AREA-ELEMENT-SYSTEM-MGMT | /admin/area-element-systems | listAreaElementSystems /api/admin/area-element-systems | 영역별 평가요소 체계 목록을 조회한다. |

## BASIC-34 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-01 | SCR-EVAL-SCORE-MGMT 목록 | ACT-CUS-026-LIST | REQ-992 | 목록 조회 결과 표시 |
| US-01 | SCR-EVAL-SCORE-MGMT 저장 form | ACT-CUS-026-SAVE | REQ-993 | 저장 후 재조회 |
| US-02 | SCR-PARTICIPATION-RATE-MGMT 목록 | ACT-CUS-027-LIST | REQ-1000 | 목록 조회 결과 표시 |
| US-02 | SCR-PARTICIPATION-RATE-MGMT 저장 form | ACT-CUS-027-SAVE | REQ-1001 | 저장 후 재조회 |
| US-03 | SCR-CALC-FORMULA-MGMT 목록 | ACT-CUS-028-LIST | REQ-1008 | 목록 조회 결과 표시 |
| US-03 | SCR-CALC-FORMULA-MGMT 저장 form | ACT-CUS-028-SAVE | REQ-1009 | 저장 후 재조회 |
| US-04 | SCR-EVAL-RULE-SET-MGMT 목록 | ACT-CUS-029-LIST | REQ-1017 | 목록 조회 결과 표시 |
| US-04 | SCR-EVAL-RULE-SET-MGMT 저장 form | ACT-CUS-029-SAVE | REQ-1018 | 저장 후 재조회 |
| US-05 | SCR-JOURNAL-INDEXING-MGMT 목록 | ACT-CUS-030-LIST | REQ-1025 | 목록 조회 결과 표시 |
| US-05 | SCR-JOURNAL-INDEXING-MGMT 저장 form | ACT-CUS-030-SAVE | REQ-1026 | 저장 후 재조회 |

## BASIC-34 Added Screen Wireframes

### SCR-EVAL-SCORE-MGMT — 평가점수 관리

```text
+------------------------------------------------------------+
| 평가 기준 관리 > 평가 기준정보 관리 > 평가점수 관리              |
| [검색조건] ruleVersionId  적용연도/기간  사용상태 [조회]  |
|------------------------------------------------------------|
| <evaluation_score_rules row>                                               |
|------------------------------------------------------------|
| [편집영역] source 입력 항목                                |
| [저장] [취소]                                              |
| 상태: loading / empty / error / permission / success        |
+------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<evaluation_score_rules row>` | SCR-EVAL-SCORE-MGMT 목록 | API 응답 행 위치 표시 | layout-only-sample | concrete row value 미제공 | DB/API 응답으로만 렌더링하고 seed로 만들지 않는다. |
| `ruleVersionId` | SCR-EVAL-SCORE-MGMT 검색조건 | 규정버전 참조 입력 | source-backed-enum | SPEC-REQUEST.md line 152 | 기존/신규 data contract field로 구현한다. |

### SCR-PARTICIPATION-RATE-MGMT — 참여구분·배분율 관리

```text
+------------------------------------------------------------+
| 평가 기준 관리 > 평가 기준정보 관리 > 참여구분·배분율 관리              |
| [검색조건] ruleVersionId  적용연도/기간  사용상태 [조회]  |
|------------------------------------------------------------|
| <participation_rate_rules row>                                               |
|------------------------------------------------------------|
| [편집영역] source 입력 항목                                |
| [저장] [취소]                                              |
| 상태: loading / empty / error / permission / success        |
+------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<participation_rate_rules row>` | SCR-PARTICIPATION-RATE-MGMT 목록 | API 응답 행 위치 표시 | layout-only-sample | concrete row value 미제공 | DB/API 응답으로만 렌더링하고 seed로 만들지 않는다. |
| `ruleVersionId` | SCR-PARTICIPATION-RATE-MGMT 검색조건 | 규정버전 참조 입력 | source-backed-enum | SPEC-REQUEST.md line 152 | 기존/신규 data contract field로 구현한다. |

### SCR-CALC-FORMULA-MGMT — 계산식 관리

```text
+------------------------------------------------------------+
| 평가 기준 관리 > 평가 기준정보 관리 > 계산식 관리              |
| [검색조건] ruleVersionId  적용연도/기간  사용상태 [조회]  |
|------------------------------------------------------------|
| <calculation_formula_versions row>                                               |
|------------------------------------------------------------|
| [편집영역] source 입력 항목                                |
| [저장] [취소]                                              |
| 상태: loading / empty / error / permission / success        |
+------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<calculation_formula_versions row>` | SCR-CALC-FORMULA-MGMT 목록 | API 응답 행 위치 표시 | layout-only-sample | concrete row value 미제공 | DB/API 응답으로만 렌더링하고 seed로 만들지 않는다. |
| `ruleVersionId` | SCR-CALC-FORMULA-MGMT 검색조건 | 규정버전 참조 입력 | source-backed-enum | SPEC-REQUEST.md line 152 | 기존/신규 data contract field로 구현한다. |

### SCR-EVAL-RULE-SET-MGMT — 업적평가 기준·점수규칙 관리

```text
+------------------------------------------------------------+
| 평가 기준 관리 > 평가 기준정보 관리 > 업적평가 기준·점수규칙 관리              |
| [검색조건] ruleVersionId  적용연도/기간  사용상태 [조회]  |
|------------------------------------------------------------|
| <evaluation_rule_sets row>                                               |
|------------------------------------------------------------|
| [편집영역] source 입력 항목                                |
| [저장] [취소]                                              |
| 상태: loading / empty / error / permission / success        |
+------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<evaluation_rule_sets row>` | SCR-EVAL-RULE-SET-MGMT 목록 | API 응답 행 위치 표시 | layout-only-sample | concrete row value 미제공 | DB/API 응답으로만 렌더링하고 seed로 만들지 않는다. |
| `ruleVersionId` | SCR-EVAL-RULE-SET-MGMT 검색조건 | 규정버전 참조 입력 | source-backed-enum | SPEC-REQUEST.md line 152 | 기존/신규 data contract field로 구현한다. |

### SCR-JOURNAL-INDEXING-MGMT — 학술지·후보지 등재정보 관리

```text
+------------------------------------------------------------+
| 평가 기준 관리 > 평가 기준정보 관리 > 학술지·후보지 등재정보 관리              |
| [검색조건] ruleVersionId  적용연도/기간  사용상태 [조회]  |
|------------------------------------------------------------|
| <journal_indexing_infos row>                                               |
|------------------------------------------------------------|
| [편집영역] source 입력 항목                                |
| [저장] [취소]                                              |
| 상태: loading / empty / error / permission / success        |
+------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<journal_indexing_infos row>` | SCR-JOURNAL-INDEXING-MGMT 목록 | API 응답 행 위치 표시 | layout-only-sample | concrete row value 미제공 | DB/API 응답으로만 렌더링하고 seed로 만들지 않는다. |
| `ruleVersionId` | SCR-JOURNAL-INDEXING-MGMT 검색조건 | 규정버전 참조 입력 | source-backed-enum | SPEC-REQUEST.md line 152 | 기존/신규 data contract field로 구현한다. |

## BASIC-34 Screen Action Mapping

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| ACT-CUS-026-LIST | SCR-EVAL-SCORE-MGMT | 조회 버튼 | listEvaluationScores / /api/admin/evaluation-scores | GET | 목록 refresh | 4xx ApiError 표시 |
| ACT-CUS-026-SAVE | SCR-EVAL-SCORE-MGMT | 저장 버튼 | saveEvaluationScore / /api/admin/evaluation-scores/save | POST | 저장 toast 후 목록 refresh | field error 또는 403/409 표시 |
| ACT-CUS-027-LIST | SCR-PARTICIPATION-RATE-MGMT | 조회 버튼 | listParticipationRates / /api/admin/participation-rates | GET | 목록 refresh | 4xx ApiError 표시 |
| ACT-CUS-027-SAVE | SCR-PARTICIPATION-RATE-MGMT | 저장 버튼 | saveParticipationRate / /api/admin/participation-rates/save | POST | 저장 toast 후 목록 refresh | field error 또는 403/409 표시 |
| ACT-CUS-028-LIST | SCR-CALC-FORMULA-MGMT | 조회 버튼 | listCalculationFormulas / /api/admin/calculation-formulas | GET | 목록 refresh | 4xx ApiError 표시 |
| ACT-CUS-028-SAVE | SCR-CALC-FORMULA-MGMT | 저장 버튼 | saveCalculationFormula / /api/admin/calculation-formulas/save | POST | 저장 toast 후 목록 refresh | field error 또는 403/409 표시 |
| ACT-CUS-029-LIST | SCR-EVAL-RULE-SET-MGMT | 조회 버튼 | listEvaluationRuleSets / /api/admin/evaluation-rule-sets | GET | 목록 refresh | 4xx ApiError 표시 |
| ACT-CUS-029-SAVE | SCR-EVAL-RULE-SET-MGMT | 저장 버튼 | saveEvaluationRuleSet / /api/admin/evaluation-rule-sets/save | POST | 저장 toast 후 목록 refresh | field error 또는 403/409 표시 |
| ACT-CUS-030-LIST | SCR-JOURNAL-INDEXING-MGMT | 조회 버튼 | listJournalIndexingInfos / /api/admin/journal-indexing-infos | GET | 목록 refresh | 4xx ApiError 표시 |
| ACT-CUS-030-SAVE | SCR-JOURNAL-INDEXING-MGMT | 저장 버튼 | saveJournalIndexingInfo / /api/admin/journal-indexing-infos/save | POST | 저장 toast 후 목록 refresh | field error 또는 403/409 표시 |

## BASIC-34 requirement-to-screen trace

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-030 | REQ-992 | AC-02: 조건별 점수 조회 | SCR-EVAL-SCORE-MGMT | /admin/evaluation-scores | listEvaluationScores | 화면에서 업적영역·분류·관리항목·소속대학·적용연도 조건으로 평가점수를 조회한다. 결과를 확인한다. |
| CMN-FR-030 | REQ-993 | AC-01: 저장 후 재조회 | SCR-EVAL-SCORE-MGMT | /admin/evaluation-scores | saveEvaluationScore | 화면에서 평가점수·최대점수·적용기간을 등록·수정한다. 결과를 확인한다. |
| CMN-FR-030 | REQ-994 | AC-02: 조합별 분리 | SCR-EVAL-SCORE-MGMT | /admin/evaluation-scores | saveEvaluationScore | 화면에서 평가점수는 업적 조건과 소속대학·적용연도·적용기간의 조합으로 관리한다. 결과를 확인한다. |
| CMN-FR-031 | REQ-1000 | AC-02: 조합별 배분율 조회 | SCR-PARTICIPATION-RATE-MGMT | /admin/participation-rates | listParticipationRates | 화면에서 업적영역·분류·관리항목별 배분율을 조회한다. 결과를 확인한다. |
| CMN-FR-031 | REQ-1001 | AC-01: 저장 후 재조회 | SCR-PARTICIPATION-RATE-MGMT | /admin/participation-rates | saveParticipationRate | 화면에서 연구자수와 참여구분별 배분율을 등록·수정한다. 결과를 확인한다. |
| CMN-FR-031 | REQ-1002 | AC-03: 업적 조건·연구자수·참여구분 적용 | SCR-PARTICIPATION-RATE-MGMT | /admin/participation-rates | saveParticipationRate | 화면에서 배분율은 업적 조건·연구자수·참여구분의 조합에 따라 적용한다. 결과를 확인한다. |
| CMN-FR-032 | REQ-1008 | AC-01/AC-02: 버전별 산식 조회 | SCR-CALC-FORMULA-MGMT | /admin/calculation-formulas | listCalculationFormulas | 화면에서 계산식 버전별 산식과 변수를 조회한다. 결과를 확인한다. |
| CMN-FR-032 | REQ-1009 | AC-01: 명시 항목 저장 후 재조회 | SCR-CALC-FORMULA-MGMT | /admin/calculation-formulas | saveCalculationFormula | 화면에서 산식ID·변수·반올림 기준·상한·하한·적용연도를 등록·수정한다. 결과를 확인한다. |
| CMN-FR-032 | REQ-1010 | AC-03: 선택 버전 적용 | SCR-CALC-FORMULA-MGMT | /admin/calculation-formulas | saveCalculationFormula | 화면에서 원점수·배분점수·환산점수 계산에 적용할 버전을 관리한다. 결과를 확인한다. |
| FR-008 | REQ-1017 | AC-01: 값 저장 후 재조회 | SCR-EVAL-RULE-SET-MGMT | /admin/evaluation-rule-sets | listEvaluationRuleSets | 화면에서 업적평가 기준·점수규칙을 조회한다. 결과를 확인한다. |
| FR-008 | REQ-1018 | AC-01: 설정값 저장 | SCR-EVAL-RULE-SET-MGMT | /admin/evaluation-rule-sets | saveEvaluationRuleSet | 화면에서 업적평가 기준·점수규칙 설정값을 등록·수정한다. 결과를 확인한다. |
| FR-008 | REQ-1019 | AC-02: 적용 대상·사용상태 확인 | SCR-EVAL-RULE-SET-MGMT | /admin/evaluation-rule-sets | listEvaluationRuleSets | 화면에서 적용 대상과 사용상태를 확인한다. 결과를 확인한다. |
| FR-010 | REQ-1025 | AC-01: 저장값 재조회 | SCR-JOURNAL-INDEXING-MGMT | /admin/journal-indexing-infos | listJournalIndexingInfos | 화면에서 학술지·후보지 등재정보를 조회한다. 결과를 확인한다. |
| FR-010 | REQ-1026 | AC-01: 설정값 저장 | SCR-JOURNAL-INDEXING-MGMT | /admin/journal-indexing-infos | saveJournalIndexingInfo | 화면에서 학술지·후보지 등재정보 설정값을 등록·수정한다. 결과를 확인한다. |
| FR-010 | REQ-1027 | AC-02: 적용 대상·사용상태 확인 | SCR-JOURNAL-INDEXING-MGMT | /admin/journal-indexing-infos | listJournalIndexingInfos | 화면에서 적용 대상과 사용상태를 확인한다. 결과를 확인한다. |


## BASIC-35 Story-to-UI Projection
| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-01 | SCR-EVALUATION-DATE-MGMT list/form | B35-01A/B35-01B | REQ-1116, REQ-1117 | 조회 후 저장 결과 재조회 |
| US-02 | SCR-INPUT-PERIOD-MGMT list/form | B35-02A/B35-02B | REQ-1122, REQ-1123 | 조회 후 저장 결과 재조회 |
| US-03 | SCR-MODIFICATION-PERIOD-MGMT list/form | B35-03A/B35-03B | REQ-1128, REQ-1129 | 조회 후 저장 결과 재조회 |
| US-04 | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT list/form | B35-04A/B35-04B | REQ-1135, REQ-1136 | 조회 후 저장 결과 재조회 |
| US-05 | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT list/form | B35-05A/B35-05B | REQ-1142, REQ-1143 | 조회 후 저장 결과 재조회 |

## BASIC-35 UI 변경 추가
### Route Inventory 추가
| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story | slice_id |
|---|---|---|---|---|---|---|---|
| /admin/evaluation-dates | SCR-EVALUATION-DATE-MGMT | R03/R04/R09 | 평가 기준 관리 > 업무기간 관리 > 평가일자 관리 | listEvaluationDates / saveEvaluationDate | evaluation_date_settings | US-01 | CUS-031 |
| /admin/input-periods | SCR-INPUT-PERIOD-MGMT | R03/R04/R09 | 평가 기준 관리 > 업무기간 관리 > 입력기간 관리 | listInputPeriods / saveInputPeriod | input_period_settings | US-02 | CUS-032 |
| /admin/modification-periods | SCR-MODIFICATION-PERIOD-MGMT | R03/R04/R09 | 평가 기준 관리 > 업무기간 관리 > 수정기간 관리 | listModificationPeriods / saveModificationPeriod | modification_period_settings | US-03 | CUS-033 |
| /admin/department-chair-confirm-periods | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | R03/R04/R09 | 평가 기준 관리 > 업무기간 관리 > 학과장 확인기간 관리 | listDepartmentChairConfirmPeriods / saveDepartmentChairConfirmPeriod | department_chair_confirm_period_settings | US-04 | CUS-034 |
| /admin/business-periods | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | R03/R04/R09 | 평가 기준 관리 > 업무기간 관리 > 평가·업적입력 기간 관리 | listBusinessPeriods / saveBusinessPeriod | business_period_integrated_settings | US-05 | CUS-035 |

### Screen Action Mapping 추가
| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| B35-01A | SCR-EVALUATION-DATE-MGMT | [조회] 클릭 | listEvaluationDates / /api/admin/evaluation-dates | GET | 평가일자 관리 목록과 pagination 표시 | 401/403 permission, 400 ApiError banner |
| B35-01B | SCR-EVALUATION-DATE-MGMT | [저장] 클릭 후 확인 | saveEvaluationDate / /api/admin/evaluation-dates/save | POST | 저장 성공 메시지와 목록 재조회 | 필수값/기간중복/권한 오류를 field-level 또는 banner로 표시 |
| B35-02A | SCR-INPUT-PERIOD-MGMT | [조회] 클릭 | listInputPeriods / /api/admin/input-periods | GET | 입력기간 관리 목록과 pagination 표시 | 401/403 permission, 400 ApiError banner |
| B35-02B | SCR-INPUT-PERIOD-MGMT | [저장] 클릭 후 확인 | saveInputPeriod / /api/admin/input-periods/save | POST | 저장 성공 메시지와 목록 재조회 | 필수값/기간중복/권한 오류를 field-level 또는 banner로 표시 |
| B35-03A | SCR-MODIFICATION-PERIOD-MGMT | [조회] 클릭 | listModificationPeriods / /api/admin/modification-periods | GET | 수정기간 관리 목록과 pagination 표시 | 401/403 permission, 400 ApiError banner |
| B35-03B | SCR-MODIFICATION-PERIOD-MGMT | [저장] 클릭 후 확인 | saveModificationPeriod / /api/admin/modification-periods/save | POST | 저장 성공 메시지와 목록 재조회 | 필수값/기간중복/권한 오류를 field-level 또는 banner로 표시 |
| B35-04A | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | [조회] 클릭 | listDepartmentChairConfirmPeriods / /api/admin/department-chair-confirm-periods | GET | 학과장 확인기간 관리 목록과 pagination 표시 | 401/403 permission, 400 ApiError banner |
| B35-04B | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | [저장] 클릭 후 확인 | saveDepartmentChairConfirmPeriod / /api/admin/department-chair-confirm-periods/save | POST | 저장 성공 메시지와 목록 재조회 | 필수값/기간중복/권한 오류를 field-level 또는 banner로 표시 |
| B35-05A | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | [조회] 클릭 | listBusinessPeriods / /api/admin/business-periods | GET | 평가·업적입력 기간 관리 목록과 pagination 표시 | 401/403 permission, 400 ApiError banner |
| B35-05B | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | [저장] 클릭 후 확인 | saveBusinessPeriod / /api/admin/business-periods/save | POST | 저장 성공 메시지와 목록 재조회 | 필수값/기간중복/권한 오류를 field-level 또는 banner로 표시 |

### SCR-EVALUATION-DATE-MGMT Wireframe
```text
+ 평가 기준 관리 > 업무기간 관리 > 평가일자 관리                                      R03/R04/R09
| 검색조건: 평가연도 [____] 평가영역/소속/유형 [server options] [조회] |
| 목록: 적용조건 | 시작 | 종료 | 기준일/허용기능 | 사용여부 | 수정일시 |
| <evaluation_date_settings row from API>                                      |
| 입력: 평가연도* [____] 시작* [____] 종료* [____] 사용여부 [Y/N]      |
| 기준일/허용기능/적용구분 [source-backed fields] 변경사유* [____]     |
| [저장] [취소]  성공: 저장 후 listEvaluationDates 재조회                  |
| 상태: loading / empty / error / permission-denied / success          |
```
#### Wireframe Sample Value Ledger
| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<evaluation_date_settings row from API>` | SCR-EVALUATION-DATE-MGMT 목록 | API row placeholder | layout-only-sample | SPEC-REQUEST.md CMN-FR-033 데이터 요구사항 | DB/API 응답에서만 렌더링하고 seed로 하드코딩하지 않는다. |
| `Y/N` | SCR-EVALUATION-DATE-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | CMN-302 사용여부 | Y/N flag enum으로 사용 가능하다. |

### SCR-INPUT-PERIOD-MGMT Wireframe
```text
+ 평가 기준 관리 > 업무기간 관리 > 입력기간 관리                                      R03/R04/R09
| 검색조건: 평가연도 [____] 평가영역/소속/유형 [server options] [조회] |
| 목록: 적용조건 | 시작 | 종료 | 기준일/허용기능 | 사용여부 | 수정일시 |
| <input_period_settings row from API>                                      |
| 입력: 평가연도* [____] 시작* [____] 종료* [____] 사용여부 [Y/N]      |
| 기준일/허용기능/적용구분 [source-backed fields] 변경사유* [____]     |
| [저장] [취소]  성공: 저장 후 listInputPeriods 재조회                  |
| 상태: loading / empty / error / permission-denied / success          |
```
#### Wireframe Sample Value Ledger
| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<input_period_settings row from API>` | SCR-INPUT-PERIOD-MGMT 목록 | API row placeholder | layout-only-sample | SPEC-REQUEST.md CMN-FR-034 데이터 요구사항 | DB/API 응답에서만 렌더링하고 seed로 하드코딩하지 않는다. |
| `Y/N` | SCR-INPUT-PERIOD-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | CMN-302 사용여부 | Y/N flag enum으로 사용 가능하다. |

### SCR-MODIFICATION-PERIOD-MGMT Wireframe
```text
+ 평가 기준 관리 > 업무기간 관리 > 수정기간 관리                                      R03/R04/R09
| 검색조건: 평가연도 [____] 평가영역/소속/유형 [server options] [조회] |
| 목록: 적용조건 | 시작 | 종료 | 기준일/허용기능 | 사용여부 | 수정일시 |
| <modification_period_settings row from API>                                      |
| 입력: 평가연도* [____] 시작* [____] 종료* [____] 사용여부 [Y/N]      |
| 기준일/허용기능/적용구분 [source-backed fields] 변경사유* [____]     |
| [저장] [취소]  성공: 저장 후 listModificationPeriods 재조회                  |
| 상태: loading / empty / error / permission-denied / success          |
```
#### Wireframe Sample Value Ledger
| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<modification_period_settings row from API>` | SCR-MODIFICATION-PERIOD-MGMT 목록 | API row placeholder | layout-only-sample | SPEC-REQUEST.md CMN-FR-035 데이터 요구사항 | DB/API 응답에서만 렌더링하고 seed로 하드코딩하지 않는다. |
| `Y/N` | SCR-MODIFICATION-PERIOD-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | CMN-302 사용여부 | Y/N flag enum으로 사용 가능하다. |

### SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT Wireframe
```text
+ 평가 기준 관리 > 업무기간 관리 > 학과장 확인기간 관리                                      R03/R04/R09
| 검색조건: 평가연도 [____] 평가영역/소속/유형 [server options] [조회] |
| 목록: 적용조건 | 시작 | 종료 | 기준일/허용기능 | 사용여부 | 수정일시 |
| <department_chair_confirm_period_settings row from API>                                      |
| 입력: 평가연도* [____] 시작* [____] 종료* [____] 사용여부 [Y/N]      |
| 기준일/허용기능/적용구분 [source-backed fields] 변경사유* [____]     |
| [저장] [취소]  성공: 저장 후 listDepartmentChairConfirmPeriods 재조회                  |
| 상태: loading / empty / error / permission-denied / success          |
```
#### Wireframe Sample Value Ledger
| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<department_chair_confirm_period_settings row from API>` | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT 목록 | API row placeholder | layout-only-sample | SPEC-REQUEST.md CMN-FR-038 데이터 요구사항 | DB/API 응답에서만 렌더링하고 seed로 하드코딩하지 않는다. |
| `Y/N` | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | CMN-302 사용여부 | Y/N flag enum으로 사용 가능하다. |

### SCR-BUSINESS-PERIOD-INTEGRATED-MGMT Wireframe
```text
+ 평가 기준 관리 > 업무기간 관리 > 평가·업적입력 기간 관리                                      R03/R04/R09
| 검색조건: 평가연도 [____] 평가영역/소속/유형 [server options] [조회] |
| 목록: 적용조건 | 시작 | 종료 | 기준일/허용기능 | 사용여부 | 수정일시 |
| <business_period_integrated_settings row from API>                                      |
| 입력: 평가연도* [____] 시작* [____] 종료* [____] 사용여부 [Y/N]      |
| 기준일/허용기능/적용구분 [source-backed fields] 변경사유* [____]     |
| [저장] [취소]  성공: 저장 후 listBusinessPeriods 재조회                  |
| 상태: loading / empty / error / permission-denied / success          |
```
#### Wireframe Sample Value Ledger
| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<business_period_integrated_settings row from API>` | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT 목록 | API row placeholder | layout-only-sample | SPEC-REQUEST.md FR-014 데이터 요구사항 | DB/API 응답에서만 렌더링하고 seed로 하드코딩하지 않는다. |
| `Y/N` | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT 사용여부 selector | 사용여부 선택 | source-backed-enum | CMN-302 사용여부 | Y/N flag enum으로 사용 가능하다. |

### Requirement-to-Screen Trace 추가
| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| CMN-FR-033 | REQ-1116 | AC-REQ-1116 | SCR-EVALUATION-DATE-MGMT | /admin/evaluation-dates | listEvaluationDates / saveEvaluationDate | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-033 | REQ-1117 | AC-REQ-1117 | SCR-EVALUATION-DATE-MGMT | /admin/evaluation-dates | listEvaluationDates / saveEvaluationDate | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-033 | REQ-1121 | AC-REQ-1121 | SCR-EVALUATION-DATE-MGMT | /admin/evaluation-dates | listEvaluationDates / saveEvaluationDate | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-034 | REQ-1122 | AC-REQ-1122 | SCR-INPUT-PERIOD-MGMT | /admin/input-periods | listInputPeriods / saveInputPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-034 | REQ-1123 | AC-REQ-1123 | SCR-INPUT-PERIOD-MGMT | /admin/input-periods | listInputPeriods / saveInputPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-034 | REQ-1124 | AC-REQ-1124 | SCR-INPUT-PERIOD-MGMT | /admin/input-periods | listInputPeriods / saveInputPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-034 | REQ-1127 | AC-REQ-1127 | SCR-INPUT-PERIOD-MGMT | /admin/input-periods | listInputPeriods / saveInputPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-035 | REQ-1128 | AC-REQ-1128 | SCR-MODIFICATION-PERIOD-MGMT | /admin/modification-periods | listModificationPeriods / saveModificationPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-035 | REQ-1129 | AC-REQ-1129 | SCR-MODIFICATION-PERIOD-MGMT | /admin/modification-periods | listModificationPeriods / saveModificationPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-035 | REQ-1130 | AC-REQ-1130 | SCR-MODIFICATION-PERIOD-MGMT | /admin/modification-periods | listModificationPeriods / saveModificationPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-035 | REQ-1131 | AC-REQ-1131 | SCR-MODIFICATION-PERIOD-MGMT | /admin/modification-periods | listModificationPeriods / saveModificationPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-035 | REQ-1134 | AC-REQ-1134 | SCR-MODIFICATION-PERIOD-MGMT | /admin/modification-periods | listModificationPeriods / saveModificationPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-038 | REQ-1135 | AC-REQ-1135 | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | /admin/department-chair-confirm-periods | listDepartmentChairConfirmPeriods / saveDepartmentChairConfirmPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-038 | REQ-1136 | AC-REQ-1136 | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | /admin/department-chair-confirm-periods | listDepartmentChairConfirmPeriods / saveDepartmentChairConfirmPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-038 | REQ-1138 | AC-REQ-1138 | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | /admin/department-chair-confirm-periods | listDepartmentChairConfirmPeriods / saveDepartmentChairConfirmPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| CMN-FR-038 | REQ-1141 | AC-REQ-1141 | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | /admin/department-chair-confirm-periods | listDepartmentChairConfirmPeriods / saveDepartmentChairConfirmPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| FR-014 | REQ-1142 | AC-REQ-1142 | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | /admin/business-periods | listBusinessPeriods / saveBusinessPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| FR-014 | REQ-1143 | AC-REQ-1143 | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | /admin/business-periods | listBusinessPeriods / saveBusinessPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| FR-014 | REQ-1145 | AC-REQ-1145 | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | /admin/business-periods | listBusinessPeriods / saveBusinessPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| FR-014 | REQ-1146 | AC-REQ-1146 | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | /admin/business-periods | listBusinessPeriods / saveBusinessPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |
| FR-014 | REQ-1148 | AC-REQ-1148 | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | /admin/business-periods | listBusinessPeriods / saveBusinessPeriod | 조회/저장/오류 상태를 화면에서 확인한다. |


## BASIC-37 Story-to-UI Projection

| User Story | screen/region or modal | action_id | canonical_id | acceptance criterion |
|---|---|---|---|---|
| US-B37-01 | SCR-FACULTY-SEARCH-LIST 목록 | B14-034 | REQ-1149 | 교원 검색 목록 조회가 DTO 매핑 오류 없이 표시된다. |
| US-B37-01 | SCR-RESEARCHER-PROFILE-LIST 목록 | B14-035 | REQ-1150 | 연구자 프로필 목록 조회가 DTO 매핑 오류 없이 표시된다. |
| US-B37-01 | SCR-DEGREE-DEFICIENCY-TARGET-LIST 목록 | B14-036 | REQ-1151 | 선행학위 미충족 대상 조회가 DTO 매핑 오류 없이 표시된다. |
| US-B37-02 | SCR-BATCH-RESULT-MGMT 결과/로그 영역 | B14-037 | REQ-1153 | 배치 결과와 로그가 기존 화면에서 정상 표시된다. |
| US-B37-03 | SCR-UPLOAD-TEMPLATE-MGMT 목록 영역 | B14-038 | REQ-1154 | 업로드 양식 목록이 정상 표시된다. |
| US-B37-04 | 엑셀 관리 5개 screen 공통 layout | B14-039 | REQ-1155 | 공통 관리 화면 CSS/layout/state 적용을 확인한다. |
| US-B37-05 | SCR-COMMON-HEADER-NAV hover menu | B14-040 | REQ-1157 | header hover로 menu hierarchy를 확인한다. |
| US-B37-05 | SCR-COMMON-HEADER-NAV route reachability | B14-041 | REQ-1158 | 기존 모든 leaf menu에 도달한다. |
| US-B37-06 | SCR-COMMON-MENU-SEARCH input/result | B14-042 | REQ-1159 | 검색 키워드 입력과 실제 메뉴 filtering을 확인한다. |
| US-B37-06 | SCR-COMMON-MENU-SEARCH result navigation | B14-043 | REQ-1160 | 결과 선택 시 해당 route로 이동한다. |

## BASIC-37 UI 변경 추가

### Route Inventory 추가

| route | 화면ID | role | menu_path | operationId 또는 path | primary_entity | owning story | slice_id |
|---|---|---|---|---|---|---|---|
| /admin/researcher-profiles/faculty-search | SCR-FACULTY-SEARCH-LIST | R09 | 연구자 프로필 관리 > 교원 검색 목록 | listFacultySearchResults | faculty_search_results | US-B37-01 | CUS-036 |
| /admin/researcher-profiles | SCR-RESEARCHER-PROFILE-LIST | R09 | 연구자 프로필 관리 > 연구자 프로필 목록 | listResearcherProfiles | researcher_profiles | US-B37-01 | CUS-037 |
| /admin/researcher-profiles/degree-deficiencies | SCR-DEGREE-DEFICIENCY-TARGET-LIST | R09 | 연구자 프로필 관리 > 선행학위 미충족 대상 | listDegreeDeficiencyTargets | degree_deficiency_targets | US-B37-01 | CUS-038 |
| common shell header | SCR-COMMON-HEADER-NAV | authenticated | 공통 header navigation | N/A | menus | US-B37-05 | CUS-039 |
| common shell search | SCR-COMMON-MENU-SEARCH | authenticated | 공통 header menu search | N/A | menus | US-B37-06 | CUS-040 |

### Screen Action Mapping 추가

| action_id | 화면ID | trigger | operationId 또는 path | method | 성공 상태 | 오류 상태 |
|---|---|---|---|---|---|---|
| B14-034 | SCR-FACULTY-SEARCH-LIST | [조회] 클릭 | listFacultySearchResults | GET | 교원 검색 목록 표시 | 400/403/DTO mapping error는 ApiError banner |
| B14-035 | SCR-RESEARCHER-PROFILE-LIST | [조회] 클릭 | listResearcherProfiles | GET | 연구자 프로필 목록 표시 | 400/403/DTO mapping error는 ApiError banner |
| B14-036 | SCR-DEGREE-DEFICIENCY-TARGET-LIST | [조회] 클릭 | listDegreeDeficiencyTargets | GET | 선행학위 미충족 대상 목록 표시 | 400/403/DTO mapping error는 ApiError banner |
| B14-037 | SCR-BATCH-RESULT-MGMT | [조회] 또는 [로그 보기] | listBatchResults / getBatchResultLog | GET | 결과/로그 목록 표시 | 조회 오류 표시, 수정 CTA 없음 |
| B14-038 | SCR-UPLOAD-TEMPLATE-MGMT | route 진입 또는 [조회] 클릭 | listUploadTemplates | GET | 업로드 양식 목록 표시 | 조회 오류 표시 |
| B14-039 | 엑셀 관리 5개 screen | route render | N/A | N/A | 공통 관리 화면 CSS/state 적용 | 기능 확장 CTA가 보이면 regression 실패 |
| B14-040 | SCR-COMMON-HEADER-NAV | header top menu hover/focus | N/A | N/A | middle/leaf menu panel 표시 | 접근 가능 menu tree가 없으면 empty 안내 |
| B14-041 | SCR-COMMON-HEADER-NAV | leaf menu 선택 | N/A | navigation-only | 해당 route 이동 | 권한 없는 leaf는 표시하지 않음 |
| B14-042 | SCR-COMMON-MENU-SEARCH | keyword 입력 | N/A | local-only | 접근 가능한 실제 메뉴 결과 표시 | 입력 비활성 또는 결과 없음 안내 |
| B14-043 | SCR-COMMON-MENU-SEARCH | 검색 결과 선택 | N/A | navigation-only | 해당 route 이동 | 권한 없는 route는 결과에서 제외 |

### SCR-COMMON-HEADER-NAV Wireframe

```text
+ Header: 한국교원대학교 교수업적평가시스템 ------------------------------+
| [시스템 관리 v] [평가 기준 관리 v] [파일·데이터 관리 v] [보안·감사 관리 v] |
| 검색 [메뉴명 또는 경로 입력____________________]       사용자: <current user> |
| hover panel: top menu -> middle group -> leaf menu route                  |
| 예: 파일·데이터 관리 > 엑셀 관리 > 업로드 양식 관리                      |
| keyboard focus 이동과 hover 모두 같은 accessible menu tree를 사용한다.    |
+-------------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `<current user>` | SCR-COMMON-HEADER-NAV 사용자 영역 | API 응답 기반 사용자 자리 | layout-only-sample | current user response에서 제공 | DB/API 응답만 렌더링하고 하드코딩 금지 |
| `파일·데이터 관리 > 엑셀 관리 > 업로드 양식 관리` | hover panel 예시 menu_path | 기존 엑셀 메뉴 경로 예시 | source-backed-enum | 기존 Route Inventory BASIC-26 | 기존 menu seed/permissions에서 렌더링한다. |

### SCR-COMMON-MENU-SEARCH Wireframe

```text
+ Header menu search ------------------------------------------------------+
| 검색어 [배치_____________________________]                              |
| 결과                                                                    |
| - 시스템 운영 관리 > 배치작업 관리 > 배치 결과 조회  -> /admin/batch-results |
| - 파일·데이터 관리 > 엑셀 관리 > 업로드 양식 관리 -> /admin/excel-upload-templates |
| 권한 없는 메뉴는 결과에 표시하지 않는다.                                  |
+-------------------------------------------------------------------------+
```

#### Wireframe Sample Value Ledger

| sample value | screen/position | purpose in wireframe | authority | source/evidence | Code Agent rule |
|---|---|---|---|---|---|
| `배치` | SCR-COMMON-MENU-SEARCH 검색어 | keyword input 예시 | layout-only-sample | 요청은 검색 활성화를 요구하나 구체 keyword seed는 없음 | 테스트 입력값으로만 사용하고 menu seed로 만들지 않는다. |
| `시스템 운영 관리 > 배치작업 관리 > 배치 결과 조회` | 검색 결과 | 기존 batch menu_path 표시 | source-backed-enum | 기존 BASIC-23 Route Inventory | 기존 menu data에서 렌더링한다. |
| `파일·데이터 관리 > 엑셀 관리 > 업로드 양식 관리` | 검색 결과 | 기존 excel menu_path 표시 | source-backed-enum | 기존 BASIC-26 Route Inventory | 기존 menu data에서 렌더링한다. |

### Requirement-to-Screen Trace 추가

| source_id | canonical_id | acceptance criterion id | 화면ID | route | OpenAPI operationId/path | UI verification |
|---|---|---|---|---|---|---|
| null | REQ-1149 | AC-REQ-1149 | SCR-FACULTY-SEARCH-LIST | /admin/researcher-profiles/faculty-search | listFacultySearchResults | [조회] 후 교원 검색 행이 표시된다. |
| null | REQ-1150 | AC-REQ-1150 | SCR-RESEARCHER-PROFILE-LIST | /admin/researcher-profiles | listResearcherProfiles | [조회] 후 연구자 프로필 행이 표시된다. |
| null | REQ-1151 | AC-REQ-1151 | SCR-DEGREE-DEFICIENCY-TARGET-LIST | /admin/researcher-profiles/degree-deficiencies | listDegreeDeficiencyTargets | [조회] 후 선행학위 미충족 대상 행이 표시된다. |
| null | REQ-1155 | AC-REQ-1155 | 엑셀 관리 5개 screen | /admin/excel-upload-templates 외 4개 | 기존 BASIC-26 operation | 공통 관리 화면 CSS/layout/state 적용을 확인한다. |
| null | REQ-1157 | AC-REQ-1157 | SCR-COMMON-HEADER-NAV | common shell header | N/A | header hover/focus로 menu panel 표시를 확인한다. |
| null | REQ-1158 | AC-REQ-1158 | SCR-COMMON-HEADER-NAV | common shell header | N/A | 기존 route inventory leaf menu 전체 도달성을 확인한다. |
| null | REQ-1159 | AC-REQ-1159 | SCR-COMMON-MENU-SEARCH | common shell header | N/A | keyword 입력 후 accessible menu result를 확인한다. |
| null | REQ-1160 | AC-REQ-1160 | SCR-COMMON-MENU-SEARCH | common shell header | N/A | 검색 결과 선택 후 해당 route 이동을 확인한다. |
