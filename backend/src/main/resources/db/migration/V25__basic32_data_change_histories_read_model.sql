CREATE INDEX IF NOT EXISTS idx_data_change_histories_change_type ON data_change_histories(change_type, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_data_change_histories_target_key ON data_change_histories(target_key, changed_at DESC);

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
VALUES (216, 200, 'MIDDLE', '데이터 이력 관리', 6, NULL, NULL, 'history', 'FILE_DATA', '업무 데이터 변경·삭제 이력 조회 관리', 'Y', 'ACTIVE', 1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name,
                                      parent_menu_id = EXCLUDED.parent_menu_id,
                                      display_order = EXCLUDED.display_order,
                                      icon = EXCLUDED.icon,
                                      business_category = EXCLUDED.business_category,
                                      description = EXCLUDED.description,
                                      system_use_yn = EXCLUDED.system_use_yn,
                                      status = EXCLUDED.status,
                                      updated_at = CURRENT_TIMESTAMP,
                                      updated_by = EXCLUDED.updated_by;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
VALUES (325, 216, 'SCREEN', '데이터 변경 이력', 1, 'SCR-DATA-CHANGE-HISTORY', '/admin/data-change-histories', 'history', 'FILE_DATA', '업무 운영 데이터 등록·수정·삭제 이력을 필드 단위 전후값으로 조회', 'Y', 'ACTIVE', 1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name,
                                      parent_menu_id = EXCLUDED.parent_menu_id,
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

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
VALUES (325, 'SCR-DATA-CHANGE-HISTORY', '/admin/data-change-histories', 'history', 'FILE_DATA', '업무 운영 데이터 등록·수정·삭제 이력을 필드 단위 전후값으로 조회', 1)
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id,
                                      url = EXCLUDED.url,
                                      icon = EXCLUDED.icon,
                                      business_category = EXCLUDED.business_category,
                                      description = EXCLUDED.description,
                                      updated_at = CURRENT_TIMESTAMP,
                                      updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
VALUES ('ROLE', 'R09', 325, 'ALLOW', 'ACTIVE', 1, 1, '시스템관리자 데이터 변경 이력 메뉴 접근')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed,
                                                            status = EXCLUDED.status,
                                                            updated_at = CURRENT_TIMESTAMP,
                                                            updated_by = EXCLUDED.updated_by,
                                                            change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by)
VALUES ('SCR-DATA-CHANGE-HISTORY', 'R09', 'READ', 'ALLOW', '시스템관리자 데이터 변경 이력 조회 기능 기본 허용', 1, 1)
ON CONFLICT (screen_id, role_code, function_type) DO NOTHING;
