INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by, change_reason)
VALUES (160, 100, 'MIDDLE', '개인정보 관리', 6, NULL, NULL, 'shield', 'SYSTEM', '개인정보 항목·조회권한·처리이력 관리', 'Y', 'ACTIVE', 1, 'BASIC-19 메뉴 구조 변경')
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name,
                                      parent_menu_id = EXCLUDED.parent_menu_id,
                                      menu_type = EXCLUDED.menu_type,
                                      display_order = EXCLUDED.display_order,
                                      screen_id = EXCLUDED.screen_id,
                                      url = EXCLUDED.url,
                                      icon = EXCLUDED.icon,
                                      business_category = EXCLUDED.business_category,
                                      description = EXCLUDED.description,
                                      system_use_yn = EXCLUDED.system_use_yn,
                                      status = EXCLUDED.status,
                                      updated_by = EXCLUDED.updated_by,
                                      change_reason = EXCLUDED.change_reason,
                                      updated_at = CURRENT_TIMESTAMP;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by, change_reason)
VALUES (161, 160, 'SCREEN', '개인정보 처리이력', 3, 'SCR-PRIVACY-ACCESS-LOG', '/admin/privacy/access-logs', 'shield', 'SYSTEM', '개인정보 조회·출력·다운로드 처리이력 조회', 'Y', 'ACTIVE', 1, 'BASIC-19 메뉴 구조 변경')
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name,
                                      parent_menu_id = EXCLUDED.parent_menu_id,
                                      menu_type = EXCLUDED.menu_type,
                                      display_order = EXCLUDED.display_order,
                                      screen_id = EXCLUDED.screen_id,
                                      url = EXCLUDED.url,
                                      icon = EXCLUDED.icon,
                                      business_category = EXCLUDED.business_category,
                                      description = EXCLUDED.description,
                                      system_use_yn = EXCLUDED.system_use_yn,
                                      status = EXCLUDED.status,
                                      updated_by = EXCLUDED.updated_by,
                                      change_reason = EXCLUDED.change_reason,
                                      updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT target_type, target_id, 161, access_allowed, status, created_by, 1, 'BASIC-19 개인정보 처리이력 메뉴 ID 이관'
FROM menu_permissions
WHERE menu_id = 130
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed,
                                                            status = EXCLUDED.status,
                                                            updated_at = CURRENT_TIMESTAMP,
                                                            updated_by = EXCLUDED.updated_by,
                                                            change_reason = EXCLUDED.change_reason;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by, status)
VALUES (161, 'SCR-PRIVACY-ACCESS-LOG', '/admin/privacy/access-logs', 'shield', 'SYSTEM', '개인정보 조회·출력·다운로드 처리이력 조회', 1, 'ACTIVE')
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id,
                                      url = EXCLUDED.url,
                                      icon = EXCLUDED.icon,
                                      business_category = EXCLUDED.business_category,
                                      description = EXCLUDED.description,
                                      status = EXCLUDED.status,
                                      updated_by = EXCLUDED.updated_by,
                                      updated_at = CURRENT_TIMESTAMP;

DELETE FROM menu_permissions WHERE menu_id = 130;
DELETE FROM menu_execution_info WHERE menu_id = 130;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by, change_reason)
VALUES (130, 100, 'MIDDLE', '메뉴 관리', 3, NULL, NULL, 'menu', 'SYSTEM', '메뉴 구조와 정보 관리', 'Y', 'ACTIVE', 1, 'BASIC-19 메뉴 구조 변경')
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name,
                                      parent_menu_id = EXCLUDED.parent_menu_id,
                                      menu_type = EXCLUDED.menu_type,
                                      display_order = EXCLUDED.display_order,
                                      screen_id = EXCLUDED.screen_id,
                                      url = EXCLUDED.url,
                                      icon = EXCLUDED.icon,
                                      business_category = EXCLUDED.business_category,
                                      description = EXCLUDED.description,
                                      system_use_yn = EXCLUDED.system_use_yn,
                                      status = EXCLUDED.status,
                                      updated_by = EXCLUDED.updated_by,
                                      change_reason = EXCLUDED.change_reason,
                                      updated_at = CURRENT_TIMESTAMP;

UPDATE menus
SET parent_menu_id = CASE menu_id
        WHEN 131 THEN 120
        WHEN 132 THEN 120
        WHEN 151 THEN 120
        ELSE parent_menu_id
    END,
    display_order = CASE menu_id
        WHEN 131 THEN 8
        WHEN 132 THEN 9
        WHEN 151 THEN 10
        ELSE display_order
    END,
    business_category = 'SYSTEM',
    updated_by = 1,
    change_reason = 'BASIC-19 메뉴 구조 변경',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_id IN (131, 132, 151);

UPDATE menus
SET parent_menu_id = CASE menu_id
        WHEN 128 THEN 160
        WHEN 129 THEN 160
        ELSE parent_menu_id
    END,
    display_order = CASE menu_id
        WHEN 128 THEN 1
        WHEN 129 THEN 2
        ELSE display_order
    END,
    business_category = 'SYSTEM',
    updated_by = 1,
    change_reason = 'BASIC-19 메뉴 구조 변경',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_id IN (128, 129);

UPDATE menu_execution_info
SET business_category = 'SYSTEM',
    updated_by = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_id IN (128, 129, 161);

SELECT setval(pg_get_serial_sequence('menus', 'menu_id'), COALESCE((SELECT MAX(menu_id) FROM menus), 1), true);
