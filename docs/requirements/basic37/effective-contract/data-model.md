# Data Model: 프로젝트 생성

이 문서는 `.specify/specs/001-feature/spec.md`, `plan.md`, `tasks.md`, `requirement-registry.json`, `SPEC-REQUEST.md`를 승인된 upstream truth로 사용하는 canonical domain data contract다. runnable DDL은 작성하지 않으며, 구현 단계 Flyway migration의 입력 계약만 정의한다.

## Entity Registry

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| users | 사용자 계정 | 로컬 DB 관리 대상 | REQ-003 |
| korus_personnel_snapshots | KORUS 교직원 Mock snapshot | KORUS Mock snapshot 조회 전용 제공 | REQ-002 |
| organizations | 조직 | 조직코드 기반 조직 관리 | REQ-016 |
| organization_relations | 조직 관계 | 조직 상위관계 적용기간 저장 | REQ-021 |
| organization_relation_history | 조직 관계 변경 이력 | 상위조직 변경 이력 보존 | REQ-018 |
| organization_user_mappings | 보직 또는 조직 사용자 매핑 | 최소 도메인별 DB 테이블 분리 | REQ-072 |
| roles | 역할 | R01~R09 역할 정의 | REQ-024 |
| user_roles | 사용자 역할 | 사용자 역할 부여 | REQ-030 |
| menus | 메뉴 | 메뉴 부모-자식 관계 관리 | REQ-044 |
| menu_execution_info | 메뉴 실행정보 | 메뉴 실행정보 관리 | REQ-049 |
| menu_permissions | 메뉴 권한 | 메뉴 접근 권한 설정 | REQ-036 |
| code_groups | 코드그룹 | 코드그룹 등록·수정 | REQ-053 |
| detail_codes | 상세코드 | 상세코드 관리 | REQ-057 |
| sessions | 세션 | 세션 쿠키 인증 경계 | REQ-093 |

| evaluation_rule_versions | 평가규정 버전 | B-1 확정 분류체계 참조와 작성중·확정·폐기 상태 | REQ-981, REQ-982 |
| evaluation_score_rules | 평가점수 관리 | BASIC-34 평가점수 관리 업무 기준정보 저장 | REQ-992, REQ-993, REQ-994 |
| participation_rate_rules | 참여구분·배분율 관리 | BASIC-34 참여구분·배분율 관리 업무 기준정보 저장 | REQ-1000, REQ-1001, REQ-1002 |
| calculation_formula_versions | 계산식 관리 | BASIC-34 계산식 관리 업무 기준정보 저장 | REQ-1008, REQ-1009, REQ-1010 |
| evaluation_rule_sets | 업적평가 기준·점수규칙 관리 | BASIC-34 업적평가 기준·점수규칙 관리 업무 기준정보 저장 | REQ-1017, REQ-1018, REQ-1019 |
| journal_indexing_infos | 학술지·후보지 등재정보 관리 | BASIC-34 학술지·후보지 등재정보 관리 업무 기준정보 저장 | REQ-1025, REQ-1026, REQ-1027 |

## Entity/Table Contract

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| users | user_id | bigint | PK, 내부 사용자 식별자 | - | - | 생성/수정/비활성화 메타정보 유지 | REQ-003 |
| users | login_id | varchar(100) | UNIQUE, 로그인 ID; seed admin은 `admin` | - | - | 변경 시 updated_at/updated_by 기록 | REQ-098 |
| users | password_hash | varchar(255) | 내부 계정 비밀번호 해시 | - | - | 직접 표시 금지, 변경 메타정보 유지 | REQ-093 |
| users | employee_no | varchar(50) | KORUS 교번 snapshot과 연결되는 업무 식별자 | - | korus_personnel_snapshots.employee_no | KORUS 원천값은 직접 수정 금지 | REQ-012 |
| users | system_use_yn | varchar(1) | 시스템 사용여부; local DB 관리 필드 | Y,N | - | 변경 전후 값, 처리자, 처리일시, 사유 추적 | REQ-013 |
| users | status | varchar(20) | 계정 상태값 | ACTIVE,INACTIVE,DELETED | - | 물리삭제보다 비활성화/논리삭제 우선 | REQ-074 |
| users | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| users | created_by | bigint | 생성자 | - | users.user_id | 생성 메타정보 | REQ-065 |
| users | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| users | updated_by | bigint | 수정자 | - | users.user_id | 수정 메타정보 | REQ-065 |
| users | change_reason | varchar(500) | 변경 사유 | - | - | 삭제성/수정성 처리 사유 추적 | REQ-065 |
| korus_personnel_snapshots | employee_no | varchar(50) | PK, KORUS 교번 | - | - | 조회 전용 snapshot, 직접 수정 금지 | REQ-002 |
| korus_personnel_snapshots | name | varchar(100) | 성명 검색/표시 필드 | - | - | KORUS 원천값 조회 전용 | REQ-010 |
| korus_personnel_snapshots | organization_code | varchar(50) | 소속 조직코드 | - | organizations.organization_code | KORUS 원천값 조회 전용 | REQ-010 |
| korus_personnel_snapshots | rank_name | varchar(100) | 직급 검색/표시 필드 | - | - | KORUS 원천값 조회 전용 | REQ-010 |
| korus_personnel_snapshots | employment_status | varchar(20) | 재직상태 검색/표시 필드 | ACTIVE,RETIRED,LEAVE | - | KORUS 원천값 조회 전용 | REQ-010 |
| korus_personnel_snapshots | position_name | varchar(100) | 보직 목록 표시 필드 | - | - | KORUS 원천값 조회 전용 | REQ-011 |
| korus_personnel_snapshots | retirement_date | date | 퇴직일자 목록 표시 필드 | - | - | KORUS 원천값 조회 전용 | REQ-011 |
| korus_personnel_snapshots | last_synced_at | timestamp | 최종 동기화일시 | - | - | Mock snapshot 갱신 시점 기록 | REQ-011 |
| korus_personnel_snapshots | status | varchar(20) | snapshot 상태값 | ACTIVE,INACTIVE | - | 상태값 유지 | REQ-073 |
| korus_personnel_snapshots | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| korus_personnel_snapshots | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| organizations | organization_code | varchar(50) | PK, 조직코드 기준 관리 | - | - | 변경 메타정보 유지 | REQ-016 |
| organizations | organization_name | varchar(200) | 조직명 | - | - | 변경 메타정보 유지 | REQ-019 |
| organizations | organization_type | varchar(30) | 대학·대학원·단과대학·학과·부서 구분 | UNIVERSITY,GRADUATE_SCHOOL,COLLEGE,DEPARTMENT,OFFICE | - | 상태/변경 메타정보 유지 | REQ-016 |
| organizations | system_use_yn | varchar(1) | 사용여부 | Y,N | - | 비활성화 우선 | REQ-073 |
| organizations | status | varchar(20) | 조직 상태값 | ACTIVE,INACTIVE,DELETED | - | 물리삭제보다 상태변경 우선 | REQ-074 |
| organizations | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| organizations | created_by | bigint | 생성자 | - | users.user_id | 처리자 추적 | REQ-065 |
| organizations | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| organizations | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| organizations | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| organization_relations | relation_id | bigint | PK, 조직 관계 식별자 | - | - | 관계 변경 이력과 연결 | REQ-021 |
| organization_relations | organization_code | varchar(50) | 하위 조직코드 | - | organizations.organization_code | 적용기간 변경 추적 | REQ-016 |
| organization_relations | parent_organization_code | varchar(50) | 상위 조직코드 | - | organizations.organization_code | 상위조직 변경 시 기존 row 종료 후 신규 row 추가 | REQ-018 |
| organization_relations | effective_start_date | date | 적용 시작일 | - | - | 종료일보다 늦을 수 없음 | REQ-017 |
| organization_relations | effective_end_date | date | 적용 종료일; null이면 현재 적용 | - | - | 시작일보다 빠르면 저장 차단 | REQ-022 |
| organization_relations | status | varchar(20) | 관계 상태 | ACTIVE,ENDED,DELETED | - | 물리삭제보다 종료상태/종료일 사용 | REQ-074 |
| organization_relations | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| organization_relations | created_by | bigint | 생성자 | - | users.user_id | 처리자 추적 | REQ-065 |
| organization_relations | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| organization_relations | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| organization_relations | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| organization_relation_history | history_id | bigint | PK, 이력 식별자 | - | - | 이력 row는 보존 대상 | REQ-018 |
| organization_relation_history | relation_id | bigint | 대상 관계 | - | organization_relations.relation_id | 관계 변경 이력 보존 | REQ-018 |
| organization_relation_history | organization_code | varchar(50) | 하위 조직코드 | - | organizations.organization_code | 이력 조회 기준 | REQ-009 |
| organization_relation_history | before_parent_organization_code | varchar(50) | 변경 전 상위조직 | - | organizations.organization_code | 변경 전 값 보존 | REQ-065 |
| organization_relation_history | after_parent_organization_code | varchar(50) | 변경 후 상위조직 | - | organizations.organization_code | 변경 후 값 보존 | REQ-065 |
| organization_relation_history | before_effective_start_date | date | 변경 전 시작일 | - | - | 변경 전 값 보존 | REQ-017 |
| organization_relation_history | before_effective_end_date | date | 변경 전 종료일 | - | - | 변경 전 값 보존 | REQ-017 |
| organization_relation_history | after_effective_start_date | date | 변경 후 시작일 | - | - | 변경 후 값 보존 | REQ-017 |
| organization_relation_history | after_effective_end_date | date | 변경 후 종료일 | - | - | 변경 후 값 보존 | REQ-017 |
| organization_relation_history | changed_at | timestamp | 처리일시 | - | - | 처리일시 추적 | REQ-065 |
| organization_relation_history | changed_by | bigint | 처리자 | - | users.user_id | 처리자 추적 | REQ-065 |
| organization_relation_history | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| organization_user_mappings | mapping_id | bigint | PK, 조직 사용자 매핑 식별자 | - | - | 상태/변경 메타정보 유지 | REQ-072 |
| organization_user_mappings | organization_code | varchar(50) | 조직코드 | - | organizations.organization_code | 조직 소속/보직 조회 연결 | REQ-010 |
| organization_user_mappings | user_id | bigint | 사용자 | - | users.user_id | 사용자 소속 연결 | REQ-010 |
| organization_user_mappings | position_name | varchar(100) | 보직명 | - | - | 보직 표시 지원 | REQ-011 |
| organization_user_mappings | mapping_type | varchar(20) | 조직 소속 또는 보직 구분 | ORGANIZATION,POSITION | - | 보직 기반 역할 구분 지원 | REQ-033 |
| organization_user_mappings | effective_start_date | date | 적용 시작일 | - | - | 유효기간 관리 | REQ-073 |
| organization_user_mappings | effective_end_date | date | 적용 종료일 | - | - | 유효기간 관리 | REQ-073 |
| organization_user_mappings | status | varchar(20) | 매핑 상태 | ACTIVE,INACTIVE,DELETED | - | 논리삭제 우선 | REQ-074 |
| organization_user_mappings | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| organization_user_mappings | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| roles | role_code | varchar(3) | PK, R01~R09 고정 역할코드 | R01,R02,R03,R04,R05,R06,R07,R08,R09 | - | 역할명 변경 후에도 code 불변 | REQ-028 |
| roles | role_name | varchar(100) | 역할명 | - | - | 역할명 변경 가능, code 불변 | REQ-024 |
| roles | purpose | varchar(500) | 역할별 목적 | - | - | 등록·변경 가능 | REQ-025 |
| roles | assignment_criteria | varchar(1000) | 부여 기준 | - | - | 등록·변경 가능 | REQ-027 |
| roles | default_data_scope | varchar(200) | 데이터 범위 기본값 | - | - | 등록·변경 가능 | REQ-027 |
| roles | system_use_yn | varchar(1) | 사용여부 | Y,N | - | 비활성화 우선 | REQ-073 |
| roles | status | varchar(20) | 역할 상태 | ACTIVE,INACTIVE | - | 신규 role_code 추가 제외 | REQ-029 |
| roles | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| roles | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| roles | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| roles | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| user_roles | assignment_id | bigint | PK, 사용자 역할 부여 식별자 | - | - | 변경/회수 이력의 기준 | REQ-030 |
| user_roles | user_id | bigint | 역할 대상 사용자 | - | users.user_id | 현재 역할 조회 기준 | REQ-034 |
| user_roles | role_code | varchar(3) | 부여 역할 | - | roles.role_code | R01~R09 역할 참조 | REQ-030 |
| user_roles | assignment_type | varchar(20) | 보직 기반 또는 수동 역할 구분 | POSITION,MANUAL | - | 목록에서 구분 표시 | REQ-033 |
| user_roles | valid_start_date | date | 역할 유효 시작일 | - | - | 유효기간 저장 | REQ-030 |
| user_roles | valid_end_date | date | 역할 유효 종료일 | - | - | 유효기간 저장 | REQ-031 |
| user_roles | approver_user_id | bigint | 승인자 겸 처리자 | - | users.user_id | 로그인 관리자 자동 기록 | REQ-035 |
| user_roles | status | varchar(20) | 역할 부여 상태 | ACTIVE,REVOKED,INACTIVE | - | 회수 시 ACTIVE -> REVOKED | REQ-032 |
| user_roles | revoked_at | timestamp | 회수 처리일시 | - | - | 회수 처리 정보 기록 | REQ-032 |
| user_roles | revoked_by | bigint | 회수 처리자 | - | users.user_id | 처리자 추적 | REQ-032 |
| user_roles | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| user_roles | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| user_roles | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| menus | menu_id | bigint | PK, 메뉴 식별자 | - | - | 변경 메타정보 유지 | REQ-044 |
| menus | parent_menu_id | bigint | 부모 메뉴 | - | menus.menu_id | 자기 자신/하위메뉴 부모 지정 차단 | REQ-047 |
| menus | menu_type | varchar(20) | 대메뉴·중메뉴·소메뉴/화면 구분 | MAIN,MIDDLE,SUB,SCREEN | - | 계층 조회/권한 단위 | REQ-046 |
| menus | menu_name | varchar(200) | 메뉴명 | - | - | 실행정보 관리 필드 | REQ-049 |
| menus | display_order | integer | 동일 계층 내 표시 순서 | - | - | 재정렬 시 저장 | REQ-045 |
| menus | screen_id | varchar(100) | 실행 화면ID | - | - | 메뉴와 실행 화면 연결 | REQ-052 |
| menus | url | varchar(300) | 실행 URL | - | - | 실행정보 조회/저장 | REQ-050 |
| menus | icon | varchar(100) | 아이콘 | - | - | 실행정보 저장 | REQ-051 |
| menus | business_category | varchar(100) | 업무구분 | - | - | 실행정보 저장 | REQ-051 |
| menus | description | varchar(1000) | 설명 | - | - | 실행정보 저장 | REQ-051 |
| menus | system_use_yn | varchar(1) | 사용여부 | Y,N | - | 사용 중 메뉴 물리삭제보다 비활성화 우선 | REQ-067 |
| menus | status | varchar(20) | 메뉴 상태 | ACTIVE,INACTIVE,DELETED | - | 논리삭제/비활성화 우선 | REQ-074 |
| menus | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| menus | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| menus | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| menus | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| menu_execution_info | menu_id | bigint | PK/FK, 실행정보 대상 메뉴 | - | menus.menu_id | 메뉴와 실행 화면 연결 보존 | REQ-049 |
| menu_execution_info | screen_id | varchar(100) | 화면ID | - | - | 메뉴 클릭 시 화면 식별 | REQ-052 |
| menu_execution_info | url | varchar(300) | URL | - | - | 실행정보 조회/저장 | REQ-050 |
| menu_execution_info | icon | varchar(100) | 아이콘 | - | - | 실행정보 저장 | REQ-051 |
| menu_execution_info | business_category | varchar(100) | 업무구분 | - | - | 실행정보 저장 | REQ-051 |
| menu_execution_info | description | varchar(1000) | 설명 | - | - | 실행정보 저장 | REQ-051 |
| menu_execution_info | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| menu_execution_info | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| menu_execution_info | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| menu_permissions | permission_id | bigint | PK, 메뉴 권한 식별자 | - | - | 변경 메타정보 유지 | REQ-036 |
| menu_permissions | target_type | varchar(20) | 권한 대상 유형 | ROLE,ORGANIZATION,USER | - | 우선순위 USER > ORGANIZATION > ROLE | REQ-043 |
| menu_permissions | target_id | varchar(100) | role_code, organization_code, user_id 중 대상 식별자 | - | - | 대상별 접근권한 조회 기준 | REQ-038 |
| menu_permissions | menu_id | bigint | 권한 대상 메뉴 | - | menus.menu_id | 화면 미노출/API 접근통제 공통 기준 | REQ-037 |
| menu_permissions | access_allowed | varchar(10) | 접근 허용 여부 | ALLOW,DENY | - | 명시적 차단은 허용보다 우선 | REQ-043 |
| menu_permissions | status | varchar(20) | 권한 row 상태 | ACTIVE,INACTIVE,DELETED | - | 논리삭제/비활성화 우선 | REQ-074 |
| menu_permissions | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| menu_permissions | created_by | bigint | 생성자 | - | users.user_id | 처리자 추적 | REQ-065 |
| menu_permissions | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| menu_permissions | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| menu_permissions | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| code_groups | group_id | varchar(100) | PK, 그룹ID | - | - | 등록 후 수정 가능 여부는 REQ-056 OQ로 보존 | REQ-053 |
| code_groups | group_name | varchar(200) | 명칭 | - | - | 등록·수정 가능 | REQ-053 |
| code_groups | description | varchar(1000) | 설명 | - | - | 등록·수정 가능 | REQ-053 |
| code_groups | managing_department | varchar(200) | 관리부서 | - | - | 등록·수정 가능 | REQ-053 |
| code_groups | system_use_yn | varchar(1) | 사용여부 | Y,N | - | 공통코드 사용여부 관리 | REQ-066 |
| code_groups | status | varchar(20) | 코드그룹 상태 | ACTIVE,INACTIVE,DELETED | - | 사용 중 코드는 비활성화 우선 | REQ-067 |
| code_groups | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| code_groups | created_by | bigint | 생성자 | - | users.user_id | 처리자 추적 | REQ-065 |
| code_groups | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| code_groups | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| code_groups | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| detail_codes | group_id | varchar(100) | PK/FK, 소속 코드그룹 | - | code_groups.group_id | 코드그룹별 조회/저장 기준 | REQ-059 |
| detail_codes | code_value | varchar(100) | PK, 코드값 | - | - | 코드값 관리 | REQ-066 |
| detail_codes | code_name | varchar(200) | 코드명 | - | - | 코드명 관리 | REQ-066 |
| detail_codes | parent_code_value | varchar(100) | 상위코드값 | - | detail_codes.code_value | 상위코드 계층 관리 | REQ-060 |
| detail_codes | sort_order | integer | 정렬순서 | - | - | 정렬순서 관리 | REQ-066 |
| detail_codes | additional_attributes | json | 추가속성; 구조·개수·형식은 REQ-062 OQ로 보존 | - | - | 확정 전 기본값/임의 매핑 생성 금지 | REQ-061 |
| detail_codes | system_use_yn | varchar(1) | 사용여부 | Y,N | - | 공통코드 사용여부 관리 | REQ-066 |
| detail_codes | valid_start_date | date | 유효 시작일 | - | - | 공통코드 유효기간 관리 | REQ-066 |
| detail_codes | valid_end_date | date | 유효 종료일 | - | - | 공통코드 유효기간 관리 | REQ-066 |
| detail_codes | status | varchar(20) | 상세코드 상태 | ACTIVE,INACTIVE,DELETED | - | 사용 중 코드는 물리삭제보다 비활성화 우선 | REQ-067 |
| detail_codes | created_at | timestamp | 생성일시 | - | - | 생성 메타정보 | REQ-073 |
| detail_codes | created_by | bigint | 생성자 | - | users.user_id | 처리자 추적 | REQ-065 |
| detail_codes | updated_at | timestamp | 수정일시 | - | - | 수정 메타정보 | REQ-073 |
| detail_codes | updated_by | bigint | 수정자 | - | users.user_id | 처리자 추적 | REQ-065 |
| detail_codes | change_reason | varchar(500) | 변경 사유 | - | - | 사유 추적 | REQ-065 |
| sessions | session_id | varchar(128) | PK, HttpOnly SameSite=Lax cookie 세션 식별자 | - | - | 로그아웃 시 폐기 | REQ-093 |
| sessions | user_id | bigint | 인증 사용자 | - | users.user_id | 현재 사용자 조회 기준 | REQ-075 |
| sessions | created_at | timestamp | 생성일시 | - | - | 세션 생성 메타정보 | REQ-073 |
| sessions | expires_at | timestamp | 만료일시 | - | - | 상태값 관리 | REQ-093 |
| sessions | status | varchar(20) | 세션 상태 | ACTIVE,EXPIRED,LOGGED_OUT | - | logout 시 ACTIVE -> LOGGED_OUT | REQ-075 |
| sessions | last_accessed_at | timestamp | 최종 접근일시 | - | - | 현재 사용자 조회 시 갱신 가능 | REQ-075 |

## Field Mutability Matrix

| Entity | Field group | Create | Update | Delete/Deactivate | canonical_id |
|---|---|---|---|---|---|
| users | KORUS-derived employee fields | forbidden except snapshot link | forbidden; only local `system_use_yn` and roles mutate | not applicable to KORUS source | REQ-012 |
| users | system_use_yn/status | seed or admin create | R09 can update through updateUserAccount | logical status change, no physical delete by default | REQ-013 |
| organizations | organization_code/type/name | seed/mock backed initial load | admin relation management must not corrupt KORUS read-only source | status/end-date based retention | REQ-016 |
| organization_relations | parent/effective dates | admin can create relation | admin can end old relation and add new relation | END/DELETED status instead of physical delete | REQ-017 |
| roles | role_code | seed R01~R09 only | immutable | new role_code creation excluded | REQ-028 |
| roles | role_name/purpose/assignment_criteria/default_data_scope | seed baseline | admin can update metadata | status inactive only | REQ-027 |
| user_roles | assignment rows | admin can assign | admin can change validity/role where allowed | revoke sets status=REVOKED | REQ-032 |
| menus | parent/display_order | seed baseline | admin can change parent and order after cycle validation | inactive/deleted status preferred | REQ-044 |
| menu_execution_info | screen/url/icon/business fields | create with menu | admin can update execution info | kept with menu; no separate physical delete contract | REQ-051 |
| menu_permissions | target/menu/access_allowed | admin can create/save | admin can overwrite effective settings | inactive/deleted status preferred | REQ-036 |
| code_groups | group_id | admin can create | REQ-056 OQ: mutability not finalized | inactive/deleted status preferred | REQ-056 |
| detail_codes | additional_attributes | admin can submit only source-defined structure | REQ-062 OQ: unknown structure blocks arbitrary mapping | inactive/deleted status preferred | REQ-062 |

## State Transition Contract

| Entity | State Field | Transition | Trigger | Guard | canonical_id |
|---|---|---|---|---|---|
| users | status | ACTIVE -> INACTIVE/DELETED | 사용여부 또는 삭제성 관리 | KORUS 원천 필드는 변경하지 않는다 | REQ-013 |
| organization_relations | status | ACTIVE -> ENDED | 상위조직 변경 | 기존 관계 종료 후 신규 관계 추가; 기간 중복 차단 | REQ-018 |
| user_roles | status | ACTIVE -> REVOKED | 역할 회수 | 승인자 겸 처리자를 로그인 관리자 자동 기록 | REQ-032 |
| menus | status | ACTIVE -> INACTIVE/DELETED | 메뉴 비활성화 | 사용 중 메뉴는 물리삭제보다 비활성화 우선 | REQ-067 |
| detail_codes | status | ACTIVE -> INACTIVE/DELETED | 상세코드 비활성화 | 사용 중 코드는 물리삭제보다 비활성화 우선 | REQ-067 |
| sessions | status | ACTIVE -> LOGGED_OUT | 로그아웃 | HttpOnly SameSite=Lax session cookie 폐기 | REQ-075 |

## Validation Rule Matrix

| Rule | Entity/Table | Field(s) | Rejection / Observable Result | canonical_id |
|---|---|---|---|---|
| KORUS 원천정보 직접 수정 금지 | users, korus_personnel_snapshots | employee_no/name/organization_code/rank_name/employment_status | source field mutation payload는 저장하지 않고 field-level error를 반환한다 | REQ-012 |
| 조직 적용기간 날짜 검증 | organization_relations | effective_start_date/effective_end_date | 종료일이 시작일보다 빠르면 저장 차단 및 기존 관계 유지 | REQ-022 |
| 조직 상위관계 기간 중복 차단 | organization_relations | organization_code/effective dates | 동일 조직의 겹치는 기간 관계 저장 차단 | REQ-023 |
| 역할코드 불변 | roles | role_code | roleName 변경 후에도 role_code는 변경되지 않는다 | REQ-028 |
| 신규 역할코드 제외 | roles | role_code | R01~R09 외 role_code create는 1차 범위 제외로 차단한다 | REQ-029 |
| 권한 우선순위 | menu_permissions | target_type/access_allowed | USER > ORGANIZATION > ROLE, 같은 우선순위 DENY가 ALLOW보다 우선 | REQ-043 |
| 메뉴 자기부모 차단 | menus | menu_id/parent_menu_id | parent_menu_id == menu_id 저장 차단 | REQ-047 |
| 메뉴 순환 차단 | menus | parent_menu_id | 하위 메뉴를 부모로 지정하는 저장 차단 | REQ-048 |
| 상세코드 추가속성 OQ | detail_codes | additional_attributes | 확정되지 않은 구조·개수·형식 또는 매핑 기준은 임의 저장/전송 금지 | REQ-062 |
| 모든 입력값 서버 검증 | all mutating tables | request fields | 서버에서 invalid field-level ApiError 반환 | REQ-069 |

## Seed Data Contract

| Seed item | Target table | Required value/evidence | canonical_id |
|---|---|---|---|
| 시드 관리자 계정 | users | login_id=`admin`, password credential=`admin`의 검증 가능 계정 | REQ-098 |
| 시드 관리자 역할 | user_roles | admin user에 R09 시스템관리자 역할 부여 | REQ-096 |
| 기준 역할 | roles | R01~R09 역할코드와 역할명 존재 | REQ-024 |
| 시스템 관리 메뉴 tree | menus | 대메뉴, 사용자·조직/역할·권한/메뉴/공통코드 중메뉴, 9개 소메뉴 | REQ-099 |
| 9개 화면 권한 | menu_permissions | R09가 9개 화면 접근 가능 | REQ-071 |
| 예시 조직 | organizations | 예시 조직 1개 이상 | REQ-099 |
| 예시 사용자/KORUS snapshot | users, korus_personnel_snapshots | 예시 사용자 1명 이상과 조회 전용 snapshot | REQ-099 |

## Screen/API Reference

| Screen/API | Primary Entity/Table | OpenAPI operationId 또는 path | canonical_id |
|---|---|---|---|
| SCR-LOGIN | sessions | login / logout / getCurrentUser | REQ-075 |
| SCR-USER-MGMT | users, korus_personnel_snapshots, user_roles | searchUsers / updateUserAccount / updateUserRoles | REQ-010 |
| SCR-ORG-MGMT | organizations, organization_relations, organization_relation_history | searchOrganizations / getOrganizationTree / saveOrganizationParentRelation / listOrganizationParentRelationHistory | REQ-019 |
| SCR-ROLE-MGMT | roles | listRoles / updateRole | REQ-026 |
| SCR-USER-ROLE-MGMT | user_roles | listUserRoleAssignments / assignUserRole / updateUserRole / revokeUserRole | REQ-030 |
| SCR-MENU-PERMISSION-MGMT | menu_permissions | listMenuPermissions / saveMenuPermissions | REQ-036 |
| SCR-MENU-STRUCTURE-MGMT | menus | getMenuTree / updateMenuParent / reorderMenus | REQ-046 |
| SCR-MENU-INFO-MGMT | menu_execution_info | getMenuExecution / updateMenuExecution | REQ-050 |
| SCR-CODE-GROUP-MGMT | code_groups | listCodeGroups / createCodeGroup / updateCodeGroup | REQ-054 |
| SCR-DETAIL-CODE-MGMT | detail_codes | listDetailCodes / createDetailCode / updateDetailCode | REQ-059 |

## Persistence Assumptions

- 실행·영속성 방식은 `plan.md`의 marker block에 따라 imperative + MyBatis + blocking이다. (REQ-088, REQ-090)
- PostgreSQL 16과 Flyway migration을 사용하지만 이 문서는 runnable DDL을 쓰지 않는다. (REQ-001, REQ-090)
- 별도 감사로그 메뉴/API/통합 감사 분석은 제외하며, 변경 전후 값·처리자·처리일시·사유는 업무 데이터 row 또는 이력 테이블의 최소 메타정보로 제한한다. (REQ-006, REQ-008, REQ-065)
- 실제 KORUS, SSO, 외부기관 API는 호출하지 않으며 `AuthenticationPort`, `PersonnelInformationPort` 뒤의 local/mock adapter로 대체한다. (REQ-091, REQ-092)

## 공통 계약 참조

| 항목 | 계약 | canonical_id |
|---|---|---|
| technology_stack.backend | Java 17, Spring Boot 3.3.x, Maven, MyBatis, PostgreSQL 16, executable JAR | REQ-088 |
| technology_stack.frontend | React 18, TypeScript, Vite 5, nginx static serving and `/api/*` reverse proxy | REQ-089 |
| technology_stack.infrastructure | Docker Compose services `backend`, `frontend`, `database`; Flyway migration | REQ-001, REQ-090, REQ-100 |
| version_bom | spring_boot 3.3.x; node 20.x; vite 5.x; react 18.x; postgres 16.x | REQ-088, REQ-089, REQ-090 |
| required_outputs | backend_dir=`backend`; frontend_dir=`frontend`; compose_file=`infra/docker-compose.yml`; health_endpoint=`/api/health` | REQ-001, REQ-101 |

## 요구 회계

| canonical_id | status | evidence |
|---|---|---|
| REQ-001 | data-constraint | `필수 산출물 생성`는 별도 신규 table 없이 관련 entity 제약으로 전달된다. |
| REQ-002 | covered | `korus_personnel_snapshots` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-003 | covered | `users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-004 | out-of-scope | 데이터 모델은 `범위 밖 메뉴/API 제한` 제외 범위에 대한 업무 테이블을 만들지 않는 방식으로 범위를 보존한다. |
| REQ-005 | out-of-scope | 데이터 모델은 `업무 데이터 종속 기능 제외` 제외 범위에 대한 업무 테이블을 만들지 않는 방식으로 범위를 보존한다. |
| REQ-006 | out-of-scope | 데이터 모델은 `파일·Excel·개인정보·감사·배치 제외` 제외 범위에 대한 업무 테이블을 만들지 않는 방식으로 범위를 보존한다. |
| REQ-007 | out-of-scope | `범위 밖 placeholder 기준 확인`는 미확정 항목으로, 확정 전 임의 field/table 확장을 금지한다. |
| REQ-008 | out-of-scope | `변경 추적과 감사로그 경계 확인`는 미확정 항목으로, 확정 전 임의 field/table 확장을 금지한다. |
| REQ-009 | covered | `organization_relation_history` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-010 | covered | `korus_personnel_snapshots, organization_user_mappings` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-011 | covered | `korus_personnel_snapshots, organization_user_mappings` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-012 | covered | `users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-013 | covered | `users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-014 | screen-only | `사용자 업무 역할 변경`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-015 | screen-only | `사용자 관리 화면 상태`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-016 | covered | `organization_relations, organizations` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-017 | covered | `organization_relation_history, organization_relations` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-018 | covered | `organization_relation_history, organization_relations` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-019 | covered | `organizations` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-020 | screen-only | `조직 계층 조회`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-021 | covered | `organization_relations` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-022 | covered | `organization_relations` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-023 | batch-only | `조직 상위관계 기간 중복 차단`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-024 | covered | `roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-025 | covered | `roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-026 | screen-only | `역할 목록과 목적 조회`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-027 | covered | `roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-028 | covered | `roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-029 | covered | `roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-030 | covered | `user_roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-031 | covered | `user_roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-032 | covered | `user_roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-033 | covered | `organization_user_mappings, user_roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-034 | covered | `user_roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-035 | covered | `user_roles` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-036 | covered | `menu_permissions` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-037 | covered | `menu_permissions` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-038 | covered | `menu_permissions` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-039 | screen-only | `조직별 메뉴 권한 조회`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-040 | screen-only | `사용자별 메뉴 권한 조회`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-041 | batch-only | `권한 없는 메뉴 숨김`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-042 | batch-only | `권한 없는 보호 API 차단`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-043 | covered | `menu_permissions` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-044 | covered | `menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-045 | covered | `menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-046 | covered | `menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-047 | covered | `menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-048 | batch-only | `메뉴 순환 계층 차단`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-049 | covered | `menu_execution_info, menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-050 | covered | `menu_execution_info, menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-051 | covered | `menu_execution_info, menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-052 | covered | `menu_execution_info, menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-053 | covered | `code_groups` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-054 | screen-only | `코드그룹 조회`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-055 | screen-only | `코드그룹 상세코드 연결`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-056 | out-of-scope | `그룹ID 수정 가능 여부 확인`는 미확정 항목으로, 확정 전 임의 field/table 확장을 금지한다. |
| REQ-057 | covered | `detail_codes` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-058 | screen-only | `상세코드 선택값·연계 매핑 제공`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-059 | covered | `detail_codes` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-060 | covered | `detail_codes` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-061 | covered | `detail_codes` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-062 | out-of-scope | `상세코드 추가속성 구조 확인`는 미확정 항목으로, 확정 전 임의 field/table 확장을 금지한다. |
| REQ-063 | screen-only | `권한은 역할별 메뉴 접근 권한과 기능 권한을 기준으`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-064 | data-constraint | `내부 시스템 계정, 사용여부, 역할, 메뉴, 권한,`는 별도 신규 table 없이 관련 entity 제약으로 전달된다. |
| REQ-065 | covered | `code_groups, detail_codes, menu_execution_info, menu_permissions, menus, organization_relation_history, organization_relations, organizations, roles, user_roles, users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-066 | covered | `code_groups, detail_codes` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-067 | covered | `code_groups, detail_codes, menus` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-068 | screen-only | `화면은 검색조건, 목록, 상세, 등록/수정 폼, 오`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-069 | batch-only | `모든 입력값은 서버에서 검증한다.`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-070 | batch-only | `브라우저 코드는 localhost 또는 Docker`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-071 | screen-only | `시드 관리자 계정으로 로그인하여 1차 목표 메뉴 전`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-072 | covered | `organization_user_mappings` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-073 | covered | `code_groups, detail_codes, korus_personnel_snapshots, menu_execution_info, menu_permissions, menus, organization_relations, organization_user_mappings, organizations, roles, sessions, user_roles, users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-074 | covered | `menu_permissions, menus, organization_relations, organization_user_mappings, organizations, users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-075 | covered | `sessions` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-076 | batch-only | `보호 API 인증 세션 요구`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-077 | batch-only | `401/403 구분`는 데이터 필드 추가보다 OpenAPI operation/검증 규칙에서 주로 충족된다. |
| REQ-078 | screen-only | `로그인 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-079 | screen-only | `사용자 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-080 | screen-only | `조직 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-081 | screen-only | `역할 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-082 | screen-only | `사용자 역할 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-083 | screen-only | `메뉴 권한 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-084 | screen-only | `메뉴 구조 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-085 | screen-only | `메뉴 정보 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-086 | screen-only | `코드그룹 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-087 | screen-only | `상세코드 관리 화면`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-088 | non-functional | `Backend 기술스택`는 데이터 구조가 아니라 공통 계약 참조 또는 실행 검증에서 다루는 기술/운영 제약이다. |
| REQ-089 | non-functional | `Frontend 기술스택`는 데이터 구조가 아니라 공통 계약 참조 또는 실행 검증에서 다루는 기술/운영 제약이다. |
| REQ-090 | non-functional | `Infra 기술스택`는 데이터 구조가 아니라 공통 계약 참조 또는 실행 검증에서 다루는 기술/운영 제약이다. |
| REQ-091 | non-functional | `실제 KORUS, SSO, 외부기관 API 접속 없`는 데이터 구조가 아니라 공통 계약 참조 또는 실행 검증에서 다루는 기술/운영 제약이다. |
| REQ-092 | non-functional | `업무 확장은 `AuthenticationPort`,`는 데이터 구조가 아니라 공통 계약 참조 또는 실행 검증에서 다루는 기술/운영 제약이다. |
| REQ-093 | covered | `sessions, users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-094 | screen-only | `시드 관리자 계정을 반드시 제공한다.`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-095 | non-functional | `시드 관리자 계정은 로컬 Docker Compose`는 데이터 구조가 아니라 공통 계약 참조 또는 실행 검증에서 다루는 기술/운영 제약이다. |
| REQ-096 | screen-only | `시드 관리자는 R09 시스템관리자 역할을 가진다.`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-097 | screen-only | `시드 관리자 계정 정보는 README 또는 quic`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-098 | covered | `users` table/field contract가 이 요구의 데이터 구조를 정의한다. |
| REQ-099 | data-constraint | `최소 시드 데이터`는 별도 신규 table 없이 관련 entity 제약으로 전달된다. |
| REQ-100 | non-functional | `WHEN Docker Compose 실행이 요청되면`는 데이터 구조가 아니라 공통 계약 참조 또는 실행 검증에서 다루는 기술/운영 제약이다. |
| REQ-101 | screen-only | `Health API`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |
| REQ-102 | screen-only | `THE DELIVERABLE SHALL README`는 data-model.md의 화면/API 참조에 연결되지만 주 소유 artifact는 UI/API 설계다. |


## BASIC-7 변경 추가 데이터 계약

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| menus | exposure_start_at | timestamp | nullable, 메뉴 노출 시작일시 | - | - | 변경 시 updated_at/updated_by/change_reason 기록 | REQ-115 |
| menus | exposure_end_at | timestamp | nullable, 메뉴 노출 종료일시 | - | - | 종료 후 미노출·직접접근 차단 판단에 사용 | REQ-115 |
| detail_codes | usage_change_reason | varchar(500) | 사용기간/사용여부 변경 사유 | - | - | 과거 업무자료 code_value는 변경하지 않음 | REQ-119 |
| common_settings | setting_key | varchar(100) | PK, 설정 항목 키 | SESSION_IDLE_MINUTES,PAGE_SIZE_DEFAULT,DEFAULT_SEARCH_PERIOD_DAYS,LARGE_QUERY_THRESHOLD,LONG_RUNNING_TASK_THRESHOLD | - | 변경 메타정보 유지 | REQ-123 |
| common_settings | setting_value | varchar(200) | 필수, 항목 단위에 맞는 값 | - | - | 변경 전후 값과 수정자 기록 | REQ-122 |
| common_settings | setting_unit | varchar(50) | 항목 단위 | MINUTES,ROWS,DAYS,COUNT,SECONDS | - | 항목별 의미 보존 | REQ-122 |
| base_year_settings | base_year | integer | PK, 기준 연도 | - | - | 기존 평가자료 삭제 금지 | REQ-127 |
| base_year_settings | current_evaluation_year | integer | 현재 평가연도 | - | - | 변경 메타정보 유지 | REQ-125 |
| base_year_settings | default_search_year | integer | 기본 조회연도 | - | - | 사용자 화면 기본값 제공 | REQ-128 |
| standard_year_preparation_history | preparation_id | bigint | PK | - | base_year_settings.base_year | 복사/초기화 실행 이력 보존 | REQ-126 |
| standard_year_preparation_history | base_year | integer | 대상 연도 | - | base_year_settings.base_year | 기존 연도 평가자료 변경 금지 | REQ-126 |
| standard_year_preparation_history | copy_requested_yn | varchar(1) | 기준정보 복사 여부 | Y,N | - | 처리자/처리일시 기록 | REQ-126 |
| standard_year_preparation_history | initialize_requested_yn | varchar(1) | 초기화 여부 | Y,N | - | 처리자/처리일시 기록 | REQ-126 |

### BASIC-7 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| common_settings | 공통 환경설정 | 세션·조회·검색·작업 기준값 관리 | REQ-121 |
| base_year_settings | 기준연도 설정 | 현재 평가연도와 기본 조회연도 관리 | REQ-125 |
| standard_year_preparation_history | 연도별 기준정보 준비 이력 | 복사·초기화 지정과 기존 평가자료 보존 증거 | REQ-126 |

### BASIC-7 Screen/API Reference 추가

| screen_id | route | operationId/path | primary entity/table | canonical_id |
|---|---|---|---|---|
| SCR-MENU-USAGE-MGMT | `/admin/menu-usage` | listMenuExposureSettings `/api/admin/menus/exposure`; saveMenuExposureSettings `/api/admin/menus/exposure-save` | menus | REQ-112, REQ-113 |
| SCR-CODE-USAGE-MGMT | `/admin/detail-code-usage` | listDetailCodeUsageSettings `/api/admin/code-groups/{groupId}/codes-usage`; updateDetailCodeUsageSetting `/api/admin/code-groups/{groupId}/codes/{codeValue}/usage` | detail_codes | REQ-117, REQ-118 |
| SCR-COMMON-SETTINGS | `/admin/common-settings` | listCommonSettings `/api/admin/system-settings/common`; saveCommonSettings `/api/admin/system-settings/common-values` | common_settings | REQ-121, REQ-122 |
| SCR-BASE-YEAR-MGMT | `/admin/base-years` | listBaseYearSettings `/api/admin/system-settings/base-years`; saveBaseYearSettings `/api/admin/system-settings/base-year-current`; prepareBaseYearStandards `/api/admin/system-settings/base-years/{baseYear}/standards-preparation` | base_year_settings, standard_year_preparation_history | REQ-125, REQ-126 |


## BASIC-14 변경 추가 데이터 계약

### BASIC-14 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| function_permissions | 기능 권한 | 화면×역할×기능구분별 허용/차단 설정 | REQ-149 |
| period_permission_links | 기간별 권한 연결 | 업무기간과 기능 권한의 기간 전/중/후 effective 상태 연결 | REQ-153 |
| temporary_permissions | 임시 권한 | 특정 교원·업무자료·기능·유효기간의 임시 예외 권한 | REQ-157 |
| permission_change_history | 권한 변경 이력 | 권한 부여·변경·회수 변경 전후 값 영구 보존 | REQ-161 |

### BASIC-14 Entity/Table Contract 추가

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| function_permissions | function_permission_id | bigint | PK | - | - | 변경 시 permission_change_history 기록 | REQ-149 |
| function_permissions | screen_id | varchar(100) | 필수, 화면 단위 | - | menus.screen_id | 메뉴 진입 권한이 아니라 화면 기능 권한 단위 | REQ-148 |
| function_permissions | role_code | varchar(3) | 필수, R01~R09 참조 | R01,R02,R03,R04,R05,R06,R07,R08,R09 | roles.role_code | 기존 역할 정의 변경 금지 | REQ-172 |
| function_permissions | function_type | varchar(50) | 필수, 기능구분 독립 저장 key | READ,CREATE,UPDATE,DELETE,EXECUTE | - | 다른 function_type row를 덮어쓰지 않음 | REQ-152 |
| function_permissions | permission_allowed | varchar(10) | 필수 | ALLOW,DENY | - | 변경 전후 값 이력 기록 | REQ-149 |
| function_permissions | change_reason | varchar(500) | 저장 사유 | - | - | permission_change_history.reason으로 복사 | REQ-162 |
| period_permission_links | period_permission_link_id | bigint | PK | - | - | 변경 시 permission_change_history 기록 | REQ-153 |
| period_permission_links | business_period_id | varchar(100) | 기존 업무기간 식별자 참조; 기간 자체 기준 정의 금지 | - | 기존 기간 기준 | 기간 정의 row 생성 금지 | REQ-168 |
| period_permission_links | function_permission_id | bigint | 필수 | - | function_permissions.function_permission_id | 기능 권한과 기간 조건 연결 | REQ-153 |
| period_permission_links | effective_start_at | timestamp | 필수 | - | - | 서버 처리 시점 판정 기준 | REQ-170 |
| period_permission_links | effective_end_at | timestamp | nullable | - | - | 종료 후 변경성 기능 차단 | REQ-156 |
| period_permission_links | period_state | varchar(20) | derived effective state | BEFORE,ACTIVE,AFTER | - | 기간 조건이 임시 권한보다 우선 | REQ-169 |
| temporary_permissions | temporary_permission_id | bigint | PK | - | - | 만료 후 회수 상태 보존 | REQ-157 |
| temporary_permissions | user_id | bigint | 대상 교원/사용자 | - | users.user_id | user_roles 기본 역할 변경 금지 | REQ-160 |
| temporary_permissions | work_data_ref | varchar(200) | 업무자료 식별자 | - | 기존 업무자료 | 교수업적평가·학술지원금 개별 규칙은 저장하지 않음 | REQ-174 |
| temporary_permissions | function_type | varchar(50) | 지정 기능 | READ,CREATE,UPDATE,DELETE,EXECUTE | function_permissions.function_type | 지정 기능 외 허용 금지 | REQ-158 |
| temporary_permissions | valid_start_at | timestamp | 필수 | - | - | 기간 조건 내에서만 예외 적용 | REQ-169 |
| temporary_permissions | valid_end_at | timestamp | 필수 | - | - | 종료 시 자동 회수 | REQ-159 |
| temporary_permissions | status | varchar(20) | 상태 | ACTIVE,REVOKED,EXPIRED | - | 만료 자동 회수 후 EXPIRED 유지 | REQ-159 |
| permission_change_history | permission_history_id | bigint | PK | - | - | 영구 보존, 수정/삭제 금지 | REQ-171 |
| permission_change_history | target_type | varchar(50) | 권한 유형 | ROLE,MENU,FUNCTION,DATA_SCOPE,TEMPORARY | - | 검색조건 | REQ-163 |
| permission_change_history | target_id | varchar(200) | 대상 식별자 | - | related permission table | 검색조건 | REQ-163 |
| permission_change_history | before_value | json | 변경 전 값 | - | - | 조회 전용 | REQ-161 |
| permission_change_history | after_value | json | 변경 후 값 | - | - | 조회 전용 | REQ-161 |
| permission_change_history | changed_by | bigint | 처리자 | - | users.user_id | 처리자 영구 보존 | REQ-162 |
| permission_change_history | reason | varchar(500) | 사유 | - | - | 사유 영구 보존 | REQ-162 |
| permission_change_history | changed_at | timestamp | 변경일시 | - | - | 영구 조회 기준 | REQ-162 |

### BASIC-14 Validation Rule Matrix 추가

| Rule | Entity/Table | Field(s) | Rejection / Observable Result | canonical_id |
|---|---|---|---|---|
| 기능구분 독립 저장 | function_permissions | screen_id, role_code, function_type | 같은 screen_id/role_code의 다른 function_type row는 변경하지 않는다 | REQ-152 |
| 직접 API 기능 권한 차단 | function_permissions | permission_allowed | DENY면 보호 API는 403 ApiError를 반환하고 대상 업무 row를 변경하지 않는다 | REQ-151 |
| 기간 조건 우선 | period_permission_links, temporary_permissions | effective_start_at/effective_end_at, valid_start_at/valid_end_at | 기간 밖 변경성 요청은 임시 권한이 있어도 차단된다 | REQ-169 |
| 처리 시점 기간 판정 | period_permission_links | effective_end_at | 서버 처리 시점 종료 상태이면 열린 화면의 저장 요청도 차단된다 | REQ-170 |
| 평가확정 수정·삭제 차단 | function_permissions | target data status | target status가 평가확정이면 모든 역할의 수정·삭제를 차단한다 | REQ-166 |
| 이력 불변·영구 보존 | permission_change_history | all fields | update/delete mapper와 API를 만들지 않고 조회만 허용한다 | REQ-164, REQ-171 |

### BASIC-14 Seed Data Contract 추가

| Seed item | Target table | Required value/evidence | canonical_id |
|---|---|---|---|
| 기능 권한 화면 메뉴 seed | menus, menu_permissions | R09가 `SCR-FUNCTION-PERMISSION-MGMT` `/admin/function-permissions` 접근 가능 | REQ-148 |
| 기간별 권한 화면 메뉴 seed | menus, menu_permissions | R09가 `SCR-PERIOD-PERMISSION-MGMT` `/admin/period-permissions` 접근 가능 | REQ-153 |
| 임시 권한 화면 메뉴 seed | menus, menu_permissions | R09 또는 단과대학 담당자 검증용 사용자가 `SCR-TEMPORARY-PERMISSION-MGMT` 접근 가능 | REQ-157 |
| 권한 이력 화면 메뉴 seed | menus, menu_permissions | R09가 `SCR-PERMISSION-HISTORY` `/admin/permission-history` 접근 가능 | REQ-161 |

### BASIC-14 Screen/API Reference 추가

| screen_id | route | operationId/path | primary entity/table | canonical_id |
|---|---|---|---|---|
| SCR-FUNCTION-PERMISSION-MGMT | `/admin/function-permissions` | listFunctionPermissions `/api/admin/function-permissions`; saveFunctionPermissions `/api/admin/function-permissions-save`; evaluateFunctionPermission `/api/admin/function-permissions/evaluate` | function_permissions | REQ-148, REQ-149, REQ-150, REQ-151, REQ-152 |
| SCR-PERIOD-PERMISSION-MGMT | `/admin/period-permissions` | listPeriodPermissions `/api/admin/period-permissions`; savePeriodPermissions `/api/admin/period-permissions-save` | period_permission_links | REQ-153, REQ-154, REQ-155, REQ-156, REQ-169, REQ-170 |
| SCR-TEMPORARY-PERMISSION-MGMT | `/admin/temporary-permissions` | listTemporaryPermissions `/api/admin/temporary-permissions`; createTemporaryPermission `/api/admin/temporary-permissions-create` | temporary_permissions | REQ-157, REQ-158, REQ-159, REQ-160 |
| SCR-PERMISSION-HISTORY | `/admin/permission-history` | listPermissionChangeHistory `/api/admin/permission-history` | permission_change_history | REQ-161, REQ-162, REQ-163, REQ-164, REQ-171 |


## BASIC-22 변경 추가 데이터 계약

### BASIC-22 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| message_codes | 메시지코드 | 상황별 메시지코드와 사용자 문구 관리 | REQ-240 |
| notices | 공지사항 | 평가일정·점검·업무안내 공지 본문과 게시기간 관리 | REQ-263 |
| notice_targets | 공지 대상 | 공지 대상 역할과 조직 조건 관리 | REQ-267 |
| notice_attachments | 공지 첨부파일 | 공지 첨부 원본 파일명과 내부 저장 식별자 관리 | REQ-270 |
| help_contents | 도움말 | screen_id별 업무 설명·입력 기준·FAQ·연락처 관리 | REQ-303 |
| manuals | 매뉴얼 | 매뉴얼 유형·버전·대상 사용자·시행일 관리 | REQ-329 |
| manual_files | 매뉴얼 파일 | 매뉴얼 원본 파일명과 내부 저장 식별자 관리 | REQ-335 |

### BASIC-22 Entity/Table Contract 추가

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| message_codes | message_code | varchar(100) | PK | - | - | 변경 시 updated_at/updated_by 기록 | REQ-242 |
| message_codes | message_type | varchar(50) | 필수 | SAVE,DELETE,APPROVAL,REJECT,ERROR,SESSION_EXPIRED | - | 유형 변경 메타정보 유지 | REQ-248 |
| message_codes | user_message | varchar(1000) | 필수 | - | - | 시스템 로그 문구와 분리 | REQ-245 |
| notices | notice_id | bigint | PK | - | - | 논리삭제/게시기간 종료 우선 | REQ-263 |
| notices | title | varchar(200) | 필수 | - | - | 변경 메타정보 유지 | REQ-264 |
| notices | publish_start_date | date | 필수 | - | - | 게시기간 노출 판정 기준 | REQ-265 |
| notices | publish_end_date | date | 필수 | - | - | 게시기간 밖 미노출 | REQ-279 |
| notices | important_yn | varchar(1) | 필수 | Y,N | - | 변경 메타정보 유지 | REQ-269 |
| notice_targets | target_type | varchar(20) | ROLE 또는 ORGANIZATION | ROLE,ORGANIZATION | roles/organizations | 역할/조직 기준정보 변경 금지 | REQ-277 |
| notice_targets | target_id | varchar(100) | 필수 대상 식별자 | - | roles/organizations | 하위 조직 포함 판정은 조직 계층 참조 | REQ-278 |
| notice_attachments | original_file_name | varchar(255) | 원본 파일명 보존 | - | notices.notice_id | 실제 파일명/경로 외부 노출 금지 | REQ-299 |
| help_contents | screen_id | varchar(100) | UNIQUE | - | menus.screen_id | 동일 screen_id는 하나의 현재 항목 | REQ-313 |
| help_contents | business_description | text | 업무 설명 | - | - | 등록/수정 메타정보 기록 | REQ-304 |
| help_contents | input_criteria | text | 입력 기준 | - | - | 실제 입력 검증규칙 변경 금지 | REQ-305 |
| help_contents | faq | text | 자주 묻는 질문 | - | - | 사용자 매뉴얼 파일과 분리 | REQ-306 |
| help_contents | contact | varchar(200) | 연락처 | - | - | 등록/수정 메타정보 기록 | REQ-307 |
| manuals | manual_type | varchar(50) | 필수 | USER,ADMIN | - | 화면별 도움말/FAQ와 분리 | REQ-331 |
| manuals | version | varchar(50) | manual_type+target_user+version UNIQUE | - | - | 중복 등록 차단 | REQ-332 |
| manuals | target_user | varchar(100) | 필수 | - | roles.role_code 또는 사용자 유형 | R01~R09 재정의 금지 | REQ-333 |
| manuals | effective_date | date | 필수 시행일 | - | - | 최신 버전 선택 기준 | REQ-334 |
| manual_files | original_file_name | varchar(255) | 원본 파일명 보존 | - | manuals.manual_id | 파일 내용 자동 수정 금지 | REQ-354 |

### BASIC-22 Validation Rule Matrix 추가

| Rule | Entity/Table | Field(s) | Rejection / Observable Result | canonical_id |
|---|---|---|---|---|
| 공지 대상 필수 | notice_targets | target_type,target_id | 대상 역할 또는 대상 조직이 없으면 저장 차단 | REQ-267 |
| 공지 노출 조건 | notices, notice_targets | publish_start_date,publish_end_date,target_id | 기간·역할·조직을 모두 만족하지 않으면 목록 미표시 | REQ-279 |
| 도움말 screen_id 유일성 | help_contents | screen_id | 동일 screen_id는 하나의 현재 항목으로 저장/조회 | REQ-313 |
| 매뉴얼 중복 버전 차단 | manuals | manual_type,target_user,version | 동일 조합 중복 저장 시 기존 row 유지 및 ApiError 반환 | REQ-343 |

### BASIC-22 Seed Data Contract 추가

| Seed item | Target table | Required value/evidence | canonical_id |
|---|---|---|---|
| 메시지 유형 seed | message_codes | 저장, 삭제, 승인, 반려, 오류, 세션만료 유형 예시 | REQ-248 |
| 공지 검증 seed | notices, notice_targets | R09 및 예시 조직 기준 게시기간 내/밖 공지 | REQ-279 |
| 도움말 검증 seed | help_contents | 기존 screen_id 하나와 도움말 없는 screen_id 하나 | REQ-313 |
| 매뉴얼 검증 seed | manuals | 같은 manual_type/target_user의 서로 다른 version/effective_date | REQ-339 |

### BASIC-22 Screen/API Reference 추가

| screen_id | route | operationId/path | primary entity/table | canonical_id |
|---|---|---|---|---|
| SCR-MESSAGE-MGMT | `/admin/messages` | listMessages `/api/admin/system-settings/messages`; saveMessage `/api/admin/system-settings/messages/{messageCode}`; getMessageText `/api/system/messages/{messageCode}` | message_codes | REQ-240, REQ-241, REQ-242, REQ-243, REQ-244, REQ-245, REQ-246 |
| SCR-NOTICE-MGMT | `/admin/notices` | listNotices `/api/admin/notices`; createNotice `/api/admin/notices`; saveNotice `/api/admin/notices/{noticeId}`; downloadNoticeAttachment `/api/admin/notices/{noticeId}/attachments/{attachmentId}/download` | notices, notice_targets, notice_attachments | REQ-263, REQ-264, REQ-265, REQ-266, REQ-267, REQ-268, REQ-269, REQ-270, REQ-271, REQ-272 |
| SCR-HELP-MGMT | `/admin/help-contents` | listHelpContents `/api/admin/help-contents`; saveHelpContent `/api/admin/help-contents/{screenId}`; getHelpContent `/api/help-contents/{screenId}` | help_contents | REQ-303, REQ-304, REQ-305, REQ-306, REQ-307, REQ-308, REQ-309, REQ-310, REQ-311, REQ-312 |
| SCR-MANUAL-MGMT | `/admin/manuals` | listManuals `/api/admin/manuals`; createManual `/api/admin/manuals`; downloadManualFile `/api/admin/manuals/{manualId}/download` | manuals, manual_files | REQ-329, REQ-330, REQ-331, REQ-332, REQ-333, REQ-334, REQ-335, REQ-336, REQ-337, REQ-338 |


## BASIC-23 변경 추가 데이터 계약

### BASIC-23 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| batch_definitions | 배치 정의 | 배치ID·업무유형·실행주기·최대실행시간·담당자 관리 | REQ-413 |
| batch_dependencies | 배치 선후행 관계 | 배치ID 기준 선행·후행 관계 관리 | REQ-416 |
| batch_parameters | 배치 실행 파라미터 | 배치 정의별 실행조건/파라미터 관리 | REQ-418 |
| batch_executions | 배치 실행 | 수동실행·중지·재실행 상태와 사유 기록 | REQ-433 |
| batch_execution_results | 배치 실행 결과 | 실행ID별 건수·시간·상태 결과 보존 | REQ-450 |
| batch_execution_logs | 배치 실행 로그 | 실행ID 연결 로그파일 조회 전용 보존 | REQ-457 |
| batch_retry_targets | 배치 재처리 대상 | 실패 배치 또는 실패 건 선택 후보 관리 | REQ-469 |
| batch_retry_results | 배치 재처리 결과 | 원실행ID 연결 재처리 실행 결과 별도 보존 | REQ-475 |

### BASIC-23 Entity/Table Contract 추가

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| batch_definitions | batch_id | varchar(100) | PK, 배치 식별자 | - | batch_dependencies.predecessor_batch_id / successor_batch_id | 변경 전후 값, 처리자, 처리일시, 요청 식별자 유지 | REQ-413 |
| batch_definitions | batch_type | varchar(50) | 업무유형 | - | - | 변경 메타정보 유지 | REQ-414 |
| batch_definitions | schedule_cycle | varchar(100) | 실행주기 | - | - | 변경 메타정보 유지 | REQ-415 |
| batch_definitions | max_execution_seconds | integer | 최대실행시간 정의값 | - | - | 초과 시 자동 후행 실행 정책은 OQ로 보존 | REQ-419 |
| batch_definitions | owner_user_id | bigint | 담당자 | - | users.user_id | 담당자 변경 메타정보 유지 | REQ-420 |
| batch_dependencies | predecessor_batch_id | varchar(100) | 선행 배치ID | - | batch_definitions.batch_id | 관계 변경 메타정보 유지 | REQ-416 |
| batch_dependencies | successor_batch_id | varchar(100) | 후행 배치ID | - | batch_definitions.batch_id | 관계 변경 메타정보 유지 | REQ-416 |
| batch_parameters | batch_id | varchar(100) | 배치 정의 참조 | - | batch_definitions.batch_id | 다른 배치 정의 파라미터를 임의 변경하지 않음 | REQ-418 |
| batch_parameters | parameter_json | jsonb | 실행 파라미터 | - | - | 변경 메타정보 유지 | REQ-418 |
| batch_executions | execution_id | varchar(100) | PK, 실행 식별자 | - | batch_execution_results.execution_id | 원실행/재실행 연결 보존 | REQ-433 |
| batch_executions | batch_id | varchar(100) | 실행 대상 배치 | - | batch_definitions.batch_id | 배치 정의를 변경하지 않음 | REQ-427 |
| batch_executions | process_type | varchar(20) | 처리유형 | MANUAL_RUN,STOP,RERUN | - | 운영자·사유와 함께 기록 | REQ-436 |
| batch_executions | reason | varchar(500) | 처리사유 필수 | - | - | 사유 누락 시 저장 차단 | REQ-437 |
| batch_executions | operator_user_id | bigint | 운영자 | - | users.user_id | 권한 있는 운영자만 변경 가능 | REQ-438 |
| batch_executions | execution_status | varchar(20) | 실행상태 | WAITING,RUNNING,STOPPED,COMPLETED,FAILED | - | 상태 전이 이력 보존 | REQ-439 |
| batch_executions | original_execution_id | varchar(100) | 재실행 원실행 참조 | - | batch_executions.execution_id | 원실행 식별 가능하게 연결 | REQ-441 |
| batch_execution_results | execution_id | varchar(100) | PK/FK | - | batch_executions.execution_id | 조회 전용, 결과 변경 금지 | REQ-450 |
| batch_execution_results | started_at | timestamp | 시작시간 | - | - | 원본 보존 | REQ-451 |
| batch_execution_results | ended_at | timestamp | 종료시간 | - | - | 원본 보존 | REQ-452 |
| batch_execution_results | total_count | integer | 처리건수 | - | - | 원본 보존 | REQ-453 |
| batch_execution_results | success_count | integer | 성공건수 | - | - | 원본 보존 | REQ-454 |
| batch_execution_results | failure_count | integer | 실패건수 | - | - | 원본 보존 | REQ-455 |
| batch_execution_results | excluded_count | integer | 제외건수 | - | - | 원본 보존 | REQ-456 |
| batch_execution_results | elapsed_millis | bigint | 소요시간 | - | - | 원본 보존 | REQ-456 |
| batch_execution_logs | execution_id | varchar(100) | 실행 로그 참조 | - | batch_executions.execution_id | 수정/삭제 금지, 조회 전용 | REQ-457 |
| batch_execution_logs | log_file_ref | varchar(500) | 로그파일 위치 또는 참조 | - | - | 원본 보존 | REQ-457 |
| batch_retry_targets | original_execution_id | varchar(100) | 실패 원실행 | - | batch_executions.execution_id | 실패 대상만 선택 가능 | REQ-469 |
| batch_retry_targets | failed_item_key | varchar(200) | 개별 실패 건 식별자 nullable | - | - | 실패 건 단위 재처리 후보 | REQ-470 |
| batch_retry_results | retry_execution_id | varchar(100) | PK, 재처리 실행ID | - | batch_executions.execution_id | 원실행 결과 덮어쓰기 금지 | REQ-475 |
| batch_retry_results | original_execution_id | varchar(100) | 원실행ID | - | batch_executions.execution_id | 원실행과 연결 보존 | REQ-473 |
| batch_retry_results | retry_reason | varchar(500) | 재처리 사유 필수 | - | - | 사유 누락 시 저장 차단 | REQ-480 |

### BASIC-23 Validation Rule Matrix 추가

| rule | entity/table | field | operationId/path | expected error behavior | canonical_id |
|---|---|---|---|---|---|
| batch_id, schedule_cycle, owner_user_id는 배치 정의 저장 필수값이다. | batch_definitions | batch_id, schedule_cycle, owner_user_id | saveBatchDefinition | 누락 시 400 ApiError.fields 반환, 저장 차단 | REQ-410 |
| 수동실행·중지·재실행은 권한 있는 운영자만 가능하다. | batch_executions | operator_user_id | createBatchExecution / updateBatchExecutionStatus / createBatchRerun | 권한 없음 시 403, 상태 변경 없음 | REQ-438 |
| 실행·중지·재실행은 처리 사유를 기록해야 한다. | batch_executions | reason | createBatchExecution / updateBatchExecutionStatus / createBatchRerun | 사유 누락 시 400 ApiError.fields.reason | REQ-437 |
| 재처리는 실패 대상만 선택해야 한다. | batch_retry_targets | original_execution_id, failed_item_key | createBatchRetry | 실패 상태가 아니면 409 ApiError, 원실행 결과 유지 | REQ-477 |
| 재처리 사유가 없으면 재실행할 수 없다. | batch_retry_results | retry_reason | createBatchRetry | 400 ApiError.fields.retryReason, retry 결과 미생성 | REQ-480 |

### BASIC-23 Seed Data Contract 추가

| seed ID | entity/table | minimal fields | purpose | canonical_id |
|---|---|---|---|---|
| SEED-BATCH-DEF-001 | batch_definitions | batch_id, batch_type, schedule_cycle, owner_user_id | 정의 목록/저장 smoke | REQ-409 |
| SEED-BATCH-EXEC-001 | batch_executions | execution_id, batch_id, execution_status=RUNNING, reason | 중지/재실행 smoke | REQ-428 |
| SEED-BATCH-RESULT-001 | batch_execution_results | execution_id, started_at, ended_at, total_count, success_count, failure_count, excluded_count | 결과 조회 smoke | REQ-449 |
| SEED-BATCH-LOG-001 | batch_execution_logs | execution_id, log_file_ref | 로그 조회 smoke | REQ-457 |
| SEED-BATCH-FAIL-001 | batch_retry_targets | original_execution_id, failed_item_key | 실패 대상 재처리 smoke | REQ-466 |

### BASIC-23 Screen/API Reference 추가

| screen_id | route | operationId/path | primary entity/table | canonical_id |
|---|---|---|---|---|
| SCR-BATCH-DEFINITION-MGMT | /admin/batch-definitions | listBatchDefinitions / saveBatchDefinition | batch_definitions | REQ-409 |
| SCR-BATCH-EXECUTION-MGMT | /admin/batch-executions | listBatchExecutions / createBatchExecution / updateBatchExecutionStatus / createBatchRerun | batch_executions | REQ-426 |
| SCR-BATCH-RESULT-MGMT | /admin/batch-results | listBatchResults / getBatchResultLog | batch_execution_results, batch_execution_logs | REQ-449 |
| SCR-BATCH-RETRY-MGMT | /admin/batch-retries | listBatchRetryTargets / createBatchRetry | batch_retry_targets, batch_retry_results | REQ-466 |


### BASIC-26 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| excel_upload_templates | 업로드 양식 | 업무별 Excel 양식 버전·시행일 관리 | REQ-595 |
| excel_upload_template_rules | 업로드 양식 검증규칙 | 필수 열·열 순서·코드값 규칙 연결 | REQ-596 |
| excel_upload_template_files | 업로드 양식 파일 | 버전별 다운로드 파일 token 관리 | REQ-597 |
| excel_upload_files | 엑셀 업로드 파일 | 업로드 파일과 원본 파일명 보존 | REQ-601 |
| excel_upload_staging_rows | 엑셀 업로드 원본행 | 반영 전 원본행·검증상태 staging | REQ-602 |
| excel_upload_errors | 엑셀 업로드 오류행 | upload_id별 행/컬럼 오류 상세 보존 | REQ-611 |
| excel_upload_histories | 엑셀 업로드 이력 | 작업별 건수·처리시간 보존 | REQ-608 |
| excel_download_jobs | 엑셀 다운로드 작업 | 조회조건·데이터범위·출력유형 기반 파일 생성 | REQ-616 |

### BASIC-26 Entity/Table Contract 추가

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| excel_upload_templates | template_id | varchar(100) | PK | - | excel_upload_template_rules.template_id | 사용 종료 버전 물리삭제 금지 | REQ-598 |
| excel_upload_templates | business_type | varchar(50) | 업무구분, version/effective_date와 unique | - | - | 변경 메타정보 유지 | REQ-595 |
| excel_upload_templates | template_version | varchar(50) | 양식 버전 | - | - | 이전 버전 조회 가능 | REQ-598 |
| excel_upload_templates | effective_date | date | 시행일 | - | - | 시행일별 보존 | REQ-598 |
| excel_upload_template_rules | template_id | varchar(100) | FK | - | excel_upload_templates.template_id | 양식과 검증규칙 연결 보존 | REQ-600 |
| excel_upload_template_rules | required_column | varchar(200) | 필수 열 | - | - | 저장 후 재조회 가능 | REQ-596 |
| excel_upload_template_rules | column_order | integer | 열 순서 | - | - | 저장 후 재조회 가능 | REQ-596 |
| excel_upload_template_rules | code_rule_ref | varchar(200) | 기존 코드기준 참조 | - | detail_codes | 코드기준 원장 변경 금지 | REQ-599 |
| excel_upload_template_files | file_token | varchar(200) | 다운로드 token | - | excel_upload_templates.template_id | storage path/stored filename 외부 노출 금지 | REQ-597 |
| excel_upload_template_files | original_file_name | varchar(255) | 원본 파일명 | - | - | 원본 파일명 별도 보존 | REQ-585 |
| excel_upload_files | upload_id | varchar(100) | PK | - | excel_upload_histories.upload_id | 물리삭제 금지, file_token만 노출 | REQ-601 |
| excel_upload_files | original_file_name | varchar(255) | 원본 파일명 | - | - | 저장 경로/실제 파일명 미노출 | REQ-585 |
| excel_upload_staging_rows | upload_id | varchar(100) | FK | - | excel_upload_files.upload_id | 반영 전 staging, 완료 후 임시데이터 삭제 대상 | REQ-588 |
| excel_upload_staging_rows | validation_status | varchar(20) | NORMAL,ERROR,EXCLUDED | NORMAL,ERROR,EXCLUDED | excel_upload_errors | 오류 존재 시 반영 차단 | REQ-602 |
| excel_upload_errors | error_id | varchar(100) | PK | - | excel_upload_files.upload_id | 삭제 필요 시 삭제 표시 | REQ-611 |
| excel_upload_errors | row_number | integer | upload_id+row_number+column_name unique | - | - | 오류 위치 보존 | REQ-612 |
| excel_upload_errors | column_name | varchar(200) | 오류 컬럼명 | - | - | 오류 위치 보존 | REQ-612 |
| excel_upload_errors | input_value | text | 입력값 | - | - | 개인정보 권한 적용 | REQ-612 |
| excel_upload_errors | error_code | varchar(50) | 오류코드 | - | - | 수정안내와 함께 보존 | REQ-612 |
| excel_upload_errors | error_reason | varchar(500) | 오류사유 | - | - | 오류목록 다운로드 포함 | REQ-613 |
| excel_upload_errors | correction_guide | varchar(500) | 수정안내 | - | - | 화면 표시 | REQ-612 |
| excel_upload_histories | upload_id | varchar(100) | PK/FK | - | excel_upload_files.upload_id | 물리삭제 금지 | REQ-610 |
| excel_upload_histories | total_count | integer | 원본행수 | - | - | 작업 결과 보존 | REQ-609 |
| excel_upload_histories | success_count | integer | 정상건수 | - | - | 작업 결과 보존 | REQ-584 |
| excel_upload_histories | error_count | integer | 오류건수 | - | - | 작업 결과 보존 | REQ-584 |
| excel_upload_histories | excluded_count | integer | 제외건수 | - | - | 작업 결과 보존 | REQ-605 |
| excel_upload_histories | saved_count | integer | 저장건수 | - | - | 작업 결과 보존 | REQ-605 |
| excel_download_jobs | download_id | varchar(100) | PK | - | - | 원천 업무자료 변경 금지 | REQ-616 |
| excel_download_jobs | output_type | varchar(30) | TARGET,STATUS,ERROR | TARGET,STATUS,ERROR | - | 권한 범위 결과만 보존 | REQ-617 |
| excel_download_jobs | file_token | varchar(200) | 결과파일 token | - | - | storage path/stored filename 외부 노출 금지 | REQ-585 |

### BASIC-26 Validation Rule Matrix 추가

| rule | entity/table | field | operationId/path | expected error behavior | canonical_id |
|---|---|---|---|---|---|
| 필수 열·열 순서·코드값 규칙·시행일 누락 시 양식 저장을 차단한다. | excel_upload_template_rules | required_column, column_order, code_rule_ref, effective_date | saveUploadTemplate | 400 ApiError.fields, 저장 없음 | REQ-596 |
| 헤더·필수값·형식·코드·중복 검증 오류는 ERROR로 기록한다. | excel_upload_staging_rows, excel_upload_errors | validation_status, error_code | createExcelUpload | 200 검증결과 + 오류행 분리 | REQ-602 |
| 오류 행이 1건 이상이면 전체 반영을 차단한다. | excel_upload_staging_rows | validation_status | commitExcelUpload | 409 ApiError, 업무자료 변경 없음 | REQ-604 |
| 다운로드 파일 요청은 권한을 재검증한다. | excel_upload_template_files, excel_download_jobs | file_token | downloadUploadTemplate / createExcelDownload | 403 ApiError, 파일 미제공 | REQ-586 |

### BASIC-26 Seed Data Contract 추가

| seed ID | entity/table | minimal fields | purpose | canonical_id |
|---|---|---|---|---|
| SEED-EXCEL-TEMPLATE-001 | excel_upload_templates | template_id, business_type, template_version, effective_date | 양식 조회/다운로드 smoke | REQ-595 |
| SEED-EXCEL-UPLOAD-VALID | excel_upload_files, excel_upload_staging_rows | upload_id, validation_status=NORMAL | 정상 반영 smoke | REQ-603 |
| SEED-EXCEL-UPLOAD-ERROR | excel_upload_files, excel_upload_errors | upload_id, row_number, error_reason | 오류 반영 차단 smoke | REQ-604 |
| SEED-EXCEL-HISTORY-001 | excel_upload_histories | upload_id, total_count, success_count, error_count, excluded_count, saved_count | 이력 조회 smoke | REQ-608 |
| SEED-EXCEL-ERROR-001 | excel_upload_errors | upload_id, row_number, column_name, error_code, error_reason, correction_guide | 오류 조회/다운로드 smoke | REQ-611 |

### BASIC-26 Screen/API Reference 추가

| screen_id | route | operationId/path | primary entity/table | canonical_id |
|---|---|---|---|---|
| SCR-UPLOAD-TEMPLATE-MGMT | /admin/excel-upload-templates | listUploadTemplates / saveUploadTemplate / downloadUploadTemplate | excel_upload_templates | REQ-595 |
| SCR-EXCEL-UPLOAD-MGMT | /admin/excel-uploads | createExcelUpload / commitExcelUpload | excel_upload_files | REQ-601 |
| SCR-UPLOAD-HISTORY-MGMT | /admin/excel-upload-histories | listExcelUploadHistories | excel_upload_histories | REQ-608 |
| SCR-UPLOAD-ERROR-MGMT | /admin/excel-upload-errors | listExcelUploadErrors / downloadExcelUploadErrors | excel_upload_errors | REQ-612 |
| SCR-EXCEL-DOWNLOAD-MGMT | /admin/excel-downloads | createExcelDownload | excel_download_jobs | REQ-617 |


## BASIC-29 접속·감사 운영기능 데이터 계약 추가

### BASIC-29 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| session_termination_history | 세션 종료 이력 | 로그아웃·유휴만료·절대만료·관리자 강제종료 원인 보존 | REQ-690 |
| business_process_audit_logs | 업무처리 감사로그 | 등록·수정·삭제·확인·인증·승인·취소·일괄처리 전후상태 보존 | REQ-696 |
| sensitive_information_access_logs | 중요정보 조회로그 | 개인정보·평가결과·계좌정보 조회 대상범위와 목적 보존 | REQ-703 |

### BASIC-29 Entity/Table Contract 추가

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| sessions | session_id | varchar(128) | 기존 PK, 강제종료 대상 | - | session_termination_history.session_id | ACTIVE -> TERMINATED 상태전이 기록 | REQ-681 |
| sessions | status | varchar(20) | 세션 상태 확장 | ACTIVE,EXPIRED,LOGGED_OUT,TERMINATED | - | 강제종료 후 활성 목록 제외 | REQ-684 |
| sessions | terminated_by | bigint | nullable, 강제종료 처리자 | - | users.user_id | 처리자 보존 | REQ-683 |
| sessions | terminated_at | timestamp | nullable, 강제종료 일시 | - | - | 처리일시 보존 | REQ-683 |
| sessions | termination_reason | varchar(500) | nullable, 강제종료 사유 | - | - | 사유 보존 | REQ-681 |
| session_termination_history | history_id | bigint | PK | - | - | 수정·삭제 금지, 장기 보존/아카이브 대상 | REQ-693 |
| session_termination_history | session_id | varchar(128) | 필수 | - | sessions.session_id | 해당 세션과 연결 | REQ-690 |
| session_termination_history | termination_type | varchar(30) | 필수 | LOGOUT,IDLE_TIMEOUT,ABSOLUTE_TIMEOUT,ADMIN_TERMINATED | - | 종료유형 보존 | REQ-688 |
| session_termination_history | termination_reason | varchar(500) | nullable | - | - | 종료사유 보존 | REQ-689 |
| session_termination_history | terminated_at | timestamp | 필수 | - | - | 종료일시 보존 | REQ-689 |
| business_process_audit_logs | audit_log_id | bigint | PK | - | - | 변경 불가 이력, 물리삭제 금지 | REQ-696 |
| business_process_audit_logs | action_type | varchar(50) | 필수 | CREATE,UPDATE,DELETE,CONFIRM,AUTH,APPROVE,CANCEL,BATCH,SESSION_TERMINATE | - | 행위유형별 조회 기준 | REQ-694 |
| business_process_audit_logs | target_key | varchar(200) | 필수 | - | source business key | 대상키 보존 | REQ-695 |
| business_process_audit_logs | before_state | jsonb | nullable, 보호값 마스킹 | - | - | 보호대상 원문값 최소화 | REQ-695 |
| business_process_audit_logs | after_state | jsonb | nullable, 보호값 마스킹 | - | - | 전후상태 보존 | REQ-695 |
| business_process_audit_logs | result_status | varchar(20) | 필수 | SUCCESS,FAILURE | - | 성공/실패 구분 | REQ-697 |
| business_process_audit_logs | request_id | varchar(100) | 필수 | - | request context | 전 구간 추적 | REQ-661 |
| sensitive_information_access_logs | access_log_id | bigint | PK | - | - | 수정·삭제 금지, 장기 보존/아카이브 대상 | REQ-706 |
| sensitive_information_access_logs | information_type | varchar(50) | 필수 | PERSONAL_EVALUATION_RESULT,SCORE_CALCULATION,PERSONAL_INFORMATION,ACCOUNT_INFORMATION | - | 정보유형별 조회 기준 | REQ-701 |
| sensitive_information_access_logs | viewer_user_id | bigint | 필수 | - | users.user_id | 조회자 보존 | REQ-702 |
| sensitive_information_access_logs | target_scope | varchar(1000) | 필수 | - | - | 대상범위 보존, 원문값 저장 금지 | REQ-703 |
| sensitive_information_access_logs | access_purpose | varchar(500) | 필수 | - | - | 사용자 입력 또는 시스템 맥락 목적 보존 | REQ-703 |
| permission_change_history | approver_user_id | bigint | nullable | - | users.user_id | 승인자 보강 | REQ-709 |

### BASIC-29 Validation Rule Matrix 추가

| Rule | Entity/Table | Field(s) | Rejection / Observable Result | canonical_id |
|---|---|---|---|---|
| 강제종료 reason 필수 | sessions | termination_reason | 누락 시 400 ApiError.fields.reason, ACTIVE 상태 유지 | REQ-681 |
| 강제종료 권한 제한 | sessions | terminated_by | R09 외 요청은 403, 대상 세션 상태 불변 | REQ-682 |
| 종료·만료 세션 활성목록 제외 | sessions | status | EXPIRED/LOGGED_OUT/TERMINATED는 listActiveSessions 결과에서 제외 | REQ-684 |
| 감사로그 불변 | business_process_audit_logs, sensitive_information_access_logs, session_termination_history, permission_change_history | all fields | update/delete API와 mapper 생성 금지 | REQ-692, REQ-706, REQ-712 |
| 중요정보 원문값 비저장 | sensitive_information_access_logs | target_scope, before_state/after_state references | 보호대상 원문값은 저장하지 않고 범위·목적만 보존 | REQ-704 |

### BASIC-29 Seed Data Contract 추가

| seed ID | entity/table | minimal fields | purpose | canonical_id |
|---|---|---|---|---|
| SEED-SESSION-ACTIVE-001 | sessions | session_id, user_id, login_at, last_accessed_at, ip_address, status=ACTIVE | 접속현황 조회/강제종료 smoke | REQ-679 |
| SEED-SESSION-HISTORY-001 | session_termination_history | session_id, termination_type, terminated_at, termination_reason | 로그아웃·만료 이력 조회 smoke | REQ-687 |
| SEED-BUSINESS-AUDIT-001 | business_process_audit_logs | action_type, target_key, before_state, after_state, actor_user_id, result_status | 업무처리 로그 조회 smoke | REQ-694 |
| SEED-SENSITIVE-ACCESS-001 | sensitive_information_access_logs | information_type, viewer_user_id, target_scope, access_purpose, access_result | 중요정보 조회 로그 smoke | REQ-701 |
| SEED-PERMISSION-CHANGE-001 | permission_change_history | target_type, target_id, before_value, after_value, approver_user_id, changed_by, reason | 권한변경 로그 조회 smoke | REQ-707 |

### BASIC-29 Screen/API Reference 추가

| screen_id | route | operationId/path | primary entity/table | canonical_id |
|---|---|---|---|---|
| SCR-ACTIVE-SESSION-STATUS | /admin/security/active-sessions | listActiveSessions / terminateActiveSession | sessions | REQ-679, REQ-680, REQ-681, REQ-682, REQ-683, REQ-684 |
| SCR-SESSION-TERMINATION-HISTORY | /admin/security/session-termination-histories | listSessionTerminationHistories | session_termination_history | REQ-687, REQ-688, REQ-689, REQ-690, REQ-692, REQ-693 |
| SCR-BUSINESS-PROCESS-LOG | /admin/audit/business-process-logs | listBusinessProcessLogs | business_process_audit_logs | REQ-694, REQ-695, REQ-696, REQ-697, REQ-698, REQ-699 |
| SCR-SENSITIVE-INFO-ACCESS-LOG | /admin/audit/sensitive-information-access-logs | listSensitiveInformationAccessLogs | sensitive_information_access_logs | REQ-701, REQ-702, REQ-703, REQ-704, REQ-705, REQ-706 |
| SCR-PERMISSION-CHANGE-LOG | /admin/audit/permission-change-logs | listPermissionChangeLogs | permission_change_history | REQ-707, REQ-708, REQ-709, REQ-711, REQ-712 |


## BASIC-32 공통 재사용·업무 상태 모델 데이터 계약 추가

### BASIC-32 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| evaluation_organization_mappings | 평가조직 매핑 | 기존 KORUS 조직·사용자와 업무 데이터 범위 연결 | REQ-768 |
| business_status_codes | 업무 상태코드 | 교수업적평가·학술지원금·이의신청 업무유형별 상태코드와 표시명 | REQ-771 |
| business_status_transitions | 업무 상태 전이규칙 | 현재 상태별 다음 상태·실행 역할·필수조건·취소 가능 여부 | REQ-774 |
| rejection_reasons | 반려사유 | 업무유형별 표준 반려사유 코드와 문구·추가 의견 허용 여부 | REQ-777 |
| data_change_histories | 데이터 변경 이력 | BASIC-32 업무 운영 데이터 등록·수정·삭제 전후값 필드 단위 조회 | REQ-780 |
| deleted_business_data | 삭제자료 조회 projection | 논리삭제 업무자료의 원본키·삭제자·삭제일시·삭제사유·복구가능여부 조회 | REQ-783 |

### BASIC-32 Entity/Table Contract 추가

| entity/table | field | type | key/required | enum/rule | source-of-truth | design intent | canonical_id |
|---|---|---|---|---|---|---|---|
| evaluation_organization_mappings | mapping_id | bigint | PK | generated | local | 평가조직 매핑 식별자 | REQ-768 |
| evaluation_organization_mappings | user_id | bigint | FK users.user_id | - | existing users | 기존 내부계정 참조 | REQ-768 |
| evaluation_organization_mappings | organization_code | varchar(50) | FK organizations.organization_code | - | KORUS snapshot/local org | 평가조직 데이터 범위 기준 | REQ-768 |
| evaluation_organization_mappings | business_type | varchar(50) | required | FACULTY_ACHIEVEMENT, ACADEMIC_GRANT, OBJECTION | local | 업무유형별 권한 연결 | REQ-769 |
| evaluation_organization_mappings | data_scope | varchar(30) | required | SELF, DEPARTMENT, COLLEGE, BUSINESS, ALL | existing permission model | 기존 데이터 범위 재사용 | REQ-769 |
| business_status_codes | status_code_id | bigint | PK | generated | local | 상태코드 row 식별자 | REQ-771 |
| business_status_codes | definition_version | varchar(30) | required | 작성중/확정/폐기 lifecycle | local | 상태정의 버전 | REQ-772 |
| business_status_codes | business_type | varchar(50) | required | FACULTY_ACHIEVEMENT, ACADEMIC_GRANT, OBJECTION | local | 업무유형별 분리 | REQ-771 |
| business_status_codes | status_code | varchar(50) | required | 업무유형 내 unique | local | 기술 상태코드 불변 대상 | REQ-773 |
| business_status_codes | display_name | varchar(100) | required | - | local | 사용자 표시명 | REQ-772 |
| business_status_transitions | transition_id | bigint | PK | generated | local | 전이규칙 식별자 | REQ-774 |
| business_status_transitions | from_status_code | varchar(50) | required | FK-like status_code | local | 현재 상태 | REQ-775 |
| business_status_transitions | to_status_code | varchar(50) | required | FK-like status_code | local | 다음 상태 | REQ-775 |
| business_status_transitions | executor_role_code | varchar(20) | required | R01~R09 | existing roles | 실행 역할 | REQ-775 |
| business_status_transitions | opinion_required_yn | char(1) | required | Y/N | local | 필수의견 여부 | REQ-776 |
| business_status_transitions | attachment_required_yn | char(1) | required | Y/N | local | 필수첨부 여부 | REQ-776 |
| business_status_transitions | cancellable_yn | char(1) | required | Y/N | local | 취소 가능 여부 | REQ-776 |
| rejection_reasons | rejection_reason_id | bigint | PK | generated | local | 반려사유 row 식별자 | REQ-777 |
| rejection_reasons | reason_code | varchar(50) | required | 업무유형 내 unique | local | 표준 반려사유 코드 | REQ-778 |
| rejection_reasons | standard_message | varchar(500) | required | - | local | 표준 문구 | REQ-778 |
| rejection_reasons | additional_opinion_allowed_yn | char(1) | required | Y/N | local | 추가 의견 허용 여부 | REQ-779 |
| data_change_histories | history_id | bigint | PK | generated | local | 변경이력 식별자 | REQ-780 |
| data_change_histories | target_business | varchar(100) | required | BASIC-32 업무 운영 데이터 | local | 대상 업무·기준정보 | REQ-780 |
| data_change_histories | target_key | varchar(200) | required | source business key | local | 대상 식별정보 | REQ-780 |
| data_change_histories | change_type | varchar(20) | required | CREATE, UPDATE, DELETE | local | 처리유형 | REQ-782 |
| data_change_histories | field_name | varchar(100) | required | snake_case field | local | 변경 필드 | REQ-781 |
| data_change_histories | before_value | text | nullable, masked when sensitive | local | 변경 전 값 | REQ-781 |
| data_change_histories | after_value | text | nullable, masked when sensitive | local | 변경 후 값 | REQ-781 |
| data_change_histories | changed_by | bigint | required | FK users.user_id | existing users | 처리자 | REQ-782 |
| data_change_histories | changed_at | timestamp | required | - | system clock | 변경일시 | REQ-782 |
| deleted_business_data | deleted_data_id | bigint | PK | generated/projection | local | 삭제자료 조회 식별자 | REQ-783 |
| deleted_business_data | original_key | varchar(200) | required | source business key | local | 원본키 | REQ-784 |
| deleted_business_data | deleted_by | bigint | required | FK users.user_id | existing users | 삭제자 | REQ-784 |
| deleted_business_data | deleted_at | timestamp | required | - | system clock | 삭제일시 | REQ-784 |
| deleted_business_data | delete_reason | varchar(500) | required | - | local | 삭제사유 | REQ-784 |
| deleted_business_data | recoverable_yn | char(1) | required | Y/N | local | 복구가능여부 표시 전용 | REQ-784 |

### BASIC-32 Validation Rule Matrix 추가

| rule | entity/table | operation/path | expected behavior | required test | canonical_id |
|---|---|---|---|---|---|
| 확정된 기술 상태코드는 수정·삭제·의미 재사용 금지 | business_status_codes | saveBusinessStatusCode /api/admin/business-status-codes | 409 ApiError, 기존 row 미변경 | 확정 version status_code 수정 요청 -> display_name/status_code 미변경 | REQ-773 |
| 전이 필수의견·필수첨부 조건 | business_status_transitions | saveBusinessStatusTransition /api/admin/business-status-transitions | 필수조건 누락 시 400 ApiError.fields | opinion_required_yn=Y인데 의견 없음 -> 상태 전이 저장/실행 차단 | REQ-776 |
| 삭제자료 조회 전용 | deleted_business_data | listDeletedBusinessData /api/admin/deleted-business-data | 복구·물리삭제 mutation 없음 | route/API에 DELETE/restore operation 부재 확인 | REQ-783 |

### BASIC-32 Seed Data Contract 추가

| seed ID | entity/table | minimal fields | purpose | consumed by |
|---|---|---|---|---|
| B32-SEED-001 | business_status_codes | business_type=FACULTY_ACHIEVEMENT, status_code=작성중/제출/학과장확인/학과장미승인/인증/인증반려/평가확정/삭제 | CMN-201 상태 seed | REQ-771, REQ-773 |
| B32-SEED-002 | business_status_codes | business_type=ACADEMIC_GRANT, DEC-022 상태 흐름 | 학술지원금 상태 seed | REQ-771 |
| B32-SEED-003 | business_status_codes | business_type=OBJECTION, DEC-023 상태 흐름 | 이의신청 상태 모델 참조 seed | REQ-771 |
| B32-SEED-004 | rejection_reasons | business_type별 reason_code, standard_message, additional_opinion_allowed_yn | 반려사유 조회/저장 smoke | REQ-777, REQ-778 |

### BASIC-32 Screen/API Reference 추가

| screen_id | route | operationId/path | entity/table | canonical_id |
|---|---|---|---|---|
| SCR-EVALUATION-ORG-MAPPING | /admin/evaluation-organization-mappings | listEvaluationOrganizationMappings /api/business/evaluation-organization-mappings; saveEvaluationOrganizationMapping /api/business/evaluation-organization-mappings | evaluation_organization_mappings | REQ-768, REQ-769 |
| SCR-BUSINESS-STATUS-CODE | /admin/business-status-codes | listBusinessStatusCodes /api/admin/business-status-codes; saveBusinessStatusCode /api/admin/business-status-codes | business_status_codes | REQ-771, REQ-772, REQ-773 |
| SCR-BUSINESS-STATUS-TRANSITION | /admin/business-status-transitions | listBusinessStatusTransitions /api/admin/business-status-transitions; saveBusinessStatusTransition /api/admin/business-status-transitions | business_status_transitions | REQ-774, REQ-775, REQ-776 |
| SCR-REJECTION-REASON | /admin/rejection-reasons | listRejectionReasons /api/admin/rejection-reasons; saveRejectionReason /api/admin/rejection-reasons | rejection_reasons | REQ-777, REQ-778, REQ-779 |
| SCR-DATA-CHANGE-HISTORY | /admin/data-change-histories | listDataChangeHistories /api/admin/data-change-histories | data_change_histories | REQ-780, REQ-781, REQ-782 |
| SCR-DELETED-BUSINESS-DATA | /admin/deleted-business-data | listDeletedBusinessData /api/admin/deleted-business-data | deleted_business_data | REQ-783, REQ-784 |


## BASIC-33 평가규칙 분류 체계 데이터 계약 추가

### BASIC-33 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| evaluation_rule_versions | 평가규정 버전 | 적용시작일·적용종료일과 작성중·확정·폐기 상태로 B-1 분류체계 구성요소를 묶는 기준 | REQ-844, REQ-806, REQ-846, REQ-847, REQ-848 |
| evaluation_areas | 평가영역 | 교육·연구/창작·봉사 영역 코드·명칭·정렬순서·사용여부·평가기간 적용방식 | REQ-864, REQ-865, REQ-866, REQ-867 |
| evaluation_items | 평가항목 | 평가영역별 평가항목·지표 코드와 상위항목 계층, 배점 적용방식 | REQ-870, REQ-871, REQ-872, REQ-873 |
| evaluation_elements | 평가요소 | 평가항목 아래 평가연도별 평가요소코드·코드명·정렬순서·사용여부 | REQ-876, REQ-877, REQ-878, REQ-879 |
| evaluation_management_items | 관리항목 | 평가요소별 세부 관리항목과 교원 입력가능·필수·데이터형식 조건 | REQ-882, REQ-883, REQ-884, REQ-885, REQ-886, REQ-887 |
| area_element_system_settings | 영역별 평가요소 체계 설정 | 업적영역별 평가항목·지표·평가요소 체계의 적용 대상과 사용상태 | REQ-890, REQ-891, REQ-892, REQ-893, REQ-894 |

### BASIC-33 Entity/Table Contract 추가

| entity/table | field | type | key/required | enum/rule | source-of-truth | design intent | canonical_id |
|---|---|---|---|---|---|---|---|
| evaluation_rule_versions | rule_version_id | bigint | PK | generated | local | 모든 평가규칙 분류 구성요소 버전 귀속 | REQ-844 |
| evaluation_rule_versions | effective_start_date | date | required | 적용시작일 | local | 확정 규정버전 적용기간 시작 | REQ-806 |
| evaluation_rule_versions | effective_end_date | date | required | 적용종료일 | local | 확정 규정버전 적용기간 종료 | REQ-806 |
| evaluation_rule_versions | version_status | varchar(20) | required | DRAFT, CONFIRMED, DISCARDED | local | 작성중·확정·폐기 lifecycle | REQ-806 |
| evaluation_areas | area_code | varchar(50) | unique within rule_version_id | 교육/연구창작/봉사 후보 | local | 최상위 평가영역 코드 | REQ-864 |
| evaluation_areas | area_name | varchar(100) | required | - | local | 평가영역 명칭 | REQ-865 |
| evaluation_areas | sort_order | integer | required | ascending | local | 사용 중 평가영역 제공 순서 | REQ-865 |
| evaluation_areas | active_yn | char(1) | required | Y/N | local | 사용여부·사용중지 | REQ-865 |
| evaluation_areas | period_apply_method | varchar(50) | required | OQ-DATA-B33-001 | local | 평가기간 적용방식 | REQ-866 |
| evaluation_items | item_code | varchar(50) | unique within area | - | local | 평가항목·지표 코드 | REQ-870, REQ-871 |
| evaluation_items | parent_item_code | varchar(50) | nullable FK-like | same area only | local | 상위항목 계층 | REQ-872 |
| evaluation_items | score_apply_method | varchar(50) | required | 점수값 제외 | local | 배점 적용방식 구분만 저장 | REQ-871 |
| evaluation_elements | evaluation_year | varchar(4) | required | YYYY | local | 연도별 평가요소 운영 단위 | REQ-876, REQ-878 |
| evaluation_elements | element_code | varchar(50) | unique within item/year | - | local | 평가요소코드 | REQ-876, REQ-877 |
| evaluation_management_items | management_item_code | varchar(50) | unique within element | - | local | 관리항목 식별 코드 | REQ-882, REQ-883 |
| evaluation_management_items | teacher_editable_yn | char(1) | required | Y/N | local | 교원 입력가능 여부 | REQ-884 |
| evaluation_management_items | required_yn | char(1) | required | Y/N | local | 필수여부 | REQ-884 |
| evaluation_management_items | data_type | varchar(30) | required | OQ-DATA-B33-002 | local | 업적 입력 검증 데이터형식 | REQ-884 |
| area_element_system_settings | target_scope | varchar(100) | required | 소속·영역·평가대상 | local/existing org | 적용 대상 | REQ-892 |
| area_element_system_settings | active_yn | char(1) | required | Y/N | local | 사용상태 | REQ-892 |

### BASIC-33 Validation Rule Matrix 추가

| rule | entity/table | operation/path | expected behavior | required test | canonical_id |
|---|---|---|---|---|---|
| 작성중 규정버전에서만 분류체계 저장 | evaluation_areas/evaluation_items/evaluation_elements/evaluation_management_items/area_element_system_settings | save* /api/admin/*/save | 확정/폐기 version 저장 요청은 409 ApiError, 기존 row 미변경 | 확정 rule_version_id로 저장 요청 -> 각 table 기존 값 유지 | REQ-867, REQ-873, REQ-879, REQ-885, REQ-894 |
| 사용 중 코드 물리삭제 금지 | evaluation_areas/evaluation_items/evaluation_elements/evaluation_management_items | save* /api/admin/*/save | 삭제 대신 active_yn=N 저장만 허용 | 참조된 코드 삭제 요청 -> 409 또는 사용중지 처리, 참조 row 유지 | REQ-803 |
| 관리항목 입력조건 제공 | evaluation_management_items | listEvaluationManagementItems /api/admin/evaluation-management-items | teacher_editable_yn/required_yn/data_type이 API 응답에 포함 | 미사용 active_yn=N 항목은 신규 입력 후보 응답에서 제외 | REQ-884, REQ-887 |

### BASIC-33 Seed Data Contract 추가

| seed ID | entity/table | minimal fields | purpose | consumed by |
|---|---|---|---|---|
| B33-SEED-001 | evaluation_rule_versions | version_status=DRAFT, effective_start_date/effective_end_date | 저장 가능 규정버전 fixture | REQ-865, REQ-871, REQ-877, REQ-883, REQ-891 |
| B33-SEED-002 | evaluation_rule_versions | version_status=CONFIRMED | 확정 후 수정 차단 fixture | REQ-867, REQ-873, REQ-879, REQ-885, REQ-894 |
| B33-SEED-003 | evaluation_areas | area_code=EDUCATION/RESEARCH_CREATION/SERVICE, active_yn=Y | 최상위 평가영역 기본 구분 | REQ-864, REQ-865 |

### BASIC-33 Screen/API Reference 추가

| screen_id | route | operationId/path | entity/table | canonical_id |
|---|---|---|---|---|
| SCR-EVALUATION-AREA-MGMT | /admin/evaluation-areas | listEvaluationAreas /api/admin/evaluation-areas; saveEvaluationArea /api/admin/evaluation-areas/save | evaluation_areas | REQ-864, REQ-865, REQ-866, REQ-867 |
| SCR-EVALUATION-ITEM-MGMT | /admin/evaluation-items | listEvaluationItems /api/admin/evaluation-items; saveEvaluationItem /api/admin/evaluation-items/save | evaluation_items | REQ-870, REQ-871, REQ-872, REQ-873 |
| SCR-EVALUATION-ELEMENT-MGMT | /admin/evaluation-elements | listEvaluationElements /api/admin/evaluation-elements; saveEvaluationElement /api/admin/evaluation-elements/save | evaluation_elements | REQ-876, REQ-877, REQ-878, REQ-879 |
| SCR-EVALUATION-MANAGEMENT-ITEM-MGMT | /admin/evaluation-management-items | listEvaluationManagementItems /api/admin/evaluation-management-items; saveEvaluationManagementItem /api/admin/evaluation-management-items/save | evaluation_management_items | REQ-882, REQ-883, REQ-884, REQ-885, REQ-886, REQ-887 |
| SCR-AREA-ELEMENT-SYSTEM-MGMT | /admin/area-element-systems | listAreaElementSystems /api/admin/area-element-systems; saveAreaElementSystem /api/admin/area-element-systems/save | area_element_system_settings | REQ-890, REQ-891, REQ-892, REQ-893, REQ-894 |


## BASIC-35 업무기간·기간통제 데이터 계약 추가
### BASIC-35 Entity Registry 추가
| entity/table | display label | purpose | related canonical_id |
|---|---|---|---|
| evaluation_date_settings | 평가일자 관리 | 업무기간·기간통제 설정 저장 | REQ-1116, REQ-1117, REQ-1118 |
| input_period_settings | 입력기간 관리 | 업무기간·기간통제 설정 저장 | REQ-1122, REQ-1123, REQ-1124 |
| modification_period_settings | 수정기간 관리 | 업무기간·기간통제 설정 저장 | REQ-1128, REQ-1129, REQ-1130 |
| department_chair_confirm_period_settings | 학과장 확인기간 관리 | 업무기간·기간통제 설정 저장 | REQ-1135, REQ-1136, REQ-1137 |
| business_period_integrated_settings | 평가·업적입력 기간 관리 | 업무기간·기간통제 설정 저장 | REQ-1142, REQ-1143, REQ-1144 |

### BASIC-35 Entity/Table Contract 추가
| entity/table | field | type | key/constraint | source-of-truth | lifecycle/status owner | related canonical_id |
|---|---|---|---|---|---|---|
| evaluation_date_settings | setting_id | bigint | PK | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | evaluation_year | varchar(4) | required | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | area_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | organization_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | user_type_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | start_at | timestamp/date | required | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | end_at | timestamp/date | required | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | base_date | date | optional | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | active_yn | varchar(1) | Y/N | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | created_at | timestamp | audit | local | 업무기간 설정 | REQ-1116 |
| evaluation_date_settings | updated_at | timestamp | audit | local | 업무기간 설정 | REQ-1116 |
| input_period_settings | setting_id | bigint | PK | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | evaluation_year | varchar(4) | required | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | area_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | organization_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | user_type_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | start_at | timestamp/date | required | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | end_at | timestamp/date | required | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | base_date | date | optional | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | active_yn | varchar(1) | Y/N | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | created_at | timestamp | audit | local | 업무기간 설정 | REQ-1122 |
| input_period_settings | updated_at | timestamp | audit | local | 업무기간 설정 | REQ-1122 |
| modification_period_settings | setting_id | bigint | PK | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | evaluation_year | varchar(4) | required | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | area_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | organization_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | user_type_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | start_at | timestamp/date | required | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | end_at | timestamp/date | required | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | base_date | date | optional | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | active_yn | varchar(1) | Y/N | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | created_at | timestamp | audit | local | 업무기간 설정 | REQ-1128 |
| modification_period_settings | updated_at | timestamp | audit | local | 업무기간 설정 | REQ-1128 |
| department_chair_confirm_period_settings | setting_id | bigint | PK | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | evaluation_year | varchar(4) | required | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | area_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | organization_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | user_type_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | start_at | timestamp/date | required | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | end_at | timestamp/date | required | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | base_date | date | optional | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | active_yn | varchar(1) | Y/N | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | created_at | timestamp | audit | local | 업무기간 설정 | REQ-1135 |
| department_chair_confirm_period_settings | updated_at | timestamp | audit | local | 업무기간 설정 | REQ-1135 |
| business_period_integrated_settings | setting_id | bigint | PK | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | evaluation_year | varchar(4) | required | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | area_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | organization_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | user_type_code | varchar(50) | optional | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | start_at | timestamp/date | required | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | end_at | timestamp/date | required | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | base_date | date | optional | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | active_yn | varchar(1) | Y/N | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | created_at | timestamp | audit | local | 업무기간 설정 | REQ-1142 |
| business_period_integrated_settings | updated_at | timestamp | audit | local | 업무기간 설정 | REQ-1142 |

### BASIC-35 Validation Rule Matrix 추가
| rule | entity/table | fields/operation | expected error behavior | required test | canonical_id |
|---|---|---|---|---|---|
| 활성 기간 중복 차단 | evaluation_date_settings | evaluation_year/area_code/organization_code/user_type_code/start_at/end_at / saveEvaluationDate | 동일 적용 조건의 겹치는 활성 기간 저장 시 409 ApiError, 기존 row 미변경 | 중복 기간 저장 요청 -> 기존 evaluation_date_settings row no-change | REQ-1118 |
| 활성 기간 중복 차단 | input_period_settings | evaluation_year/area_code/organization_code/user_type_code/start_at/end_at / saveInputPeriod | 동일 적용 조건의 겹치는 활성 기간 저장 시 409 ApiError, 기존 row 미변경 | 중복 기간 저장 요청 -> 기존 input_period_settings row no-change | REQ-1124 |
| 활성 기간 중복 차단 | modification_period_settings | evaluation_year/area_code/organization_code/user_type_code/start_at/end_at / saveModificationPeriod | 동일 적용 조건의 겹치는 활성 기간 저장 시 409 ApiError, 기존 row 미변경 | 중복 기간 저장 요청 -> 기존 modification_period_settings row no-change | REQ-1130 |
| 활성 기간 중복 차단 | department_chair_confirm_period_settings | evaluation_year/area_code/organization_code/user_type_code/start_at/end_at / saveDepartmentChairConfirmPeriod | 동일 적용 조건의 겹치는 활성 기간 저장 시 409 ApiError, 기존 row 미변경 | 중복 기간 저장 요청 -> 기존 department_chair_confirm_period_settings row no-change | REQ-1137 |
| 활성 기간 중복 차단 | business_period_integrated_settings | evaluation_year/area_code/organization_code/user_type_code/start_at/end_at / saveBusinessPeriod | 동일 적용 조건의 겹치는 활성 기간 저장 시 409 ApiError, 기존 row 미변경 | 중복 기간 저장 요청 -> 기존 business_period_integrated_settings row no-change | REQ-1144 |

### BASIC-35 Seed Data Contract 추가
| seed ID | entity/table | minimal fields | purpose | consumed by | canonical_id |
|---|---|---|---|---|---|
| B35-SEED-001 | evaluation_date_settings | evaluation_year=2026, active_yn=Y, source-backed period fields | 평가일자 관리 smoke/contract test | listEvaluationDates, SCR-EVALUATION-DATE-MGMT | REQ-1116 |
| B35-SEED-002 | input_period_settings | evaluation_year=2026, active_yn=Y, source-backed period fields | 입력기간 관리 smoke/contract test | listInputPeriods, SCR-INPUT-PERIOD-MGMT | REQ-1122 |
| B35-SEED-003 | modification_period_settings | evaluation_year=2026, active_yn=Y, source-backed period fields | 수정기간 관리 smoke/contract test | listModificationPeriods, SCR-MODIFICATION-PERIOD-MGMT | REQ-1128 |
| B35-SEED-004 | department_chair_confirm_period_settings | evaluation_year=2026, active_yn=Y, source-backed period fields | 학과장 확인기간 관리 smoke/contract test | listDepartmentChairConfirmPeriods, SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | REQ-1135 |
| B35-SEED-005 | business_period_integrated_settings | evaluation_year=2026, active_yn=Y, source-backed period fields | 평가·업적입력 기간 관리 smoke/contract test | listBusinessPeriods, SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | REQ-1142 |

### BASIC-35 Screen/API Reference 추가
| entity/table | screen IDs | routes | OpenAPI operationIds/paths | task slices | canonical_id |
|---|---|---|---|---|---|
| evaluation_date_settings | SCR-EVALUATION-DATE-MGMT | /admin/evaluation-dates | listEvaluationDates `/api/admin/evaluation-dates`; saveEvaluationDate `/api/admin/evaluation-dates/save` | CUS-031 | REQ-1116, REQ-1117 |
| input_period_settings | SCR-INPUT-PERIOD-MGMT | /admin/input-periods | listInputPeriods `/api/admin/input-periods`; saveInputPeriod `/api/admin/input-periods/save` | CUS-032 | REQ-1122, REQ-1123 |
| modification_period_settings | SCR-MODIFICATION-PERIOD-MGMT | /admin/modification-periods | listModificationPeriods `/api/admin/modification-periods`; saveModificationPeriod `/api/admin/modification-periods/save` | CUS-033 | REQ-1128, REQ-1129 |
| department_chair_confirm_period_settings | SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT | /admin/department-chair-confirm-periods | listDepartmentChairConfirmPeriods `/api/admin/department-chair-confirm-periods`; saveDepartmentChairConfirmPeriod `/api/admin/department-chair-confirm-periods/save` | CUS-034 | REQ-1135, REQ-1136 |
| business_period_integrated_settings | SCR-BUSINESS-PERIOD-INTEGRATED-MGMT | /admin/business-periods | listBusinessPeriods `/api/admin/business-periods`; saveBusinessPeriod `/api/admin/business-periods/save` | CUS-035 | REQ-1142, REQ-1143 |


## BASIC-37 조회 오류·예시 데이터 변경 계약

### BASIC-37 Entity Registry 추가

| entity/table | label | primary purpose | canonical_id |
|---|---|---|---|
| faculty_search_results | 교원 검색 조회 projection | 연구자 프로필 관리 하위 교원 검색 목록 DTO 매핑 검증 | REQ-1149 |
| researcher_profiles | 연구자 프로필 조회 projection | 연구자 프로필 목록 DTO 매핑 검증 | REQ-1150 |
| degree_deficiency_targets | 선행학위 미충족 대상 projection | 선행학위 미충족 대상 조회 DTO 매핑 검증 | REQ-1151 |

### BASIC-37 Entity/Table Contract 추가

| Entity | Field | Type | Constraint | Enum | Relationship | Audit/Delete Policy | canonical_id |
|---|---|---|---|---|---|---|---|
| faculty_search_results | faculty_id | varchar(100) | PK/projection key | - | users.user_id 또는 KORUS snapshot 식별자 | 조회 전용, 개발 seed 외 운영 수정 금지 | REQ-1149 |
| faculty_search_results | faculty_name | varchar(200) | required | - | - | DTO alias는 response field와 일치 | REQ-1149 |
| faculty_search_results | organization_code | varchar(50) | optional FK-like | - | organizations.organization_code | 연관 seed integrity 필요 | REQ-1149 |
| researcher_profiles | researcher_profile_id | varchar(100) | PK/projection key | - | faculty_search_results.faculty_id | 조회 전용 projection | REQ-1150 |
| researcher_profiles | profile_status | varchar(30) | required when available | - | - | DTO alias mismatch 회귀 테스트 대상 | REQ-1150 |
| degree_deficiency_targets | target_id | varchar(100) | PK/projection key | - | researcher_profiles.researcher_profile_id | 조회 전용 projection | REQ-1151 |
| degree_deficiency_targets | deficiency_reason | varchar(500) | required when target row exists | - | - | 선행학위 미충족 사유 표시 | REQ-1151 |

### BASIC-37 Validation Rule Matrix 추가

| rule | entity/table | field | operationId/path | expected error behavior | canonical_id |
|---|---|---|---|---|---|
| 조회 DTO field alias는 response DTO property와 일치해야 한다. | faculty_search_results | faculty_id, faculty_name, organization_code | listFacultySearchResults | mismatch 시 contract test 실패, 운영 응답은 500 stack trace를 노출하지 않음 | REQ-1149 |
| 연구자 프로필 목록 mapper resultMap은 projection column을 누락하지 않아야 한다. | researcher_profiles | researcher_profile_id, profile_status | listResearcherProfiles | mismatch 시 contract test 실패, 응답 envelope 유지 | REQ-1150 |
| 선행학위 미충족 대상 mapper는 미충족 사유 column alias를 보존해야 한다. | degree_deficiency_targets | target_id, deficiency_reason | listDegreeDeficiencyTargets | mismatch 시 contract test 실패, 응답 envelope 유지 | REQ-1151 |

### BASIC-37 Seed Data Contract 추가

| seed ID | entity/table | minimal fields | purpose | canonical_id |
|---|---|---|---|---|
| BASIC37-SEED-FACULTY-001~005 | faculty_search_results | faculty_id, faculty_name, organization_code | 교원 검색 목록 조회 smoke에서 5건 이상 표시 | REQ-1149, REQ-1156 |
| BASIC37-SEED-RESEARCHER-001~005 | researcher_profiles | researcher_profile_id, faculty_id, profile_status | 연구자 프로필 목록 조회 smoke에서 5건 이상 표시 | REQ-1150, REQ-1156 |
| BASIC37-SEED-DEGREE-001~005 | degree_deficiency_targets | target_id, researcher_profile_id, deficiency_reason | 선행학위 미충족 대상 조회 smoke에서 5건 이상 표시 | REQ-1151, REQ-1156 |
| BASIC37-SEED-BATCH-001~005 | batch_execution_results, batch_execution_logs | execution_id, counts, log_text | 배치 결과 조회 오류 회귀 smoke | REQ-1153, REQ-1156 |
| BASIC37-SEED-EXCEL-TEMPLATE-001~005 | excel_upload_templates, excel_upload_template_rules | template_id, business_type, template_version, effective_date | 업로드 양식 관리 조회 smoke | REQ-1154, REQ-1156 |

### BASIC-37 Screen/API Reference 추가

| screen_id | route | operationId/path | primary entity/table | canonical_id |
|---|---|---|---|---|
| SCR-FACULTY-SEARCH-LIST | /admin/researcher-profiles/faculty-search | listFacultySearchResults /api/admin/researcher-profiles/faculty-search | faculty_search_results | REQ-1149 |
| SCR-RESEARCHER-PROFILE-LIST | /admin/researcher-profiles | listResearcherProfiles /api/admin/researcher-profiles | researcher_profiles | REQ-1150 |
| SCR-DEGREE-DEFICIENCY-TARGET-LIST | /admin/researcher-profiles/degree-deficiencies | listDegreeDeficiencyTargets /api/admin/researcher-profiles/degree-deficiencies | degree_deficiency_targets | REQ-1151 |
| SCR-UPLOAD-TEMPLATE-MGMT | /admin/excel-upload-templates | listUploadTemplates /api/admin/excel-upload-templates | excel_upload_templates | REQ-1154 |
