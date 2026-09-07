-- BASIC-53 학교정보 조회 메뉴·실행정보·역할 권한 seed
-- 학교정보 조회 결과는 저장하지 않으며, 이 migration은 메뉴 seed만 추가한다.

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
    updated_by,
    change_reason
)
VALUES (
    1540,
    100,
    'MIDDLE',
    '외부 연동',
    9,
    NULL,
    NULL,
    'settings',
    'SYSTEM',
    '외부 공개 API 연동 검증 메뉴 그룹',
    'Y',
    'ACTIVE',
    1,
    'BASIC-53 학교정보 조회 메뉴 그룹 추가'
)
ON CONFLICT (menu_id) DO UPDATE SET
    parent_menu_id = EXCLUDED.parent_menu_id,
    menu_name = EXCLUDED.menu_name,
    display_order = EXCLUDED.display_order,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    system_use_yn = EXCLUDED.system_use_yn,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

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
    updated_by,
    change_reason
)
VALUES (
    1541,
    1540,
    'SCREEN',
    '학교정보 조회',
    1,
    'SCR-SCHOOL-INFO-LOOKUP',
    '/admin/school-info',
    'settings',
    'SYSTEM',
    'NEIS 학교기본정보 공개 API 조회 결과를 저장 없이 확인한다.',
    'Y',
    'ACTIVE',
    1,
    'BASIC-53 학교정보 조회 화면 추가'
)
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
    screen_id,
    url,
    icon,
    business_category,
    description,
    1
FROM menus
WHERE menu_id = 1541
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    status = 'ACTIVE',
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
    roles.role_code,
    menus.menu_id,
    'ALLOW',
    'ACTIVE',
    1,
    1,
    'BASIC-53 학교정보 조회 화면 R09 접근 허용'
FROM roles
JOIN menus ON menus.screen_id = 'SCR-SCHOOL-INFO-LOOKUP'
WHERE roles.role_code = 'R09'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;
