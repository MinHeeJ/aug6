-- BASIC-79 강의평가 실적 관리 화면의 메뉴 접근과 기능 권한을 현재 메뉴 ID 범위와 충돌 없이 등록한다.
WITH new_menu AS (
    INSERT INTO menus (
        menu_id,
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
        updated_by
    )
    SELECT
        COALESCE(MAX(menu_id), 0) + 1,
        NULL,
        'SCREEN',
        '강의평가 실적 관리',
        1,
        'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
        '/achievements/education/lecture-evaluations',
        'graduation-cap',
        'BUSINESS',
        '교육영역 강의평가 실적의 조회·입력·제출을 제공한다.',
        'Y',
        'ACTIVE',
        1
    FROM menus
    WHERE NOT EXISTS (
        SELECT 1
        FROM menus
        WHERE url = '/achievements/education/lecture-evaluations'
    )
    RETURNING menu_id
),
existing_menu AS (
    SELECT menu_id
    FROM new_menu
    UNION ALL
    SELECT menu_id
    FROM menus
    WHERE url = '/achievements/education/lecture-evaluations'
      AND NOT EXISTS (SELECT 1 FROM new_menu)
)
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
    menu_id,
    'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
    '/achievements/education/lecture-evaluations',
    'graduation-cap',
    'BUSINESS',
    '교육영역 강의평가 실적의 조회·입력·제출을 제공한다.',
    1
FROM existing_menu
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
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
    role_seed.role_code,
    menu.menu_id,
    'ALLOW',
    'ACTIVE',
    1,
    1,
    'BASIC-79 강의평가 실적 관리 메뉴 접근'
FROM menus menu
CROSS JOIN (VALUES ('R01'), ('R02'), ('R04')) AS role_seed(role_code)
WHERE menu.url = '/achievements/education/lecture-evaluations'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (
    screen_id,
    role_code,
    function_type,
    permission_allowed,
    change_reason,
    created_by,
    updated_by
)
SELECT
    'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
    role_seed.role_code,
    function_seed.function_type,
    'ALLOW',
    'BASIC-79 강의평가 실적 관리 기능 접근',
    1,
    1
FROM (VALUES ('R01'), ('R02'), ('R04')) AS role_seed(role_code)
CROSS JOIN (VALUES ('READ'), ('CREATE'), ('UPDATE')) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET
    permission_allowed = EXCLUDED.permission_allowed,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;
