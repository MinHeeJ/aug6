-- 교육실적 연관 검증 시드. 신규 작업 실행 정책을 승인하거나 실행하지 않는다.
INSERT INTO evaluation_areas (
    rule_version_id, area_code, area_name, sort_order, active_yn,
    period_apply_method, change_reason, created_by, updated_by
)
SELECT
    r.rule_version_id, 'EDUCATION', '교육영역', 1, 'Y',
    'AREA_PERIOD', '교육실적 기준 참조 시드', u.user_id, u.user_id
FROM evaluation_rule_versions r
CROSS JOIN users u
WHERE r.version_code = 'B33-DRAFT-2026'
    AND u.login_id = 'admin'
ON CONFLICT (rule_version_id, area_code) DO NOTHING;

INSERT INTO evaluation_items (
    area_id, item_code, item_name, sort_order, active_yn,
    score_apply_method, change_reason, created_by, updated_by
)
SELECT
    a.area_id, 'EDUCATION_ACHIEVEMENT', '교육영역 실적', 90, 'Y',
    'MANAGEMENT_ITEM', '교육실적 기준 참조 시드', u.user_id, u.user_id
FROM evaluation_areas a
JOIN evaluation_rule_versions r ON r.rule_version_id = a.rule_version_id
CROSS JOIN users u
WHERE r.version_code = 'B33-DRAFT-2026'
    AND a.area_code = 'EDUCATION'
    AND u.login_id = 'admin'
ON CONFLICT (area_id, item_code) DO NOTHING;

INSERT INTO evaluation_elements (
    item_id, evaluation_year, element_code, element_name, sort_order,
    active_yn, change_reason, created_by, updated_by
)
SELECT
    i.item_id, '2026', 'EDUCATION_EXTENSION', '교육실적 후반', 90,
    'Y', '교육실적 기준 참조 시드', u.user_id, u.user_id
FROM evaluation_items i
JOIN evaluation_areas a ON a.area_id = i.area_id
JOIN evaluation_rule_versions r ON r.rule_version_id = a.rule_version_id
CROSS JOIN users u
WHERE r.version_code = 'B33-DRAFT-2026'
    AND a.area_code = 'EDUCATION'
    AND i.item_code = 'EDUCATION_ACHIEVEMENT'
    AND u.login_id = 'admin'
ON CONFLICT (item_id, evaluation_year, element_code) DO NOTHING;

INSERT INTO evaluation_management_items (
    element_id, management_item_code, management_item_name, sort_order, active_yn,
    teacher_editable_yn, required_yn, data_type, change_reason, created_by, updated_by
)
SELECT
    e.element_id, s.code, s.name, s.sort_order, 'Y',
    'Y', 'Y', 'TEXT', '교육실적 관리항목 시드', u.user_id, u.user_id
FROM evaluation_elements e
JOIN evaluation_items i ON i.item_id = e.item_id
JOIN evaluation_areas a ON a.area_id = i.area_id
JOIN evaluation_rule_versions r ON r.rule_version_id = a.rule_version_id
CROSS JOIN users u
CROSS JOIN (
    VALUES
        ('EMPLOYMENT_RATE_IMPROVEMENT', '취업률 제고', 1),
        ('COURSE_OPERATION', '강좌 개설·운영', 2),
        ('LECTURE_IMPROVEMENT', '강의개선', 3),
        ('EMPLOYMENT_RATE_ACHIEVEMENT', '취업률 실적', 4)
) AS s(code, name, sort_order)
WHERE r.version_code = 'B33-DRAFT-2026'
    AND a.area_code = 'EDUCATION'
    AND i.item_code = 'EDUCATION_ACHIEVEMENT'
    AND e.element_code = 'EDUCATION_EXTENSION'
    AND e.evaluation_year = '2026'
    AND u.login_id = 'admin'
ON CONFLICT (element_id, management_item_code) DO NOTHING;

-- 기존 교원은 COMP 소속이고 기존 EDU 입력기간만으로는 저장 불가하다.
-- 기존 설정은 그대로 두고 동일 평가년도의 COMP 시드 범위를 명시한다.
INSERT INTO input_period_settings (
    evaluation_year, area_code, organization_code, user_type_code, start_at, end_at,
    active_yn, created_by, updated_by, change_reason
)
SELECT
    '2026', 'EDUCATION', m.organization_code, 'FACULTY',
    TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2026-12-31 23:59:59',
    'Y', u.user_id, u.user_id, '교육실적 기존 교원 소속의 정상 입력 시드'
FROM users u
JOIN organization_user_mappings m ON m.user_id = u.user_id
WHERE u.login_id = 'professor1'
    AND m.organization_code = 'KNUE-DEPT-COMP'
    AND m.mapping_type = 'ORGANIZATION'
    AND m.status = 'ACTIVE'
    AND NOT EXISTS (
        SELECT 1
        FROM input_period_settings p
        WHERE p.evaluation_year = '2026'
            AND p.area_code = 'EDUCATION'
            AND p.organization_code = m.organization_code
            AND p.active_yn = 'Y'
    );

INSERT INTO education_achievements (
    achievement_type, management_no, teacher_user_id, organization_code, evaluation_year,
    management_item_code, achievement_date, achievement_status,
    attachment_ref, achievement_detail, created_by, updated_by
)
SELECT
    t.code, 'EDU-' || t.code || '-' || s.number, u.user_id, m.organization_code, '2026',
    t.code, s.achievement_date, s.status,
    '[]', jsonb_build_object('achievementName', s.name)::text, u.user_id, u.user_id
FROM (
    VALUES
        ('EMPLOYMENT_RATE_IMPROVEMENT'), ('COURSE_OPERATION'),
        ('LECTURE_IMPROVEMENT'), ('EMPLOYMENT_RATE_ACHIEVEMENT')
) AS t(code)
CROSS JOIN (
    VALUES
        ('001', 'professor1', DATE '2026-03-15', 'DRAFT', '정상 입력 실적'),
        ('002', 'professor2', DATE '2025-12-31', 'SUBMITTED', '평가대상 기간 밖 경계 실적'),
        ('003', 'business-owner', DATE '2026-03-20', 'EVALUATION_CONFIRMED', '평가확정 잠금 실적')
) AS s(number, login_id, achievement_date, status, name)
JOIN users u ON u.login_id = s.login_id
JOIN organization_user_mappings m ON m.user_id = u.user_id
    AND m.mapping_type = 'ORGANIZATION'
    AND m.organization_code = 'KNUE-DEPT-COMP'
    AND m.status = 'ACTIVE'
ON CONFLICT (management_no) DO NOTHING;

INSERT INTO employment_rate_improvement_achievement_details (
    achievement_id, special_lecture_start_date, special_lecture_end_date, mock_exam_question_period
)
SELECT
    a.achievement_id, a.achievement_date, a.achievement_date + 1, '3월 모의시험 출제'
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-EMPLOYMENT_RATE_IMPROVEMENT-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO course_operation_achievement_details (achievement_id, performance_detail)
SELECT
    a.achievement_id, '교육과정 강좌 개설 및 운영 실적'
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-COURSE_OPERATION-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO lecture_improvement_achievement_details (
    achievement_id, performance_content, academic_year, semester_code
)
SELECT
    a.achievement_id, '강의 개선 보고서 작성', 2025,
    CASE WHEN a.achievement_status = 'DRAFT' THEN '1' ELSE '2' END
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-LECTURE_IMPROVEMENT-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO education_achievement_status_histories (
    achievement_type, achievement_id, previous_status, next_status, action_type,
    opinion, processed_by, request_id
)
SELECT
    a.achievement_type, a.achievement_id, NULL, a.achievement_status, 'CREATE',
    '연관 상태 검증 시드', a.created_by, 'SEED-' || a.management_no
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-%'
    AND NOT EXISTS (
        SELECT 1
        FROM education_achievement_status_histories h
        WHERE h.achievement_type = a.achievement_type
            AND h.achievement_id = a.achievement_id
    );

INSERT INTO data_change_histories (
    target_business, target_key, change_type, field_name, before_value, after_value,
    changed_by, change_reason, request_id
)
SELECT
    a.achievement_type, a.achievement_id::text, 'CREATE', 'achievement_detail',
    NULL, a.achievement_detail, a.created_by, '교육실적 연관 감사 시드', 'SEED-' || a.management_no
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-%';

-- 결과조회 검증용 기록이며 실제 GENERATE/DELETE 실행으로 생성된 업무자료가 아니다.
INSERT INTO employment_rate_batch_jobs (
    evaluation_year, organization_code, target_condition_json, action_type,
    job_status, total_count, processed_count, unprocessed_count,
    request_id, created_by, updated_by
)
SELECT
    '2026', 'KNUE-DEPT-COMP', '{"seedResultOnly":true}', s.action_type,
    s.status, 1, s.processed_count, s.unprocessed_count,
    s.request_id, u.user_id, u.user_id
FROM users u
CROSS JOIN (
    VALUES
        ('GENERATE', 'COMPLETED', 1, 0, 'SEED-EMPLOYMENT-RESULT-001'),
        ('GENERATE', 'FAILED', 0, 1, 'SEED-EMPLOYMENT-RESULT-002'),
        ('DELETE', 'FAILED', 0, 1, 'SEED-EMPLOYMENT-RESULT-003')
) AS s(action_type, status, processed_count, unprocessed_count, request_id)
WHERE u.login_id = 'admin'
ON CONFLICT (request_id) DO NOTHING;

INSERT INTO employment_rate_batch_job_items (
    batch_job_id, target_user_id, processed_yn, unprocessed_reason, created_by, updated_by
)
SELECT
    j.batch_job_id, u.user_id, s.processed_yn, s.reason, j.created_by, j.created_by
FROM employment_rate_batch_jobs j
JOIN (
    VALUES
        ('SEED-EMPLOYMENT-RESULT-001', 'professor1', 'Y', NULL),
        ('SEED-EMPLOYMENT-RESULT-002', 'professor2', 'N', '중복 대상 경계 시드'),
        ('SEED-EMPLOYMENT-RESULT-003', 'business-owner', 'N', '평가확정 변경금지 시드')
) AS s(request_id, login_id, processed_yn, reason) ON s.request_id = j.request_id
JOIN users u ON u.login_id = s.login_id
ON CONFLICT (batch_job_id, target_user_id) DO NOTHING;

INSERT INTO excel_upload_templates (
    template_id, business_type, template_version, effective_date, created_by, updated_by, change_reason
)
SELECT
    'EMPLOYMENT-RATE-TEMPLATE-V1', 'EMPLOYMENT_RATE_ACHIEVEMENT', 'v1.0', DATE '2026-01-01',
    u.user_id, u.user_id, '취업률 표준 XLSX 양식 메타데이터'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO excel_upload_template_rules (
    rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by
)
SELECT
    'EMPLOYMENT-RATE-COLUMN-' || s.column_order,
    'EMPLOYMENT-RATE-TEMPLATE-V1', s.column_name, s.column_order,
    s.code_rule_ref, u.user_id, u.user_id
FROM users u
CROSS JOIN (
    VALUES
        ('교번', 1, 'users.employee_no'),
        ('관리항목코드', 2, 'evaluation_management_items.management_item_code'),
        ('업적발생일', 3, 'education_achievements.achievement_date'),
        ('실적명', 4, 'education_achievements.achievement_detail.achievementName'),
        ('첨부참조', 5, 'education_achievements.attachment_ref')
) AS s(column_name, column_order, code_rule_ref)
WHERE u.login_id = 'admin'
ON CONFLICT (rule_id) DO NOTHING;

INSERT INTO menus (
    menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url,
    icon, business_category, description, system_use_yn, status, updated_by, change_reason
)
SELECT
    (SELECT COALESCE(MAX(m.menu_id), 0) FROM menus m) + s.number,
    NULL, 'SCREEN', s.name, 1100 + s.number, s.screen_id, s.url,
    'clipboard-list', 'EDUCATION_ACHIEVEMENT', s.name, 'Y', 'ACTIVE', u.user_id, '교육실적 메뉴 연결'
FROM users u
CROSS JOIN (
    VALUES
        (1, '취업률 제고 실적', 'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT',
            '/faculty/employment-rate-improvement-achievements'),
        (2, '강좌 개설·운영 실적', 'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
            '/faculty/course-offering-operation-achievements'),
        (3, '강의개선 실적', 'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT',
            '/faculty/teaching-improvement-achievements'),
        (4, '취업률 실적', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT', '/faculty/employment-rate-achievements')
) AS s(number, name, screen_id, url)
WHERE u.login_id = 'admin'
    AND NOT EXISTS (
        SELECT 1
        FROM menus m
        WHERE m.screen_id = s.screen_id
    );

INSERT INTO menu_execution_info (
    menu_id, screen_id, url, icon, business_category, description, updated_by
)
SELECT
    m.menu_id, m.screen_id, m.url, m.icon, m.business_category, m.description, m.updated_by
FROM menus m
WHERE m.url IN (
    '/faculty/employment-rate-improvement-achievements', '/faculty/course-offering-operation-achievements',
    '/faculty/teaching-improvement-achievements', '/faculty/employment-rate-achievements'
)
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO menu_permissions (
    target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason
)
SELECT
    'ROLE', r.role_code, m.menu_id, 'ALLOW', 'ACTIVE', u.user_id, u.user_id, '교육실적 메뉴 권한'
FROM menus m
CROSS JOIN users u
CROSS JOIN roles r
WHERE u.login_id = 'admin'
    AND m.url IN (
        '/faculty/employment-rate-improvement-achievements', '/faculty/course-offering-operation-achievements',
        '/faculty/teaching-improvement-achievements', '/faculty/employment-rate-achievements'
    )
    AND (
        r.role_code IN ('R01', 'R02', 'R04', 'R09')
        OR (r.role_code = 'R07' AND m.url = '/faculty/employment-rate-achievements')
    )
ON CONFLICT (target_type, target_id, menu_id) DO NOTHING;
