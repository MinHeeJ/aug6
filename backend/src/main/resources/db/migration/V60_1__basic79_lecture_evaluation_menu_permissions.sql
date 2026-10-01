INSERT INTO menus (
    parent_menu_id,
    menu_type,
    menu_name,
    display_order,
    screen_id,
    url,
    icon,
    business_category,
    description,
    system_use_yn,
    status,
    updated_by,
    change_reason
)
SELECT
    NULL,
    'SCREEN',
    '강의평가 실적 관리',
    1,
    'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
    '/achievements/education/lecture-evaluations',
    'book-open',
    'ACHIEVEMENT',
    '교육영역 강의평가 실적의 검색과 저장을 제공한다.',
    'Y',
    'ACTIVE',
    administrator.user_id,
    'BASIC-79 강의평가 실적 화면 메뉴'
FROM users administrator
WHERE administrator.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM menus existing_menu
      WHERE existing_menu.screen_id = 'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT'
  );

INSERT INTO menu_execution_info (
    menu_id,
    screen_id,
    url,
    icon,
    business_category,
    description,
    updated_by
)
SELECT
    menu.menu_id,
    menu.screen_id,
    menu.url,
    menu.icon,
    menu.business_category,
    menu.description,
    administrator.user_id
FROM menus menu
JOIN users administrator ON administrator.login_id = 'admin'
WHERE menu.screen_id = 'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT'
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (
    target_type,
    target_id,
    menu_id,
    access_allowed,
    status,
    created_by,
    updated_by,
    change_reason
)
SELECT
    'ROLE',
    role_code,
    menu.menu_id,
    'ALLOW',
    'ACTIVE',
    administrator.user_id,
    administrator.user_id,
    'BASIC-79 강의평가 실적 역할 접근'
FROM (VALUES ('R01'), ('R02'), ('R04')) AS roles(role_code)
JOIN menus menu ON menu.screen_id = 'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT'
JOIN users administrator ON administrator.login_id = 'admin'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;
