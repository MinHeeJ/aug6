ALTER TABLE area_element_system_settings
    ADD COLUMN IF NOT EXISTS item_id bigint;
ALTER TABLE area_element_system_settings
    ADD COLUMN IF NOT EXISTS element_id bigint;

COMMENT ON COLUMN area_element_system_settings.item_id IS 'evaluation_items.item_id 참조 의도 (영역별 평가항목 기준 연결)';
COMMENT ON COLUMN area_element_system_settings.element_id IS 'evaluation_elements.element_id 참조 의도 (영역별 평가요소 기준 연결)';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_area_element_system_settings_item'
    ) THEN
        ALTER TABLE area_element_system_settings
            ADD CONSTRAINT fk_area_element_system_settings_item FOREIGN KEY (item_id) REFERENCES evaluation_items(item_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_area_element_system_settings_element'
    ) THEN
        ALTER TABLE area_element_system_settings
            ADD CONSTRAINT fk_area_element_system_settings_element FOREIGN KEY (element_id) REFERENCES evaluation_elements(element_id);
    END IF;
END $$;

ALTER TABLE area_element_system_settings
    DROP CONSTRAINT IF EXISTS uq_area_element_system_settings_key;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_area_element_system_settings_scope'
    ) THEN
        ALTER TABLE area_element_system_settings
            ADD CONSTRAINT uq_area_element_system_settings_scope UNIQUE (area_id, item_id, element_id, target_scope);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_area_element_system_settings_element_active
    ON area_element_system_settings(element_id, active_yn);

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
VALUES ('ROLE', 'R04', 335, 'ALLOW', 'ACTIVE', 1, 1, '교수지원과 담당자 영역별 평가요소 체계 관리 메뉴 접근')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed,
                                                            status = EXCLUDED.status,
                                                            updated_at = CURRENT_TIMESTAMP,
                                                            updated_by = EXCLUDED.updated_by,
                                                            change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by)
SELECT 'SCR-AREA-ELEMENT-SYSTEM-MGMT', 'R04', function_type, 'ALLOW', '교수지원과 담당자 영역별 평가요소 체계 관리 기능 허용', 1, 1
FROM (VALUES ('READ'), ('CREATE'), ('UPDATE')) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET permission_allowed = EXCLUDED.permission_allowed,
                                                               change_reason = EXCLUDED.change_reason,
                                                               updated_at = CURRENT_TIMESTAMP,
                                                               updated_by = EXCLUDED.updated_by;
