-- 신규 화면만 기존 메뉴·함수 권한 체계에 연결한다. 기존 관리자 정책은 유지한다.
WITH missing AS (
    SELECT
        seed.*,
        ROW_NUMBER() OVER (ORDER BY seed.sequence) AS new_sequence
    FROM (VALUES
    (1, 'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT',
        '/faculty/employment-rate-improvement-achievements', '취업률 제고 실적'),
    (2, 'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT', '/faculty/course-offering-operation-achievements', '강좌 개설·운영 실적'),
    (3, 'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT', '/faculty/teaching-improvement-achievements', '강의개선 실적'),
    (4, 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT', '/faculty/employment-rate-achievements', '취업률 실적')
    ) AS seed(sequence, screen_id, url, menu_name)
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
    menu_id, parent_menu_id, menu_type, menu_name, display_order,
    screen_id, url, icon, business_category, description, updated_by, change_reason
)
SELECT
    maximum.menu_id + missing.new_sequence,
    NULL,
    'SCREEN',
    missing.menu_name,
    1100 + missing.sequence,
    missing.screen_id,
    missing.url,
    'graduation-cap',
    'EDUCATION_ACHIEVEMENT',
    missing.menu_name,
    u.user_id,
    '교육영역 후반 실적 화면 등록'
FROM missing
CROSS JOIN maximum
CROSS JOIN users u
WHERE u.login_id = 'admin';
INSERT INTO menu_execution_info (
    menu_id, screen_id, url, icon, business_category, description, updated_by
)
SELECT
    m.menu_id,
    m.screen_id,
    m.url,
    m.icon,
    m.business_category,
    m.description,
    m.updated_by
FROM menus m
WHERE m.screen_id IN (
    'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT',
    'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
    'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT',
    'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'
)
ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO menu_permissions (
    target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason
)
SELECT
    'ROLE',
    r.role_code,
    m.menu_id,
    'ALLOW',
    'ACTIVE',
    u.user_id,
    u.user_id,
    '교육영역 업무 역할 메뉴 접근'
FROM menus m
CROSS JOIN (VALUES ('R01'), ('R02'), ('R04'), ('R07'), ('R09')) AS r(role_code)
CROSS JOIN users u
WHERE m.screen_id IN (
    'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT',
    'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
    'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT',
    'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'
)
  AND (r.role_code <> 'R07' OR m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT')
  AND u.login_id = 'admin'
ON CONFLICT (target_type, target_id, menu_id) DO NOTHING;
INSERT INTO function_permissions (
    screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by
)
SELECT
    m.screen_id,
    permission.role_code,
    permission.function_type,
    'ALLOW',
    '교육영역 실적 업무 함수 권한',
    u.user_id,
    u.user_id
FROM menus m
CROSS JOIN (VALUES
    ('R01', 'READ'), ('R01', 'CREATE'), ('R01', 'UPDATE'),
    ('R02', 'READ'), ('R04', 'READ'),
    ('R07', 'READ'), ('R07', 'CREATE'), ('R07', 'EXECUTE'),
    ('R09', 'READ'), ('R09', 'CREATE'), ('R09', 'UPDATE'), ('R09', 'EXECUTE')
) AS permission(role_code, function_type)
CROSS JOIN users u
WHERE m.screen_id IN (
    'SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT',
    'SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT',
    'SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT',
    'SCR-EMPLOYMENT-RATE-ACHIEVEMENT'
)
  AND (permission.role_code <> 'R07' OR m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENT')
  AND u.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM function_permissions p
      WHERE p.screen_id = m.screen_id
        AND p.role_code = permission.role_code
        AND p.function_type = permission.function_type
  );
-- 기존 명시적 DENY를 변경하지 않는다.
SELECT setval(
    pg_get_serial_sequence('menus', 'menu_id'),
    (SELECT MAX(menu_id) FROM menus),
    true
);

INSERT INTO excel_upload_templates (
    template_id, business_type, template_version, effective_date, created_by, updated_by, change_reason
)
SELECT
    'EMPLOYMENT-RATE-TEMPLATE-001',
    'EMPLOYMENT_RATE_ACHIEVEMENT',
    'v1.0',
    DATE '2026-01-01',
    u.user_id,
    u.user_id,
    '취업률 실적 공통열 양식'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (business_type, template_version, effective_date) DO NOTHING;
INSERT INTO excel_upload_template_rules (
    rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by
)
SELECT
    'EMPLOYMENT-RATE-RULE-' || seed.column_order,
    t.template_id,
    seed.column_name,
    seed.column_order,
    seed.rule_ref,
    u.user_id,
    u.user_id
FROM excel_upload_templates t
CROSS JOIN users u
CROSS JOIN (VALUES
    (1, '교번', 'users.employee_no'),
    (2, '관리항목코드', 'evaluation_element_management_item_settings.management_item_code'),
    (3, '업적발생일', 'ISO_DATE'),
    (4, '실적명', 'OPTIONAL_TEXT'),
    (5, '첨부참조', 'OPTIONAL_OWNED_FILE_TOKEN')
) AS seed(column_order, column_name, rule_ref)
WHERE t.business_type = 'EMPLOYMENT_RATE_ACHIEVEMENT'
  AND t.template_version = 'v1.0'
  AND t.effective_date = DATE '2026-01-01'
  AND u.login_id = 'admin'
ON CONFLICT (template_id, column_order) DO NOTHING;
-- 실제 workbook은 Excel adapter가 생성한다. 존재하지 않는 file token/size는 seed하지 않는다.
