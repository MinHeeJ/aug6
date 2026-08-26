INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(113,110,'SCREEN','보직 관리',3,'SCR-POSITION-ASSIGNMENT-MGMT','/admin/position-assignments','badge','SYSTEM','보직 대상자 및 유효기간 관리','Y','ACTIVE',1),
(114,110,'SCREEN','업무담당자 관리',4,'SCR-DUTY-ASSIGNMENT-MGMT','/admin/duty-assignments','briefcase','SYSTEM','업무조직별 담당자·담당영역 지정','Y','ACTIVE',1),
(170,120,'SCREEN','데이터 범위 권한',4,'SCR-DATA-SCOPE-RULE-MGMT','/admin/data-scope-rules','filter','SYSTEM','역할별 데이터 범위 규칙 설정','Y','ACTIVE',1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, parent_menu_id = EXCLUDED.parent_menu_id, url = EXCLUDED.url, screen_id = EXCLUDED.screen_id, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1 FROM menus WHERE menu_id IN (113,114,170)
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, icon = EXCLUDED.icon, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, '시스템관리자 BASIC-10 운영기능 메뉴 접근'
FROM menus WHERE menu_id IN (113,114,170)
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

INSERT INTO position_assignments (position_code, user_id, organization_code, effective_start_date, effective_end_date, status, confirmed_at, created_by, updated_by, change_reason)
SELECT 'DEPT_HEAD', u.user_id, 'KNUE-DEPT-COMP', CURRENT_DATE, NULL, 'ACTIVE', CURRENT_TIMESTAMP, 1, 1, '초기 보직 지정'
FROM users u
WHERE u.login_id = 'professor1'
  AND NOT EXISTS (SELECT 1 FROM position_assignments pa WHERE pa.position_code = 'DEPT_HEAD' AND pa.user_id = u.user_id AND pa.organization_code = 'KNUE-DEPT-COMP' AND pa.status = 'ACTIVE');

INSERT INTO duty_assignments (duty_organization, user_id, duty_area, valid_start_date, valid_end_date, data_scope_type, processing_permission, status, confirmed_at, created_by, updated_by, change_reason)
SELECT '단과대학', u.user_id, '교수업적평가', CURRENT_DATE, NULL, 'COLLEGE', 'READ_WRITE', 'ACTIVE', CURRENT_TIMESTAMP, 1, 1, '초기 업무담당자 지정'
FROM users u
WHERE u.login_id = 'professor1'
  AND NOT EXISTS (SELECT 1 FROM duty_assignments da WHERE da.user_id = u.user_id AND da.duty_area = '교수업적평가' AND da.status = 'ACTIVE');

INSERT INTO data_scope_rules (role_code, data_scope_type, organization_code, duty_area, status, created_by, updated_by, change_reason)
VALUES ('R01','SELF',NULL,NULL,'ACTIVE',1,1,'초기 데이터 범위'),
       ('R02','DEPARTMENT',NULL,NULL,'ACTIVE',1,1,'초기 데이터 범위'),
       ('R09','ALL',NULL,NULL,'ACTIVE',1,1,'초기 데이터 범위')
ON CONFLICT (role_code, data_scope_type, organization_code, duty_area) DO UPDATE SET status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;
