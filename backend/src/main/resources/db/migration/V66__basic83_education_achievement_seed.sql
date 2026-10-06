-- Database-backed examples only: no current input-period or bulk-policy approval is granted.
-- Keep all existing users, roles, classification branches and old achievement rows unchanged.
INSERT INTO evaluation_rule_versions (
    version_code, effective_start_date, effective_end_date, version_status, change_reason
)
VALUES ('EDUCATION-LATE-2025', DATE '2025-01-01', DATE '2025-12-31', 'CONFIRMED', '교육 후반 조회 fixture')
ON CONFLICT (version_code) DO NOTHING;

INSERT INTO evaluation_areas (
    rule_version_id, area_code, area_name, sort_order, period_apply_method, change_reason
)
SELECT
    v.rule_version_id, 'EDUCATION', '교육', 1, 'YEAR', '교육 후반 조회 fixture'
FROM evaluation_rule_versions v
WHERE v.version_code = 'EDUCATION-LATE-2025'
ON CONFLICT (rule_version_id, area_code) DO NOTHING;

INSERT INTO evaluation_items (area_id, item_code, item_name, sort_order, score_apply_method, change_reason)
SELECT
    a.area_id, 'EDUCATION_LATE', '교육영역 후반 실적', 1, 'SUM', '교육 후반 조회 fixture'
FROM evaluation_areas a
JOIN evaluation_rule_versions v
    ON v.rule_version_id = a.rule_version_id
WHERE v.version_code = 'EDUCATION-LATE-2025'
  AND a.area_code = 'EDUCATION'
ON CONFLICT (area_id, item_code) DO NOTHING;

INSERT INTO evaluation_elements (
    item_id, evaluation_year, element_code, element_name, sort_order, change_reason
)
SELECT
    i.item_id, '2025', s.element_code, s.element_name, s.sort_order, '교육 후반 조회 fixture'
FROM evaluation_items i
JOIN evaluation_areas a
    ON a.area_id = i.area_id
JOIN evaluation_rule_versions v
    ON v.rule_version_id = a.rule_version_id
CROSS JOIN (
    VALUES
        ('FR-029', '취업률 제고', 1),
        ('FR-030', '강좌 개설·운영', 2),
        ('FR-031', '강의개선', 3),
        ('FR-032', '취업률 실적', 4)
) AS s(element_code, element_name, sort_order)
WHERE v.version_code = 'EDUCATION-LATE-2025'
  AND i.item_code = 'EDUCATION_LATE'
ON CONFLICT (item_id, evaluation_year, element_code) DO NOTHING;

INSERT INTO evaluation_management_items (
    element_id, management_item_code, management_item_name, sort_order,
    teacher_editable_yn, required_yn, data_type, change_reason
)
SELECT
    e.element_id, e.element_code, e.element_name, 1, 'Y', 'Y', 'TEXT', '교육 후반 조회 fixture'
FROM evaluation_elements e
JOIN evaluation_items i
    ON i.item_id = e.item_id
JOIN evaluation_areas a
    ON a.area_id = i.area_id
JOIN evaluation_rule_versions v
    ON v.rule_version_id = a.rule_version_id
WHERE v.version_code = 'EDUCATION-LATE-2025'
  AND e.evaluation_year = '2025'
ON CONFLICT (element_id, management_item_code) DO NOTHING;

-- Semester choices are persistent codes, not a hard-coded UI/service enumeration.
INSERT INTO code_groups (group_id, group_name, description, change_reason)
VALUES
    ('SEMESTER', '학기', '교육 실적의 학기 선택 코드', '교육 후반 fixture'),
    ('ACADEMIC_YEAR', '학년도', '교육 실적의 학년도 선택 코드', '교육 후반 fixture')
ON CONFLICT (group_id) DO NOTHING;
INSERT INTO detail_codes (group_id, code_value, code_name, sort_order, change_reason)
VALUES
    ('SEMESTER', '1', '1학기', 1, '교육 후반 fixture'),
    ('SEMESTER', '2', '2학기', 2, '교육 후반 fixture'),
    ('ACADEMIC_YEAR', '2025', '2025학년도', 1, '교육 후반 fixture')
ON CONFLICT (group_id, code_value) DO NOTHING;

INSERT INTO education_achievements (
    management_no, achievement_type, teacher_user_id, organization_code,
    evaluation_year, management_item_code, achievement_date, achievement_name,
    achievement_detail, achievement_status, created_by, updated_by
)
SELECT
    s.management_no, s.achievement_type, u.user_id, 'KNUE-DEPT-COMP',
    '2025', s.achievement_type, s.achievement_date, s.achievement_name,
    '{}'::jsonb, s.achievement_status, u.user_id, u.user_id
FROM (
    VALUES
        ('EDU-FR-029-2025-001', 'FR-029', 'professor1', DATE '2025-04-10',
            '취업률 제고 정상 입력 예시', 'DRAFT'),
        ('EDU-FR-029-2025-002', 'FR-029', 'professor2', DATE '2024-12-31',
            '취업률 제고 평가대상기간 경계 예시', 'SUBMITTED'),
        ('EDU-FR-029-2025-003', 'FR-029', 'business-owner', DATE '2025-04-12',
            '취업률 제고 확정 잠금 예시', 'EVALUATION_CONFIRMED'),
        ('EDU-FR-030-2025-001', 'FR-030', 'professor1', DATE '2025-04-10',
            '강좌 개설·운영 정상 입력 예시', 'DRAFT'),
        ('EDU-FR-030-2025-002', 'FR-030', 'professor2', DATE '2024-12-31',
            '강좌 개설·운영 평가대상기간 경계 예시', 'SUBMITTED'),
        ('EDU-FR-030-2025-003', 'FR-030', 'business-owner', DATE '2025-04-12',
            '강좌 개설·운영 확정 잠금 예시', 'EVALUATION_CONFIRMED'),
        ('EDU-FR-031-2025-001', 'FR-031', 'professor1', DATE '2025-04-10',
            '강의개선 정상 입력 예시', 'DRAFT'),
        ('EDU-FR-031-2025-002', 'FR-031', 'professor2', DATE '2024-12-31',
            '강의개선 평가대상기간 경계 예시', 'SUBMITTED'),
        ('EDU-FR-031-2025-003', 'FR-031', 'business-owner', DATE '2025-04-12',
            '강의개선 확정 잠금 예시', 'EVALUATION_CONFIRMED'),
        ('EDU-FR-032-2025-001', 'FR-032', 'professor1', DATE '2025-04-10',
            '취업률 실적 정상 입력 예시', 'DRAFT'),
        ('EDU-FR-032-2025-002', 'FR-032', 'professor2', DATE '2024-12-31',
            '취업률 실적 평가대상기간 경계 예시', 'SUBMITTED'),
        ('EDU-FR-032-2025-003', 'FR-032', 'business-owner', DATE '2025-04-12',
            '취업률 실적 확정 잠금 예시', 'EVALUATION_CONFIRMED')
) AS s(management_no, achievement_type, login_id, achievement_date, achievement_name, achievement_status)
JOIN users u
    ON u.login_id = s.login_id
ON CONFLICT (management_no) DO NOTHING;

INSERT INTO employment_rate_improvement_achievement_details (
    achievement_id, special_lecture_start_date, special_lecture_end_date, mock_exam_question_period
)
SELECT
    a.achievement_id,
    DATE '2025-04-01',
    CASE WHEN a.achievement_status = 'SUBMITTED' THEN DATE '2025-04-01' ELSE DATE '2025-04-03' END,
    '2025-03-01 ~ 2025-03-07'
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-FR-029-2025-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
SELECT
    a.achievement_id, a.achievement_name || ': 개설·운영 내역'
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-FR-030-2025-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO lecture_improvement_achievement_details (
    achievement_id, performance_content, academic_year, semester_code
)
SELECT
    a.achievement_id, a.achievement_name || ': 강의 개선 내용', 2025,
    CASE WHEN a.achievement_status = 'SUBMITTED' THEN '2' ELSE '1' END
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-FR-031-2025-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO education_achievement_status_histories (
    achievement_type, achievement_id, previous_status, next_status, action_type, opinion, processed_by
)
SELECT
    a.achievement_type, a.achievement_id, NULL, a.achievement_status,
    'SEED', '조회·경계·확정 상태 예시; 실제 사용자 상태전이 아님', a.created_by
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-FR-%-2025-%';

INSERT INTO data_change_histories (
    target_business, target_key, change_type, field_name, before_value, after_value, changed_by, change_reason
)
SELECT
    a.achievement_type, a.achievement_id::text, 'CREATE', 'snapshot', NULL,
    JSONB_BUILD_OBJECT(
        'achievement', TO_JSONB(a),
        'detail', COALESCE(TO_JSONB(er), TO_JSONB(co), TO_JSONB(li), '{}'::jsonb)
    )::text,
    a.created_by, '교육 후반 실적 조회 fixture 최초 상태'
FROM education_achievements a
LEFT JOIN employment_rate_improvement_achievement_details er
    ON er.achievement_id = a.achievement_id
LEFT JOIN course_operation_achievement_details co
    ON co.achievement_id = a.achievement_id
LEFT JOIN lecture_improvement_achievement_details li
    ON li.achievement_id = a.achievement_id
WHERE a.management_no LIKE 'EDU-FR-%-2025-%';

-- A disabled fixture identity owns historical examples; no default password or live login is added.
INSERT INTO users (login_id, password_hash, system_use_yn, status, change_reason)
VALUES ('employment-rate-fixture-operator', '!', 'N', 'INACTIVE', '교육 후반 작업결과 예시 소유자')
ON CONFLICT (login_id) DO NOTHING;
INSERT INTO user_roles (user_id, role_code, status, change_reason)
SELECT
    u.user_id, 'R07', 'ACTIVE', '교육 후반 작업결과 예시 역할'
FROM users u
WHERE u.login_id = 'employment-rate-fixture-operator'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles r
      WHERE r.user_id = u.user_id AND r.role_code = 'R07'
  );

INSERT INTO employment_rate_batch_jobs (
    batch_job_id, evaluation_year, target_condition_json, action_type,
    job_status, total_count, processed_count, unprocessed_count, requested_by,
    requested_at, started_at, completed_at, created_by, updated_by
)
SELECT
    s.batch_job_id, '2025', '{"fixture":true,"policyApproved":false}'::jsonb, s.action_type,
    'FAILED', 1, 0, 1, u.user_id,
    TIMESTAMP '2025-04-15 09:00:00', NULL, TIMESTAMP '2025-04-15 09:00:00', u.user_id, u.user_id
FROM users u
CROSS JOIN (
    VALUES
        ('EDU-EMPLOYMENT-JOB-001', 'GENERATE'),
        ('EDU-EMPLOYMENT-JOB-002', 'DELETE'),
        ('EDU-EMPLOYMENT-JOB-003', 'GENERATE')
) AS s(batch_job_id, action_type)
WHERE u.login_id = 'employment-rate-fixture-operator'
ON CONFLICT (batch_job_id) DO NOTHING;
INSERT INTO employment_rate_batch_job_items (
    batch_job_id, target_user_id, processed_yn, unprocessed_reason, created_by
)
SELECT
    j.batch_job_id, u.user_id, 'N', s.reason, j.requested_by
FROM employment_rate_batch_jobs j
JOIN (
    VALUES
        ('EDU-EMPLOYMENT-JOB-001', 'professor1', '예시: 생성 정책 미승인'),
        ('EDU-EMPLOYMENT-JOB-002', 'business-owner', '예시: 평가확정 및 삭제 정책 미승인'),
        ('EDU-EMPLOYMENT-JOB-003', 'professor2', '예시: 입력기간 비활성')
) AS s(batch_job_id, login_id, reason)
    ON s.batch_job_id = j.batch_job_id
JOIN users u
    ON u.login_id = s.login_id
ON CONFLICT (batch_job_id, target_user_id) DO NOTHING;

-- Resolve menu IDs from the real registry, rather than reserving potentially conflicting literal IDs.
INSERT INTO menus (
    menu_id, menu_type, menu_name, display_order, screen_id, url,
    icon, business_category, description, change_reason
)
SELECT
    (SELECT COALESCE(MAX(m.menu_id), 0) FROM menus m) + s.ordinal,
    'SCREEN', s.menu_name, 1100 + s.ordinal, s.screen_id, s.url,
    'clipboard-check', 'EDUCATION_ACHIEVEMENT', s.menu_name, '교육 후반 화면 등록'
FROM (
    VALUES
        (1, '취업률 제고 실적', 'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT',
            '/faculty/employment-rate-improvement-achievements'),
        (2, '강좌 개설·운영 실적', 'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
            '/faculty/course-offering-operation-achievements'),
        (3, '강의개선 실적', 'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT',
            '/faculty/teaching-improvement-achievements'),
        (4, '취업률 실적·일괄등록', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT',
            '/faculty/employment-rate-achievements')
) AS s(ordinal, menu_name, screen_id, url)
WHERE NOT EXISTS (
    SELECT 1
    FROM menus m
    WHERE m.screen_id = s.screen_id
);
INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description)
SELECT
    m.menu_id, m.screen_id, m.url, m.icon, m.business_category, m.description
FROM menus m
WHERE m.business_category = 'EDUCATION_ACHIEVEMENT'
  AND m.screen_id IN (
      'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT', 'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
      'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'
  )
ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, change_reason)
SELECT
    'ROLE', r.role_code, m.menu_id, 'ALLOW', '교육 후반 역할별 진입'
FROM menus m
CROSS JOIN (VALUES ('R01'), ('R02'), ('R04'), ('R07')) AS r(role_code)
WHERE m.screen_id IN (
      'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT', 'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
      'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'
  )
  AND (r.role_code <> 'R07' OR m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT')
ON CONFLICT (target_type, target_id, menu_id) DO NOTHING;
INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason)
SELECT
    m.screen_id, r.role_code, f.function_type, 'ALLOW', '교육 후반 API 역할과 기능권한 일치'
FROM menus m
CROSS JOIN (VALUES ('R01'), ('R02'), ('R04'), ('R07')) AS r(role_code)
CROSS JOIN (VALUES ('READ'), ('CREATE'), ('UPDATE'), ('EXECUTE')) AS f(function_type)
WHERE m.screen_id IN (
      'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT', 'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
      'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'
  )
  AND (
      (r.role_code IN ('R01', 'R02', 'R04') AND f.function_type = 'READ')
      OR (r.role_code = 'R01' AND f.function_type IN ('CREATE', 'UPDATE'))
      OR (r.role_code = 'R07' AND m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'
          AND f.function_type IN ('READ', 'EXECUTE'))
  )
ON CONFLICT (screen_id, role_code, function_type) DO NOTHING;

INSERT INTO excel_upload_templates (template_id, business_type, template_version, effective_date, change_reason)
VALUES ('EMPLOYMENT-RATE-V1', 'EMPLOYMENT_RATE', '1', DATE '2025-01-01', '취업률 실적 공통열 양식')
ON CONFLICT (template_id) DO NOTHING;
INSERT INTO excel_upload_template_rules (
    rule_id, template_id, required_column, column_order, code_rule_ref
)
VALUES
    ('EMPLOYMENT-RATE-V1-EMPLOYEE', 'EMPLOYMENT-RATE-V1', '교번', 1, 'users.employee_no'),
    ('EMPLOYMENT-RATE-V1-MANAGEMENT', 'EMPLOYMENT-RATE-V1', '관리항목코드', 2,
        'evaluation_management_items.management_item_code'),
    ('EMPLOYMENT-RATE-V1-DATE', 'EMPLOYMENT-RATE-V1', '업적발생일', 3, 'ISO_LOCAL_DATE'),
    ('EMPLOYMENT-RATE-V1-NAME', 'EMPLOYMENT-RATE-V1', '실적명', 4, 'TEXT'),
    ('EMPLOYMENT-RATE-V1-ATTACHMENT', 'EMPLOYMENT-RATE-V1', '첨부참조', 5, 'OPTIONAL_OWNED_FILE_REFERENCE')
ON CONFLICT (rule_id) DO NOTHING;
-- Template bytes are generated from the persisted rules by the XLSX service; no fake file token is seeded.
