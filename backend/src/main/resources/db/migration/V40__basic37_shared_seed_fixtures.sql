WITH admin AS (
    SELECT user_id FROM users WHERE login_id = 'admin'
), faculty_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-FACULTY-001', 'BASIC37 검증교원 001', 'KNUE-DEPT-COMP', '교수', 'ACTIVE', '학과장', 'BASIC37-SEED-RESEARCHER-001', 'DOCTOR', 'Y'),
        ('BASIC37-SEED-FACULTY-002', 'BASIC37 검증교원 002', 'KNUE-DEPT-COMP', '부교수', 'ACTIVE', '전공주임', 'BASIC37-SEED-RESEARCHER-002', 'MASTER', 'N'),
        ('BASIC37-SEED-FACULTY-003', 'BASIC37 검증교원 003', 'KNUE-DEPT-COMP', '조교수', 'ACTIVE', '연구책임자', 'BASIC37-SEED-RESEARCHER-003', 'DOCTOR', 'Y'),
        ('BASIC37-SEED-FACULTY-004', 'BASIC37 검증교원 004', 'KNUE-COL-EDU', '교수', 'LEAVE', '연구소장', 'BASIC37-SEED-RESEARCHER-004', 'DOCTOR', 'Y'),
        ('BASIC37-SEED-FACULTY-005', 'BASIC37 검증교원 005', 'KNUE-COL-EDU', '부교수', 'ACTIVE', '평가위원', 'BASIC37-SEED-RESEARCHER-005', 'BACHELOR', 'N')
    ) AS seed(employee_no, name, organization_code, rank_name, employment_status, position_name, researcher_registration_no, final_degree_type, degree_prerequisite_missing_yn)
)
INSERT INTO korus_personnel_snapshots (employee_no, name, organization_code, rank_name, employment_status, position_name, retirement_date, status, appointment_id, snapshot_year)
SELECT employee_no, name, organization_code, rank_name, employment_status, position_name, NULL, 'ACTIVE', employee_no || '-APPOINTMENT', 2026
FROM faculty_seed
ON CONFLICT (employee_no) DO UPDATE SET
    name = EXCLUDED.name,
    organization_code = EXCLUDED.organization_code,
    rank_name = EXCLUDED.rank_name,
    employment_status = EXCLUDED.employment_status,
    position_name = EXCLUDED.position_name,
    appointment_id = EXCLUDED.appointment_id,
    snapshot_year = EXCLUDED.snapshot_year,
    updated_at = CURRENT_TIMESTAMP;

WITH admin AS (
    SELECT user_id FROM users WHERE login_id = 'admin'
), faculty_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-FACULTY-001', 'basic37.faculty001'),
        ('BASIC37-SEED-FACULTY-002', 'basic37.faculty002'),
        ('BASIC37-SEED-FACULTY-003', 'basic37.faculty003'),
        ('BASIC37-SEED-FACULTY-004', 'basic37.faculty004'),
        ('BASIC37-SEED-FACULTY-005', 'basic37.faculty005')
    ) AS seed(employee_no, login_id)
)
INSERT INTO users (login_id, password_hash, employee_no, system_use_yn, status, created_by, updated_by, change_reason)
SELECT seed.login_id, 'sha256:disabled', seed.employee_no, 'Y', 'ACTIVE', admin_user.user_id, admin_user.user_id, 'BASIC-37 개발·검증용 교원 사용자 seed'
FROM faculty_seed seed
JOIN admin admin_user ON TRUE
ON CONFLICT (login_id) DO UPDATE SET
    employee_no = EXCLUDED.employee_no,
    system_use_yn = EXCLUDED.system_use_yn,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO user_roles (user_id, role_code, assignment_type, approver_user_id, status, change_reason)
SELECT seeded_user.user_id, 'R01', 'MANUAL', admin_user.user_id, 'ACTIVE', 'BASIC-37 개발·검증용 교원 역할 seed'
FROM users seeded_user
JOIN users admin_user ON admin_user.login_id = 'admin'
WHERE seeded_user.login_id IN ('basic37.faculty001', 'basic37.faculty002', 'basic37.faculty003', 'basic37.faculty004', 'basic37.faculty005')
  AND NOT EXISTS (
      SELECT 1 FROM user_roles existing
      WHERE existing.user_id = seeded_user.user_id
        AND existing.role_code = 'R01'
        AND existing.status = 'ACTIVE'
  );

WITH mapping_seed AS (
    SELECT * FROM (VALUES
        ('basic37.faculty001', 'KNUE-DEPT-COMP', '학과장'),
        ('basic37.faculty002', 'KNUE-DEPT-COMP', '전공주임'),
        ('basic37.faculty003', 'KNUE-DEPT-COMP', '연구책임자'),
        ('basic37.faculty004', 'KNUE-COL-EDU', '연구소장'),
        ('basic37.faculty005', 'KNUE-COL-EDU', '평가위원')
    ) AS seed(login_id, organization_code, position_name)
)
INSERT INTO organization_user_mappings (organization_code, user_id, position_name, mapping_type, effective_start_date, status)
SELECT seed.organization_code, seeded_user.user_id, seed.position_name, 'ORGANIZATION', DATE '2026-01-01', 'ACTIVE'
FROM mapping_seed seed
JOIN users seeded_user ON seeded_user.login_id = seed.login_id
WHERE NOT EXISTS (
    SELECT 1 FROM organization_user_mappings existing
    WHERE existing.organization_code = seed.organization_code
      AND existing.user_id = seeded_user.user_id
      AND existing.mapping_type = 'ORGANIZATION'
      AND existing.effective_start_date = DATE '2026-01-01'
);

WITH faculty_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-FACULTY-001', '010-3701-0001', 'BASIC37-SEED-RESEARCHER-001', 'DOCTOR', 'Y'),
        ('BASIC37-SEED-FACULTY-002', '010-3701-0002', 'BASIC37-SEED-RESEARCHER-002', 'MASTER', 'N'),
        ('BASIC37-SEED-FACULTY-003', '010-3701-0003', 'BASIC37-SEED-RESEARCHER-003', 'DOCTOR', 'Y'),
        ('BASIC37-SEED-FACULTY-004', '010-3701-0004', 'BASIC37-SEED-RESEARCHER-004', 'DOCTOR', 'Y'),
        ('BASIC37-SEED-FACULTY-005', '010-3701-0005', 'BASIC37-SEED-RESEARCHER-005', 'BACHELOR', 'N')
    ) AS seed(employee_no, contact, researcher_registration_no, final_degree_type, degree_prerequisite_missing_yn)
)
INSERT INTO researcher_profiles (employee_no, contact, researcher_registration_no, external_provision_yn, information_public_yn, final_degree_type, degree_prerequisite_missing_yn, created_by, updated_by)
SELECT seed.employee_no, seed.contact, seed.researcher_registration_no, 'N', 'Y', seed.final_degree_type, seed.degree_prerequisite_missing_yn, admin_user.user_id, admin_user.user_id
FROM faculty_seed seed
JOIN users admin_user ON admin_user.login_id = 'admin'
ON CONFLICT (employee_no) DO UPDATE SET
    contact = EXCLUDED.contact,
    researcher_registration_no = EXCLUDED.researcher_registration_no,
    information_public_yn = EXCLUDED.information_public_yn,
    final_degree_type = EXCLUDED.final_degree_type,
    degree_prerequisite_missing_yn = EXCLUDED.degree_prerequisite_missing_yn,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

WITH degree_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-FACULTY-001', 'DOCTOR', '한국교원대학교 BASIC37-SEED-DEGREE-001', '202402', 'BASIC37-SEED-DEGREE-001 지도교수'),
        ('BASIC37-SEED-FACULTY-002', 'MASTER', '한국교원대학교 BASIC37-SEED-DEGREE-002', '202302', 'BASIC37-SEED-DEGREE-002 지도교수'),
        ('BASIC37-SEED-FACULTY-003', 'DOCTOR', '한국교원대학교 BASIC37-SEED-DEGREE-003', '202202', 'BASIC37-SEED-DEGREE-003 지도교수'),
        ('BASIC37-SEED-FACULTY-004', 'DOCTOR', '한국교원대학교 BASIC37-SEED-DEGREE-004', '202102', 'BASIC37-SEED-DEGREE-004 지도교수'),
        ('BASIC37-SEED-FACULTY-005', 'BACHELOR', '한국교원대학교 BASIC37-SEED-DEGREE-005', '202002', 'BASIC37-SEED-DEGREE-005 지도교수')
    ) AS seed(employee_no, degree_type, university_name, acquired_ym, advisor_name)
)
INSERT INTO researcher_degrees (employee_no, degree_type, university_name, start_ym, acquired_ym, country_name, college_name, advisor_name, changed_by)
SELECT seed.employee_no, seed.degree_type, seed.university_name, '201803', seed.acquired_ym, '대한민국', 'BASIC-37 검증대학원', seed.advisor_name, admin_user.user_id
FROM degree_seed seed
JOIN users admin_user ON admin_user.login_id = 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM researcher_degrees existing
    WHERE existing.employee_no = seed.employee_no
      AND existing.degree_type = seed.degree_type
      AND existing.university_name = seed.university_name
);

WITH batch_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-BATCH-001', 'FACULTY_PROFILE_SYNC', '2026-08-27 01:00:00'::timestamp, '2026-08-27 01:05:00'::timestamp, 50, 49, 1, 0, 300000, 'COMPLETED'),
        ('BASIC37-SEED-BATCH-002', 'RESEARCHER_PROFILE_INDEX', '2026-08-27 02:00:00'::timestamp, '2026-08-27 02:04:10'::timestamp, 45, 45, 0, 0, 250000, 'COMPLETED'),
        ('BASIC37-SEED-BATCH-003', 'DEGREE_PREREQUISITE_SCAN', '2026-08-27 03:00:00'::timestamp, '2026-08-27 03:06:00'::timestamp, 30, 27, 3, 0, 360000, 'FAILED'),
        ('BASIC37-SEED-BATCH-004', 'EXCEL_TEMPLATE_INDEX', '2026-08-27 04:00:00'::timestamp, '2026-08-27 04:03:20'::timestamp, 20, 20, 0, 0, 200000, 'COMPLETED'),
        ('BASIC37-SEED-BATCH-005', 'MENU_PERMISSION_REFRESH', '2026-08-27 05:00:00'::timestamp, '2026-08-27 05:02:30'::timestamp, 15, 15, 0, 0, 150000, 'COMPLETED')
    ) AS seed(seed_id, batch_type, started_at, ended_at, total_count, success_count, failure_count, excluded_count, elapsed_millis, execution_status)
)
INSERT INTO batch_definitions (batch_id, batch_type, schedule_cycle, max_execution_seconds, owner_user_id, request_id, created_by, updated_by)
SELECT seed.seed_id, seed.batch_type, 'MANUAL BASIC-37 검증', 3600, admin_user.user_id, 'basic37-seed-fixture', admin_user.user_id, admin_user.user_id
FROM batch_seed seed
JOIN users admin_user ON admin_user.login_id = 'admin'
ON CONFLICT (batch_id) DO NOTHING;

WITH batch_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-BATCH-001', 'COMPLETED'),
        ('BASIC37-SEED-BATCH-002', 'COMPLETED'),
        ('BASIC37-SEED-BATCH-003', 'FAILED'),
        ('BASIC37-SEED-BATCH-004', 'COMPLETED'),
        ('BASIC37-SEED-BATCH-005', 'COMPLETED')
    ) AS seed(seed_id, execution_status)
)
INSERT INTO batch_executions (execution_id, batch_id, process_type, reason, operator_user_id, execution_status, original_execution_id, request_id)
SELECT seed.seed_id, seed.seed_id, 'MANUAL_RUN', 'BASIC-37 개발·검증용 배치 결과 seed', admin_user.user_id, seed.execution_status, NULL, 'basic37-seed-fixture'
FROM batch_seed seed
JOIN users admin_user ON admin_user.login_id = 'admin'
ON CONFLICT (execution_id) DO NOTHING;

WITH batch_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-BATCH-001', '2026-08-27 01:00:00'::timestamp, '2026-08-27 01:05:00'::timestamp, 50, 49, 1, 0, 300000),
        ('BASIC37-SEED-BATCH-002', '2026-08-27 02:00:00'::timestamp, '2026-08-27 02:04:10'::timestamp, 45, 45, 0, 0, 250000),
        ('BASIC37-SEED-BATCH-003', '2026-08-27 03:00:00'::timestamp, '2026-08-27 03:06:00'::timestamp, 30, 27, 3, 0, 360000),
        ('BASIC37-SEED-BATCH-004', '2026-08-27 04:00:00'::timestamp, '2026-08-27 04:03:20'::timestamp, 20, 20, 0, 0, 200000),
        ('BASIC37-SEED-BATCH-005', '2026-08-27 05:00:00'::timestamp, '2026-08-27 05:02:30'::timestamp, 15, 15, 0, 0, 150000)
    ) AS seed(seed_id, started_at, ended_at, total_count, success_count, failure_count, excluded_count, elapsed_millis)
)
INSERT INTO batch_execution_results (execution_id, started_at, ended_at, total_count, success_count, failure_count, excluded_count, elapsed_millis)
SELECT seed_id, started_at, ended_at, total_count, success_count, failure_count, excluded_count, elapsed_millis
FROM batch_seed
ON CONFLICT (execution_id) DO NOTHING;

INSERT INTO batch_execution_logs (execution_id, log_file_ref)
SELECT seed_id, 'logs/batch/' || seed_id || '.log'
FROM (VALUES
    ('BASIC37-SEED-BATCH-001'),
    ('BASIC37-SEED-BATCH-002'),
    ('BASIC37-SEED-BATCH-003'),
    ('BASIC37-SEED-BATCH-004'),
    ('BASIC37-SEED-BATCH-005')
) AS seed(seed_id)
ON CONFLICT (execution_id) DO NOTHING;

WITH template_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-EXCEL-TEMPLATE-001', 'FACULTY_PROFILE', 'v2026.1', DATE '2026-01-01'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-002', 'RESEARCHER_PROFILE', 'v2026.1', DATE '2026-01-01'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-003', 'DEGREE_DEFICIENCY', 'v2026.1', DATE '2026-01-01'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-004', 'BATCH_RESULT', 'v2026.1', DATE '2026-01-01'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-005', 'MENU_PERMISSION', 'v2026.1', DATE '2026-01-01')
    ) AS seed(template_id, business_type, template_version, effective_date)
)
INSERT INTO excel_upload_templates (template_id, business_type, template_version, effective_date, system_use_yn, status, created_by, updated_by, change_reason)
SELECT seed.template_id, seed.business_type, seed.template_version, seed.effective_date, 'Y', 'ACTIVE', admin_user.user_id, admin_user.user_id, 'BASIC-37 개발·검증용 업로드 양식 seed'
FROM template_seed seed
JOIN users admin_user ON admin_user.login_id = 'admin'
ON CONFLICT (template_id) DO UPDATE SET
    business_type = EXCLUDED.business_type,
    template_version = EXCLUDED.template_version,
    effective_date = EXCLUDED.effective_date,
    system_use_yn = EXCLUDED.system_use_yn,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

WITH rule_seed AS (
    SELECT * FROM (VALUES
        ('BASIC37-SEED-EXCEL-TEMPLATE-001', '교번'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-002', '연구자등록번호'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-003', '학위구분'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-004', '배치실행ID'),
        ('BASIC37-SEED-EXCEL-TEMPLATE-005', '메뉴ID')
    ) AS seed(template_id, second_column)
), expanded_rules AS (
    SELECT template_id, template_id || '-RULE-001' AS rule_id, '업무구분' AS required_column, 1 AS column_order, 'COMMON_STATUS.ACTIVE' AS code_rule_ref FROM rule_seed
    UNION ALL
    SELECT template_id, template_id || '-RULE-002' AS rule_id, second_column AS required_column, 2 AS column_order, 'COMMON_STATUS.ACTIVE' AS code_rule_ref FROM rule_seed
)
INSERT INTO excel_upload_template_rules (rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by)
SELECT rules.rule_id, rules.template_id, rules.required_column, rules.column_order, rules.code_rule_ref, admin_user.user_id, admin_user.user_id
FROM expanded_rules rules
JOIN users admin_user ON admin_user.login_id = 'admin'
ON CONFLICT (rule_id) DO NOTHING;

INSERT INTO excel_upload_template_files (file_token, template_id, original_file_name, content_type, file_size_bytes, created_by)
SELECT template_id || '-FILE-TOKEN', template_id, template_id || '.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 4096, admin_user.user_id
FROM (VALUES
    ('BASIC37-SEED-EXCEL-TEMPLATE-001'),
    ('BASIC37-SEED-EXCEL-TEMPLATE-002'),
    ('BASIC37-SEED-EXCEL-TEMPLATE-003'),
    ('BASIC37-SEED-EXCEL-TEMPLATE-004'),
    ('BASIC37-SEED-EXCEL-TEMPLATE-005')
) AS seed(template_id)
JOIN users admin_user ON admin_user.login_id = 'admin'
ON CONFLICT (file_token) DO NOTHING;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by, change_reason) VALUES
(3701, 3630, 'SCREEN', '교원 검색 목록', 1, 'SCR-FACULTY-SEARCH-LIST', '/admin/researcher-profiles/faculty-search', 'search', 'BASIC37', 'BASIC-37 교원 검색 목록 조회 검증 메뉴', 'Y', 'ACTIVE', 1, 'BASIC-37 shared seed fixture'),
(3702, 3630, 'SCREEN', '연구자 프로필 목록', 2, 'SCR-RESEARCHER-PROFILE-LIST', '/admin/researcher-profiles', 'id-card', 'BASIC37', 'BASIC-37 연구자 프로필 목록 조회 검증 메뉴', 'Y', 'ACTIVE', 1, 'BASIC-37 shared seed fixture'),
(3703, 3630, 'SCREEN', '선행학위 미충족 대상', 3, 'SCR-DEGREE-DEFICIENCY-TARGET-LIST', '/admin/researcher-profiles/degree-deficiencies', 'alert-triangle', 'BASIC37', 'BASIC-37 선행학위 미충족 대상 조회 검증 메뉴', 'Y', 'ACTIVE', 1, 'BASIC-37 shared seed fixture')
ON CONFLICT (menu_id) DO UPDATE SET
    parent_menu_id = EXCLUDED.parent_menu_id,
    menu_name = EXCLUDED.menu_name,
    display_order = EXCLUDED.display_order,
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    system_use_yn = EXCLUDED.system_use_yn,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1
FROM menus
WHERE menu_id IN (3701, 3702, 3703)
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_code, menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-37 조회 오류 수정 검증 메뉴 접근'
FROM roles
CROSS JOIN (VALUES (3701), (3702), (3703), (193), (211)) AS menu_ids(menu_id)
WHERE role_code IN ('R04', 'R09')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;
