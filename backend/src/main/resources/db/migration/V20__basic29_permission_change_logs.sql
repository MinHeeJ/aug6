ALTER TABLE permission_change_history ADD COLUMN IF NOT EXISTS approver_user_id bigint;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_permission_change_history_approver') THEN
        ALTER TABLE permission_change_history ADD CONSTRAINT fk_permission_change_history_approver FOREIGN KEY (approver_user_id) REFERENCES users(user_id);
    END IF;
END $$;

COMMENT ON COLUMN permission_change_history.approver_user_id IS 'users.user_id 참조 의도 (권한 변경 승인자, 승인 절차가 없는 변경은 nullable)';
COMMENT ON COLUMN permission_change_history.changed_by IS 'users.user_id 참조 의도 (권한 변경 처리자)';
COMMENT ON COLUMN permission_change_history.reason IS '권한 변경 사유를 PermissionChangeHistoryMapper.insertPermissionChangeHistory 또는 승인 처리 서비스가 영구 보존';
COMMENT ON COLUMN permission_change_history.changed_at IS '권한 변경 처리일시를 데이터베이스 기본값 또는 승인 처리 서비스가 영구 보존';

CREATE INDEX IF NOT EXISTS idx_permission_change_history_approver ON permission_change_history(approver_user_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_permission_change_history_changed_at ON permission_change_history(changed_at DESC);

INSERT INTO permission_change_history (target_type, target_id, before_value, after_value, approver_user_id, changed_by, reason, changed_at)
SELECT 'FUNCTION', 'SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE',
       '{"permissionAllowed":"ALLOW"}'::jsonb,
       '{"permissionAllowed":"DENY"}'::jsonb,
       u.user_id,
       u.user_id,
       'SEED-PERMISSION-CHANGE-001 권한변경 로그 조회 smoke',
       CURRENT_TIMESTAMP - INTERVAL '6 minutes'
FROM users u
WHERE u.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM permission_change_history
      WHERE reason = 'SEED-PERMISSION-CHANGE-001 권한변경 로그 조회 smoke'
  );

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(227,224,'SCREEN','권한변경 로그',3,'SCR-PERMISSION-CHANGE-LOG','/admin/audit/permission-change-logs','history','SYSTEM','권한유형·변경대상·승인자·처리자별 권한변경 로그 조회','Y','ACTIVE',1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, parent_menu_id = EXCLUDED.parent_menu_id, url = EXCLUDED.url, screen_id = EXCLUDED.screen_id, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1 FROM menus WHERE screen_id = 'SCR-PERMISSION-CHANGE-LOG'
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-29 권한변경 로그 조회 권한'
FROM menus WHERE screen_id = 'SCR-PERMISSION-CHANGE-LOG'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;
