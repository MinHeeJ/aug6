-- 정상·기간 경계·확정 상태 seed와 기존 조직에 연결된 신규 화면 권한.
-- 진단 작업 seed는 정책 미승인 상태이며 업무 실적을 생성하거나 삭제하지 않는다.
INSERT INTO evaluation_management_items (
    element_id, management_item_code, management_item_name, sort_order,
    active_yn, teacher_editable_yn, required_yn, data_type, change_reason
)
SELECT
    element.element_id, seed.code, seed.name, 100, 'Y', 'Y', 'N', 'TEXT', '교육실적 입력 기준'
FROM (
    SELECT e.element_id
    FROM evaluation_elements e
    JOIN evaluation_items i ON i.item_id = e.item_id
    JOIN evaluation_areas a ON a.area_id = i.area_id
    WHERE a.area_code = 'EDUCATION'
      AND e.evaluation_year = '2026'
      AND e.active_yn = 'Y'
    ORDER BY e.element_id
    LIMIT 1
) element
CROSS JOIN (
    VALUES
        ('EMPLOYMENT_RATE_IMPROVEMENT', '취업률 제고'),
        ('COURSE_OPERATION', '강좌 개설·운영'),
        ('LECTURE_IMPROVEMENT', '강의개선'),
        ('EMPLOYMENT_RATE_ACHIEVEMENT', '취업률')
) seed(code, name)
ON CONFLICT (element_id, management_item_code) DO NOTHING;

INSERT INTO evaluation_element_management_item_settings (
    rule_version_id, target_scope, area_code, item_code, evaluation_year,
    element_code, management_item_code, management_item_name, sort_order,
    active_yn, teacher_editable_yn, teacher_editable_part, effective_start_date,
    effective_end_date, evaluation_confirmed_yn, change_reason
)
SELECT
    version.rule_version_id, 'KNUE-DEPT-COMP', 'EDUCATION', seed.code, '2026',
    seed.code, seed.code, seed.name, 100, 'Y', 'Y', 'SELF_REPORT',
    DATE '2026-01-01', DATE '2026-12-31', 'N', '기존 교원 소속과 일치하는 교육실적 입력 기준'
FROM evaluation_rule_versions version
CROSS JOIN (
    VALUES
        ('EMPLOYMENT_RATE_IMPROVEMENT', '취업률 제고'),
        ('COURSE_OPERATION', '강좌 개설·운영'),
        ('LECTURE_IMPROVEMENT', '강의개선'),
        ('EMPLOYMENT_RATE_ACHIEVEMENT', '취업률')
) seed(code, name)
WHERE version.version_code = 'B60-DRAFT-2026'
ON CONFLICT (
    rule_version_id, target_scope, area_code, item_code,
    evaluation_year, element_code, management_item_code
) DO NOTHING;

-- 기존 학과 소속 교원의 개발용 입력기간을 추가하고 기존 단과대학 기간은 변경하지 않는다.
INSERT INTO input_period_settings (
    evaluation_year, area_code, organization_code, user_type_code,
    start_at, end_at, base_date, active_yn, change_reason
)
SELECT
    '2026', 'EDUCATION', 'KNUE-DEPT-COMP', 'FACULTY',
    TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2026-12-31 23:59:59',
    DATE '2026-01-01', 'Y', '교육실적 개발용 입력기간: 기존 학과 소속 교원'
WHERE NOT EXISTS (
    SELECT 1
    FROM input_period_settings p
    WHERE p.evaluation_year = '2026'
      AND p.area_code = 'EDUCATION'
      AND p.organization_code = 'KNUE-DEPT-COMP'
      AND p.active_yn = 'Y'
      AND p.start_at <= TIMESTAMP '2026-12-31 23:59:59'
      AND p.end_at >= TIMESTAMP '2026-01-01 00:00:00'
);

INSERT INTO code_groups (group_id, group_name, system_use_yn, status)
VALUES ('EDUCATION_SEMESTER', '교육실적 학기', 'Y', 'ACTIVE')
ON CONFLICT (group_id) DO NOTHING;
INSERT INTO detail_codes (group_id, code_value, code_name, sort_order, system_use_yn, status)
VALUES
    ('EDUCATION_SEMESTER', '1', '1학기', 1, 'Y', 'ACTIVE'),
    ('EDUCATION_SEMESTER', '2', '2학기', 2, 'Y', 'ACTIVE')
ON CONFLICT (group_id, code_value) DO NOTHING;

INSERT INTO education_achievements (
    achievement_type, teacher_user_id, organization_code, evaluation_year,
    management_item_code, achievement_date, achievement_name, achievement_detail,
    achievement_status, created_by, updated_by, request_id
)
SELECT
    seed.kind, owner.user_id, mapping.organization_code, '2026',
    seed.kind, seed.day, seed.name, '{}', seed.status,
    owner.user_id, owner.user_id, 'education-achievement-seed'
FROM (
    VALUES
        ('EMPLOYMENT_RATE_IMPROVEMENT', 'professor2', DATE '2026-04-10', 'DRAFT', '취업률 제고 정상'),
        ('EMPLOYMENT_RATE_IMPROVEMENT', 'professor2', DATE '2025-12-31', 'SUBMITTED', '취업률 제고 평가대상 기간 밖 경고'),
        ('EMPLOYMENT_RATE_IMPROVEMENT', 'business-owner', DATE '2026-04-12', 'EVALUATION_CONFIRMED', '취업률 제고 평가확정 잠금'),
        ('COURSE_OPERATION', 'professor2', DATE '2026-04-10', 'DRAFT', '강좌 개설·운영 정상'),
        ('COURSE_OPERATION', 'professor2', DATE '2025-12-31', 'SUBMITTED', '강좌 개설·운영 평가대상 기간 밖 경고'),
        ('COURSE_OPERATION', 'business-owner', DATE '2026-04-12', 'EVALUATION_CONFIRMED', '강좌 개설·운영 평가확정 잠금'),
        ('LECTURE_IMPROVEMENT', 'professor2', DATE '2026-04-10', 'DRAFT', '강의개선 정상'),
        ('LECTURE_IMPROVEMENT', 'professor2', DATE '2025-12-31', 'SUBMITTED', '강의개선 평가대상 기간 밖 경고'),
        ('LECTURE_IMPROVEMENT', 'business-owner', DATE '2026-04-12', 'EVALUATION_CONFIRMED', '강의개선 평가확정 잠금'),
        ('EMPLOYMENT_RATE_ACHIEVEMENT', 'professor2', DATE '2026-04-10', 'DRAFT', '취업률 정상'),
        ('EMPLOYMENT_RATE_ACHIEVEMENT', 'professor2', DATE '2025-12-31', 'SUBMITTED', '취업률 평가대상 기간 밖 경고'),
        ('EMPLOYMENT_RATE_ACHIEVEMENT', 'business-owner', DATE '2026-04-12', 'EVALUATION_CONFIRMED', '취업률 평가확정 잠금')
) seed(kind, login_id, day, status, name)
JOIN users owner ON owner.login_id = seed.login_id
JOIN organization_user_mappings mapping ON mapping.user_id = owner.user_id
    AND mapping.organization_code = 'KNUE-DEPT-COMP'
    AND mapping.mapping_type = 'ORGANIZATION'
    AND mapping.status = 'ACTIVE'
ON CONFLICT DO NOTHING;

INSERT INTO employment_rate_improvement_achievement_details (
    achievement_id, special_lecture_start_date, special_lecture_end_date, mock_exam_question_period
)
SELECT
    a.achievement_id, a.achievement_date, a.achievement_date, '모의시험 출제기간 예시'
FROM education_achievements a
WHERE a.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
SELECT
    a.achievement_id, a.achievement_name
FROM education_achievements a
WHERE a.achievement_type = 'COURSE_OPERATION'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO lecture_improvement_achievement_details (
    achievement_id, performance_content, academic_year, semester_code
)
SELECT
    a.achievement_id, a.achievement_name, 2026,
    CASE WHEN a.achievement_status = 'SUBMITTED' THEN '2' ELSE '1' END
FROM education_achievements a
WHERE a.achievement_type = 'LECTURE_IMPROVEMENT'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO education_achievement_status_histories (
    achievement_type, achievement_id, next_status, action_type, opinion, processed_by, request_id
)
SELECT
    a.achievement_type, a.achievement_id, a.achievement_status,
    'SEED', '정상·경계·확정 차이 진단 데이터', a.created_by, a.request_id
FROM education_achievements a;

INSERT INTO employment_rate_batch_jobs (
    batch_job_id, evaluation_year, target_condition_json, action_type, status,
    total_count, processed_count, unprocessed_count, requester_user_id,
    diagnostic_message, request_id, created_by, updated_by
)
SELECT
    seed.id, '2026', '{"organizationCode":"KNUE-DEPT-COMP","diagnosticOnly":true}'::jsonb,
    seed.action, 'BLOCKED', 1, 0, 1, owner.user_id,
    seed.reason, 'employment-rate-diagnostic-seed', owner.user_id, owner.user_id
FROM (
    VALUES
        ('employment-rate-diagnostic-normal', 'GENERATE', '진단 전용: 생성 정책 미승인'),
        ('employment-rate-diagnostic-boundary', 'GENERATE', '진단 전용: 입력기간 경계 대상'),
        ('employment-rate-diagnostic-locked', 'DELETE', '진단 전용: 평가확정 대상 삭제 차단')
) seed(id, action, reason)
JOIN users owner ON owner.login_id = 'admin'
ON CONFLICT (batch_job_id) DO NOTHING;

INSERT INTO employment_rate_batch_job_items (
    batch_job_item_id, batch_job_id, target_user_id, processed_yn, unprocessed_reason
)
SELECT
    job.batch_job_id || '-item', job.batch_job_id, target.user_id, 'N', job.diagnostic_message
FROM employment_rate_batch_jobs job
JOIN users target ON target.login_id = 'professor2'
WHERE job.request_id = 'employment-rate-diagnostic-seed'
ON CONFLICT (batch_job_item_id) DO NOTHING;

WITH missing AS (
    SELECT seed.*, ROW_NUMBER() OVER (ORDER BY seed.display_order) AS sequence_number
    FROM (
        VALUES
        (1, 'SCR-EMPLOYMENT-RATE-IMPROVEMENTS', '/faculty/education/employment-rate-improvements', '취업률 제고 실적 관리'),
        (2, 'SCR-COURSE-OPERATIONS', '/faculty/education/course-operations', '강좌 개설·운영 실적 관리'),
        (3, 'SCR-LECTURE-IMPROVEMENTS', '/faculty/education/lecture-improvements', '강의개선 실적 관리'),
        (4, 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS', '/faculty/education/employment-rate-achievements', '취업률 실적 관리')
    ) seed(display_order, screen_id, url, name)
    WHERE NOT EXISTS (
        SELECT 1
        FROM menus m
        WHERE m.screen_id = seed.screen_id OR m.url = seed.url
    )
), maximum AS (
    SELECT COALESCE(MAX(menu_id), 0) AS menu_id
    FROM menus
)
INSERT INTO menus (
    menu_id, menu_type, menu_name, display_order, screen_id, url, business_category, description
)
SELECT
    maximum.menu_id + missing.sequence_number, 'SCREEN', missing.name,
    1100 + missing.display_order, missing.screen_id, missing.url,
    'EDUCATION_ACHIEVEMENT', missing.name
FROM missing
CROSS JOIN maximum;
SELECT setval(
    pg_get_serial_sequence('menus', 'menu_id'),
    (SELECT MAX(menu_id) FROM menus),
    true
);

INSERT INTO menu_execution_info (menu_id, screen_id, url, business_category, description)
SELECT
    m.menu_id, m.screen_id, m.url, m.business_category, m.description
FROM menus m
WHERE m.url IN (
    '/faculty/education/employment-rate-improvements',
    '/faculty/education/course-operations',
    '/faculty/education/lecture-improvements',
    '/faculty/education/employment-rate-achievements'
)
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, change_reason)
SELECT
    'ROLE', role.role_code, m.menu_id, 'ALLOW', '교육실적 메뉴 접근'
FROM menus m
CROSS JOIN (VALUES ('R01'), ('R02'), ('R04'), ('R09')) role(role_code)
WHERE m.url IN (
    '/faculty/education/employment-rate-improvements',
    '/faculty/education/course-operations',
    '/faculty/education/lecture-improvements',
    '/faculty/education/employment-rate-achievements'
)
ON CONFLICT (target_type, target_id, menu_id) DO NOTHING;
INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, change_reason)
SELECT
    'ROLE', 'R07', m.menu_id, 'ALLOW', '취업률 Excel·일괄 진단 화면 접근'
FROM menus m
WHERE m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS'
ON CONFLICT (target_type, target_id, menu_id) DO NOTHING;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason)
SELECT
    m.screen_id, rule.role_code, rule.function_type, 'ALLOW', '교육실적 operation별 기능 허용'
FROM menus m
CROSS JOIN (
    VALUES
        ('R01', 'READ'), ('R02', 'READ'), ('R04', 'READ'),
        ('R01', 'CREATE'), ('R01', 'UPDATE'),
        ('R09', 'READ'), ('R09', 'CREATE'), ('R09', 'UPDATE'), ('R09', 'EXECUTE')
) rule(role_code, function_type)
WHERE m.url IN (
    '/faculty/education/employment-rate-improvements',
    '/faculty/education/course-operations',
    '/faculty/education/lecture-improvements',
    '/faculty/education/employment-rate-achievements'
)
ON CONFLICT (screen_id, role_code, function_type) DO NOTHING;
INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason)
SELECT
    'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS', 'R07', rule.function_type, 'ALLOW', '취업률 Excel·일괄 진단 허용'
FROM (VALUES ('READ'), ('CREATE'), ('EXECUTE')) rule(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO NOTHING;

INSERT INTO excel_upload_templates (
    template_id, business_type, template_version, effective_date, change_reason
)
VALUES (
    'EMPLOYMENT_RATE_ACHIEVEMENT', 'EMPLOYMENT_RATE_ACHIEVEMENT', '1.0',
    DATE '2026-01-01', '취업률 실적 검증 후 사용자 확인·원자 반영 양식'
)
ON CONFLICT (template_id) DO NOTHING;
INSERT INTO excel_upload_template_rules (
    rule_id, template_id, required_column, column_order, code_rule_ref
)
SELECT
    'employment-rate-column-' || seed.position,
    'EMPLOYMENT_RATE_ACHIEVEMENT', seed.name, seed.position, seed.rule_reference
FROM (
    VALUES
        (1, 'templateVersion', 'excel_upload_templates.template_version'),
        (2, 'employeeNo', 'users.employee_no'),
        (3, 'evaluationYear', 'input_period_settings.evaluation_year'),
        (4, 'managementItemCode', 'evaluation_management_items.management_item_code'),
        (5, 'achievementDate', 'ISO_DATE'),
        (6, 'achievementName', 'TEXT'),
        (7, 'achievementDetail', 'JSON'),
        (8, 'attachmentRef', 'OPAQUE_ATTACHMENT_REFERENCE')
) seed(position, name, rule_reference)
ON CONFLICT (rule_id) DO NOTHING;
