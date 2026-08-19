INSERT INTO users (login_id, password_hash, employee_no, system_use_yn, status, change_reason)
VALUES ('admin', 'sha256:8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'E0001', 'Y', 'ACTIVE', '초기 시드 관리자')
ON CONFLICT (login_id) DO UPDATE SET password_hash = EXCLUDED.password_hash, employee_no = EXCLUDED.employee_no, updated_at = CURRENT_TIMESTAMP;

SELECT setval(pg_get_serial_sequence('users', 'user_id'), COALESCE((SELECT MAX(user_id) FROM users), 1), true);

INSERT INTO roles (role_code, role_name, purpose, assignment_criteria, default_data_scope, system_use_yn, status) VALUES
('R01','교원','본인 관련 업무 수행','교원 재직자','본인','Y','ACTIVE'),
('R02','학과장','소속 학과 교원 확인','학과장 보직자','소속 학과','Y','ACTIVE'),
('R03','단과대학(원) 행정실','단과대학 또는 대학원 행정 처리','단과대학 행정 담당자','소속 대학','Y','ACTIVE'),
('R04','교수지원과','기준정보와 평가 관련 행정 관리','교수지원과 담당자','전체','Y','ACTIVE'),
('R05','산학협력단','연구비·간접비·지식재산 자료 관리','산학협력단 담당자','관련 연구 데이터','Y','ACTIVE'),
('R06','입학인재관리과','입학·취업률 자료 관리','입학인재관리과 담당자','관련 입학 데이터','Y','ACTIVE'),
('R07','실적부서','담당 실적 자료 관리','실적 담당 부서','담당 부서','Y','ACTIVE'),
('R08','점수산출 감사자','산출 과정과 근거 조회','감사 담당자','감사 범위','Y','ACTIVE'),
('R09','시스템관리자','사용자·조직·메뉴·권한·코드 관리','시스템 관리자','전체','Y','ACTIVE')
ON CONFLICT (role_code) DO UPDATE SET role_name = EXCLUDED.role_name, purpose = EXCLUDED.purpose, updated_at = CURRENT_TIMESTAMP;

INSERT INTO user_roles (user_id, role_code, assignment_type, approver_user_id, status, change_reason)
SELECT user_id, 'R09', 'MANUAL', user_id, 'ACTIVE', '초기 관리자 권한'
FROM users u
WHERE u.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id AND ur.role_code = 'R09' AND ur.status = 'ACTIVE'
  );

INSERT INTO organizations (organization_code, organization_name, organization_type, system_use_yn, status, created_by, updated_by, change_reason) VALUES
('KNUE','한국교원대학교','UNIVERSITY','Y','ACTIVE',1,1,'예시 최상위 조직'),
('KNUE-COL-EDU','교육과학대학','COLLEGE','Y','ACTIVE',1,1,'예시 단과대학'),
('KNUE-DEPT-COMP','컴퓨터교육과','DEPARTMENT','Y','ACTIVE',1,1,'예시 학과')
ON CONFLICT (organization_code) DO UPDATE SET organization_name = EXCLUDED.organization_name, updated_at = CURRENT_TIMESTAMP;

INSERT INTO korus_personnel_snapshots (employee_no, name, organization_code, rank_name, employment_status, position_name, retirement_date, status) VALUES
('E0001','시스템 관리자','KNUE','직원','ACTIVE','시스템관리자',NULL,'ACTIVE'),
('E1001','홍길동','KNUE-DEPT-COMP','교수','ACTIVE','학과장',NULL,'ACTIVE')
ON CONFLICT (employee_no) DO UPDATE SET name = EXCLUDED.name, organization_code = EXCLUDED.organization_code, updated_at = CURRENT_TIMESTAMP;

INSERT INTO users (login_id, password_hash, employee_no, system_use_yn, status, created_by, updated_by, change_reason)
VALUES ('professor1', 'sha256:disabled', 'E1001', 'Y', 'ACTIVE', 1, 1, '예시 사용자')
ON CONFLICT (login_id) DO UPDATE SET employee_no = EXCLUDED.employee_no, updated_at = CURRENT_TIMESTAMP;

INSERT INTO organization_user_mappings (organization_code, user_id, position_name, mapping_type, effective_start_date, status)
SELECT v.organization_code, u.user_id, v.position_name, v.mapping_type, CURRENT_DATE, 'ACTIVE'
FROM (VALUES
    ('admin', 'KNUE', NULL, 'ORGANIZATION'),
    ('admin', 'KNUE', '시스템관리자', 'POSITION'),
    ('professor1', 'KNUE-DEPT-COMP', NULL, 'ORGANIZATION'),
    ('professor1', 'KNUE-DEPT-COMP', '학과장', 'POSITION')
) AS v(login_id, organization_code, position_name, mapping_type)
JOIN users u ON u.login_id = v.login_id
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_user_mappings existing
    WHERE existing.user_id = u.user_id
      AND existing.organization_code = v.organization_code
      AND existing.mapping_type = v.mapping_type
      AND existing.status = 'ACTIVE'
);

INSERT INTO organization_relations (organization_code, parent_organization_code, effective_start_date, status, created_by, updated_by, change_reason)
SELECT v.organization_code, v.parent_organization_code, CURRENT_DATE, 'ACTIVE', 1, 1, '초기 조직 관계'
FROM (VALUES
    ('KNUE-COL-EDU','KNUE'),
    ('KNUE-DEPT-COMP','KNUE-COL-EDU')
) AS v(organization_code, parent_organization_code)
WHERE NOT EXISTS (
    SELECT 1 FROM organization_relations r
    WHERE r.organization_code = v.organization_code
      AND r.parent_organization_code = v.parent_organization_code
      AND r.status = 'ACTIVE'
);

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(100,NULL,'MAIN','시스템 관리',1,NULL,NULL,'settings','SYSTEM','시스템 관리 대메뉴','Y','ACTIVE',1),
(110,100,'MIDDLE','사용자·조직 관리',1,NULL,NULL,'users','SYSTEM','사용자와 조직 관리','Y','ACTIVE',1),
(120,100,'MIDDLE','역할·권한 관리',2,NULL,NULL,'shield','SYSTEM','역할과 권한 관리','Y','ACTIVE',1),
(130,100,'MIDDLE','메뉴 관리',3,NULL,NULL,'menu','SYSTEM','메뉴 구조와 정보 관리','Y','ACTIVE',1),
(140,100,'MIDDLE','공통코드 관리',4,NULL,NULL,'codesandbox','SYSTEM','공통코드 관리','Y','ACTIVE',1),
(150,100,'MIDDLE','환경설정 관리',5,NULL,NULL,'settings','SYSTEM','공통 환경설정과 기준연도 관리','Y','ACTIVE',1),
(111,110,'SCREEN','사용자 관리',1,'SCR-USER-MGMT','/admin/users','user','SYSTEM','사용자 조회와 사용여부·역할 관리','Y','ACTIVE',1),
(112,110,'SCREEN','조직 관리',2,'SCR-ORG-MGMT','/admin/organizations','building','SYSTEM','조직 조회와 관계 관리','Y','ACTIVE',1),
(121,120,'SCREEN','역할 관리',1,'SCR-ROLE-MGMT','/admin/roles','key','SYSTEM','역할 메타정보 관리','Y','ACTIVE',1),
(122,120,'SCREEN','사용자 역할 관리',2,'SCR-USER-ROLE-MGMT','/admin/user-roles','user-check','SYSTEM','사용자 역할 부여와 회수','Y','ACTIVE',1),
(123,120,'SCREEN','메뉴 권한 관리',3,'SCR-MENU-PERMISSION-MGMT','/admin/menu-permissions','lock','SYSTEM','메뉴 접근 권한 관리','Y','ACTIVE',1),
(131,130,'SCREEN','메뉴 구조 관리',1,'SCR-MENU-STRUCTURE-MGMT','/admin/menu-structure','tree','SYSTEM','메뉴 부모와 정렬 관리','Y','ACTIVE',1),
(132,130,'SCREEN','메뉴 정보 관리',2,'SCR-MENU-INFO-MGMT','/admin/menu-info','file-cog','SYSTEM','메뉴 실행 정보 관리','Y','ACTIVE',1),
(133,130,'SCREEN','메뉴 사용 관리',3,'SCR-MENU-USAGE-MGMT','/admin/menu-usage','toggle-left','SYSTEM','메뉴별 사용여부와 노출기간 관리','Y','ACTIVE',1),
(141,140,'SCREEN','코드그룹 관리',1,'SCR-CODE-GROUP-MGMT','/admin/code-groups','folder-code','SYSTEM','코드그룹 관리','Y','ACTIVE',1),
(142,140,'SCREEN','상세코드 관리',2,'SCR-DETAIL-CODE-MGMT','/admin/detail-codes','list','SYSTEM','상세코드 관리','Y','ACTIVE',1),
(143,140,'SCREEN','코드 사용 관리',3,'SCR-CODE-USAGE-MGMT','/admin/code-usage','toggle-left','SYSTEM','상세코드별 사용여부와 적용기간 관리','Y','ACTIVE',1),
(151,150,'SCREEN','공통 환경설정',1,'SCR-COMMON-SETTINGS-MGMT','/admin/common-settings','sliders-horizontal','SYSTEM','세션 유휴시간, 페이지당 조회건수, 기본 검색기간, 대량조회 기준건수, 장시간작업 안내 기준 관리','Y','ACTIVE',1),
(152,150,'SCREEN','기준연도 관리',2,'SCR-EVALUATION-YEAR-MGMT','/admin/evaluation-years','calendar','SYSTEM','현재 평가연도와 기본 조회연도, 대상연도 준비 상태 관리','Y','ACTIVE',1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, parent_menu_id = EXCLUDED.parent_menu_id, url = EXCLUDED.url, screen_id = EXCLUDED.screen_id, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1 FROM menus WHERE screen_id IS NOT NULL
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, '시스템관리자 1차 범위 메뉴 접근'
FROM menus WHERE screen_id IS NOT NULL
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_usage_settings (menu_id, system_use_yn, exposure_start_at, exposure_end_at, updated_by, change_reason)
SELECT menu_id, 'Y', TIMESTAMP '2000-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', 1, '초기 메뉴 사용 설정'
FROM menus
WHERE screen_id IS NOT NULL
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO code_groups (group_id, group_name, description, managing_department, created_by, updated_by, system_use_yn, status)
VALUES ('COMMON_STATUS','공통 상태','공통 상태 코드','시스템관리',1,1,'Y','ACTIVE'),
       ('PROC_STATUS','처리상태','처리상태 코드','시스템관리',1,1,'Y','ACTIVE')
ON CONFLICT (group_id) DO NOTHING;

INSERT INTO detail_codes (group_id, code_value, code_name, sort_order, valid_start_date, valid_end_date, created_by, updated_by, system_use_yn, status)
VALUES ('COMMON_STATUS','ACTIVE','활성',1,DATE '2000-01-01',DATE '2099-12-31',1,1,'Y','ACTIVE'),
       ('COMMON_STATUS','INACTIVE','비활성',2,DATE '2000-01-01',DATE '2099-12-31',1,1,'Y','ACTIVE'),
       ('PROC_STATUS','OPEN','진행',1,DATE '2000-01-01',DATE '2099-12-31',1,1,'Y','ACTIVE'),
       ('PROC_STATUS','ENDED','종료됨',2,DATE '2020-01-01',DATE '2026-08-19',1,1,'N','INACTIVE')
ON CONFLICT (group_id, code_value) DO NOTHING;

INSERT INTO common_system_settings (setting_key, setting_value, unit, updated_by, change_reason)
VALUES ('SESSION_IDLE_MINUTES','30','minutes',1,'초기 공통 환경설정'),
       ('PAGE_SIZE','20','rows',1,'초기 공통 환경설정'),
       ('DEFAULT_SEARCH_PERIOD','30','days',1,'초기 공통 환경설정'),
       ('BULK_QUERY_THRESHOLD','1000','rows',1,'초기 공통 환경설정'),
       ('LONG_TASK_NOTICE_THRESHOLD','60','seconds',1,'초기 공통 환경설정')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO evaluation_year_settings (id, current_evaluation_year, default_search_year, updated_by, change_reason)
VALUES (1, 2026, 2026, 1, '초기 기준연도 설정')
ON CONFLICT (id) DO NOTHING;

INSERT INTO evaluation_year_preparations (target_year, copy_requested_yn, reset_requested_yn, updated_by, change_reason)
VALUES (2027, 'N', 'N', 1, '초기 대상연도 준비 상태')
ON CONFLICT (target_year) DO NOTHING;
