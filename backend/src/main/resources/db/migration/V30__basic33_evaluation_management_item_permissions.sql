INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
VALUES ('ROLE', 'R04', 334, 'ALLOW', 'ACTIVE', 1, 1, '교수지원과 담당자 관리항목 관리 메뉴 접근')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed,
                                                            status = EXCLUDED.status,
                                                            updated_at = CURRENT_TIMESTAMP,
                                                            updated_by = EXCLUDED.updated_by,
                                                            change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by)
SELECT 'SCR-EVALUATION-MANAGEMENT-ITEM-MGMT', 'R04', function_type, 'ALLOW', '교수지원과 담당자 관리항목 관리 기능 허용', 1, 1
FROM (VALUES ('READ'), ('CREATE'), ('UPDATE')) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET permission_allowed = EXCLUDED.permission_allowed,
                                                               change_reason = EXCLUDED.change_reason,
                                                               updated_at = CURRENT_TIMESTAMP,
                                                               updated_by = EXCLUDED.updated_by;
