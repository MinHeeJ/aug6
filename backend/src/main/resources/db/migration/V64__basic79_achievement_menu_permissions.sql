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
    seed.menu_name,
    seed.display_order,
    seed.screen_id,
    seed.url,
    'book-open',
    'ACHIEVEMENT',
    seed.description,
    'Y',
    'ACTIVE',
    administrator.user_id,
    'BASIC-79 교육영역 실적 화면 메뉴'
FROM users administrator
CROSS JOIN (
    VALUES
        (2, 'SCR-LECTURE-ACHIEVEMENT-MGMT', '/achievements/education/lecture-achievements', '강의실적 관리', '교육영역 강의실적의 검색과 저장을 제공한다.'),
        (3, 'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT', '/achievements/education/degree-completion-achievements', '석·박사 배출 실적 관리', '교육영역 석·박사 배출 실적의 검색과 저장을 제공한다.'),
        (4, 'SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD', '/achievements/education/student-guidance-achievements', '학생지도 실적 관리', '교육영역 학생지도 실적의 검색과 저장을 제공한다.')
) AS seed(display_order, screen_id, url, menu_name, description)
WHERE administrator.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM menus existing_menu
      WHERE existing_menu.screen_id = seed.screen_id
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
WHERE menu.screen_id IN (
    'SCR-LECTURE-ACHIEVEMENT-MGMT',
    'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT',
    'SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD'
)
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
    'BASIC-79 교육영역 실적 역할 접근'
FROM (VALUES ('R01'), ('R02'), ('R04')) AS roles(role_code)
JOIN menus menu
    ON menu.screen_id IN (
        'SCR-LECTURE-ACHIEVEMENT-MGMT',
        'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT',
        'SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD'
    )
JOIN users administrator ON administrator.login_id = 'admin'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;
