-- 신규 화면을 기존 메뉴·기능 권한·Excel 템플릿 원장에 추가한다. 기존 DENY/사용자 우선 정책은 유지한다.
LOCK TABLE menus IN SHARE ROW EXCLUSIVE MODE;
WITH next_menu AS (
    SELECT COALESCE(MAX(m.menu_id), 0) AS maximum_menu_id
    FROM menus m
), requested_menus AS (
    SELECT s.*
    FROM (
        VALUES
            (1, '취업률 제고 실적 관리', 'SCR-EMPLOYMENT-RATE-IMPROVEMENTS',
                '/faculty/education/employment-rate-improvements', 'trending-up'),
            (2, '강좌 개설·운영 실적 관리', 'SCR-COURSE-OPERATIONS',
                '/faculty/education/course-operations', 'book-open'),
            (3, '강의개선 실적 관리', 'SCR-LECTURE-IMPROVEMENTS',
                '/faculty/education/lecture-improvements', 'clipboard-check'),
            (4, '취업률 실적 관리', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS',
                '/faculty/education/employment-rate-achievements', 'file-spreadsheet')
    ) AS s(menu_order, menu_name, screen_id, url, icon)
), missing_menus AS (
    SELECT
        s.*,
        ROW_NUMBER() OVER (ORDER BY s.menu_order) AS missing_sequence
    FROM requested_menus s
    WHERE NOT EXISTS (
        SELECT 1
        FROM menus m
        WHERE m.screen_id = s.screen_id OR m.url = s.url
    )
)
INSERT INTO menus (
    menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon,
    business_category, description, system_use_yn, status, updated_by, change_reason
)
SELECT
    n.maximum_menu_id + s.missing_sequence,
    NULL,
    'SCREEN',
    s.menu_name,
    1100 + s.menu_order,
    s.screen_id,
    s.url,
    s.icon,
    'EDUCATION_ACHIEVEMENT',
    '교육영역 후반 실적 화면 (기존 세션·메뉴·업무별 역할 경계 적용)',
    'Y',
    'ACTIVE',
    u.user_id,
    '교육영역 후반 신규 메뉴'
FROM missing_menus s
CROSS JOIN next_menu n
CROSS JOIN users u
WHERE u.login_id = 'admin';

-- 명시 menu_id 삽입 이후 정상 identity 등록이 충돌하지 않도록 sequence를 맞춘다.
SELECT setval(
    pg_get_serial_sequence('menus', 'menu_id'),
    COALESCE((SELECT MAX(m.menu_id) FROM menus m), 1),
    true
);

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
    'SCR-EMPLOYMENT-RATE-IMPROVEMENTS', 'SCR-COURSE-OPERATIONS',
    'SCR-LECTURE-IMPROVEMENTS', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS'
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
    '교육영역 후반 계약 역할 메뉴 접근 (R07은 취업률 Excel/일괄 화면만)'
FROM menus m
CROSS JOIN roles r
CROSS JOIN users u
WHERE u.login_id = 'admin'
  AND m.screen_id IN (
      'SCR-EMPLOYMENT-RATE-IMPROVEMENTS', 'SCR-COURSE-OPERATIONS',
      'SCR-LECTURE-IMPROVEMENTS', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS'
  )
  AND (
      r.role_code IN ('R01', 'R02', 'R04')
      OR (r.role_code = 'R07' AND m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS')
  )
ON CONFLICT (target_type, target_id, menu_id) DO NOTHING;

INSERT INTO function_permissions (
    screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by
)
SELECT
    m.screen_id,
    r.role_code,
    f.function_type,
    'ALLOW',
    '교육영역 후반 계약 역할 기능 접근 (단건 쓰기 R01, Excel/일괄 R07)',
    u.user_id,
    u.user_id
FROM menus m
CROSS JOIN roles r
CROSS JOIN (VALUES ('READ'), ('CREATE'), ('UPDATE'), ('EXECUTE')) AS f(function_type)
CROSS JOIN users u
WHERE u.login_id = 'admin'
  AND m.screen_id IN (
      'SCR-EMPLOYMENT-RATE-IMPROVEMENTS', 'SCR-COURSE-OPERATIONS',
      'SCR-LECTURE-IMPROVEMENTS', 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS'
  )
  AND (
      (r.role_code IN ('R01', 'R02', 'R04') AND f.function_type = 'READ')
      OR (r.role_code = 'R01' AND f.function_type IN ('CREATE', 'UPDATE'))
      OR (
          r.role_code = 'R07' AND m.screen_id = 'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS'
          AND f.function_type IN ('READ', 'EXECUTE')
      )
  )
ON CONFLICT (screen_id, role_code, function_type) DO NOTHING;

-- 파일 metadata에 존재하지 않는 XLSX token을 넣지 않는다. 업무 서비스가 규칙으로 실제 양식을 생성한다.
INSERT INTO excel_upload_templates (
    template_id, business_type, template_version, effective_date, system_use_yn, status,
    created_by, updated_by, change_reason
)
SELECT
    'EMPLOYMENT-RATE-ACHIEVEMENT-v1',
    'EMPLOYMENT_RATE_ACHIEVEMENT',
    'v1.0',
    DATE '2026-01-01',
    'Y',
    'ACTIVE',
    u.user_id,
    u.user_id,
    '취업률 실적 공통열 양식; 검증 결과 확인 후 원자 commit'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO excel_upload_template_rules (
    rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by
)
SELECT
    CONCAT('EMPLOYMENT-RATE-ACHIEVEMENT-RULE-', s.column_order),
    t.template_id,
    s.required_column,
    s.column_order,
    s.code_rule_ref,
    u.user_id,
    u.user_id
FROM excel_upload_templates t
CROSS JOIN users u
CROSS JOIN (
    VALUES
        (1, '교번', 'users.employee_no'),
        (2, '관리항목코드', 'evaluation_management_items.management_item_code'),
        (3, '업적발생일', 'ISO_LOCAL_DATE'),
        (4, '실적명', 'TEXT'),
        (5, '첨부참조', 'FILE_REFERENCE_OPTIONAL')
) AS s(column_order, required_column, code_rule_ref)
WHERE t.template_id = 'EMPLOYMENT-RATE-ACHIEVEMENT-v1' AND u.login_id = 'admin'
ON CONFLICT (rule_id) DO NOTHING;
