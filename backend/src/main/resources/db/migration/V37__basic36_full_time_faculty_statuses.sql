ALTER TABLE korus_personnel_snapshots ADD COLUMN IF NOT EXISTS snapshot_year integer;
UPDATE korus_personnel_snapshots
SET snapshot_year = EXTRACT(YEAR FROM last_synced_at)::integer
WHERE snapshot_year IS NULL;
ALTER TABLE korus_personnel_snapshots ALTER COLUMN snapshot_year SET NOT NULL;
ALTER TABLE korus_personnel_snapshots ALTER COLUMN snapshot_year SET DEFAULT EXTRACT(YEAR FROM CURRENT_DATE)::integer;

COMMENT ON COLUMN korus_personnel_snapshots.snapshot_year IS 'FullTimeFacultyStatusService.list 기준연도 조회 시 KORUS snapshot 유효연도로 애플리케이션에서 사용';

CREATE INDEX IF NOT EXISTS idx_korus_personnel_snapshots_snapshot_year ON korus_personnel_snapshots(snapshot_year);
CREATE INDEX IF NOT EXISTS idx_korus_personnel_snapshots_year_org ON korus_personnel_snapshots(snapshot_year, organization_code);
CREATE INDEX IF NOT EXISTS idx_korus_personnel_snapshots_year_employee ON korus_personnel_snapshots(snapshot_year, employee_no);

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
VALUES
(3620, 3600, 'MIDDLE', '기준정보 조회', 2, NULL, NULL, 'search', 'BASIC36', '교수업적평가 기준정보 조회', 'Y', 'ACTIVE', 1),
(3621, 3620, 'SCREEN', '전임교원 현황', 1, 'SCR-FULL-TIME-FACULTY-STATUS', '/admin/full-time-faculty-statuses', 'users', 'BASIC36', '기준연도·소속 조건별 전임교원 교번·성명·소속·직급·퇴직일자 현황 조회', 'Y', 'ACTIVE', 1)
ON CONFLICT (menu_id) DO UPDATE SET parent_menu_id = EXCLUDED.parent_menu_id, menu_name = EXCLUDED.menu_name, screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, description = EXCLUDED.description, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1
FROM menus
WHERE screen_id = 'SCR-FULL-TIME-FACULTY-STATUS'
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, icon = EXCLUDED.icon, business_category = EXCLUDED.business_category, description = EXCLUDED.description, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_code, 3621, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-36 전임교원 현황 조회 접근'
FROM roles
WHERE role_code IN ('R03','R04','R09')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;
