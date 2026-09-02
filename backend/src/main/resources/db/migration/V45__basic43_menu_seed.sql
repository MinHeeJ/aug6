-- BASIC-43 신규 화면 메뉴·실행정보·역할/기능 권한 seed
-- V44는 이미 적용된 migration이므로 기존 파일을 수정하지 않고 증분 migration으로 등록한다.

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
VALUES (
    3650,
    300,
    'MIDDLE',
    '확인·승인 관리',
    6,
    NULL,
    NULL,
    'badge-check',
    'BUSINESS',
    'BASIC-43 업적 확인·인증·지급승인 화면 관리',
    'Y',
    'ACTIVE',
    1
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
    updated_by = EXCLUDED.updated_by;

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
VALUES
    (3651, 3650, 'SCREEN', '학과장 확인 관리', 1, 'SCR-DEPARTMENT-CHAIR-CONFIRM-MGMT', '/admin/department-chair-confirmations', 'clipboard-check', 'BUSINESS', '학과장 확인기간의 소속 학과 업적 확인·미승인 처리', 'Y', 'ACTIVE', 1),
    (3652, 3650, 'SCREEN', '담당자 인증 관리', 2, 'SCR-ACHIEVEMENT-VERIFICATION-MGMT', '/admin/achievement-verifications', 'badge-check', 'BUSINESS', '학과장 확인 실적 인증·인증반려·인증취소 처리', 'Y', 'ACTIVE', 1),
    (3653, 3650, 'SCREEN', '지급승인 관리', 3, 'SCR-GRANT-PAYMENT-APPROVAL-MGMT', '/admin/grant-payment-approvals', 'banknote', 'BUSINESS', '연구비 지급대상 조회·지급승인·승인취소 처리', 'Y', 'ACTIVE', 1),
    (3654, 312, 'SCREEN', '이의신청 의견 관리', 4, 'SCR-OBJECTION-OPINION-MGMT', '/admin/objection-opinions', 'message-square-text', 'BUSINESS', '이의신청 내용·의견 조회와 검토의견·결정 처리', 'Y', 'ACTIVE', 1)
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
    updated_by = EXCLUDED.updated_by;

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
WHERE menu_id BETWEEN 3651 AND 3654
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
    'R09',
    menu_id,
    'ALLOW',
    'ACTIVE',
    1,
    1,
    'BASIC-43 신규 확인·인증·승인 메뉴 접근'
FROM (VALUES (3651), (3652), (3653), (3654)) AS menu_seed(menu_id)
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
    screen_seed.screen_id,
    'R09',
    function_seed.function_type,
    'ALLOW',
    'BASIC-43 신규 확인·인증·승인 기능 기본 허용',
    1,
    1
FROM (
    VALUES
        ('SCR-DEPARTMENT-CHAIR-CONFIRM-MGMT'),
        ('SCR-ACHIEVEMENT-VERIFICATION-MGMT'),
        ('SCR-GRANT-PAYMENT-APPROVAL-MGMT'),
        ('SCR-OBJECTION-OPINION-MGMT')
) AS screen_seed(screen_id)
CROSS JOIN (
    VALUES ('READ'), ('CREATE'), ('UPDATE')
) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET
    permission_allowed = EXCLUDED.permission_allowed,
    updated_at = CURRENT_TIMESTAMP,
    change_reason = EXCLUDED.change_reason,
    updated_by = EXCLUDED.updated_by;
