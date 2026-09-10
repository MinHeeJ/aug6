CREATE TABLE IF NOT EXISTS basic59_phase1_foundation_reservations (
    screen_id VARCHAR(100) PRIMARY KEY,
    route_path VARCHAR(200) NOT NULL,
    api_prefix VARCHAR(200) NOT NULL,
    reserved_menu_id BIGINT NOT NULL UNIQUE,
    basis_note VARCHAR(500) NOT NULL,
    review_checklist VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE basic59_phase1_foundation_reservations IS 'BASIC-59 Phase 1 선행조건으로 기존 저장소·인증·메뉴 채번·migration 경계를 기록하고 네 leaf menu id를 예약한다.';
COMMENT ON COLUMN basic59_phase1_foundation_reservations.screen_id IS 'BASIC-59 화면 식별자.';
COMMENT ON COLUMN basic59_phase1_foundation_reservations.route_path IS '기존 React shell에 추후 연결할 UI route.';
COMMENT ON COLUMN basic59_phase1_foundation_reservations.api_prefix IS '기존 SessionCookie/Principal/권한 필터를 재사용할 API prefix.';
COMMENT ON COLUMN basic59_phase1_foundation_reservations.reserved_menu_id IS '기존 menus 최대 menu_id 564 이후 충돌 방지를 위해 예약한 연속 leaf menu id.';
COMMENT ON COLUMN basic59_phase1_foundation_reservations.basis_note IS 'T001~T005 확인 근거와 예약 기준.';
COMMENT ON COLUMN basic59_phase1_foundation_reservations.review_checklist IS 'T006 금지 경계 체크리스트.';

INSERT INTO basic59_phase1_foundation_reservations (screen_id, route_path, api_prefix, reserved_menu_id, basis_note, review_checklist) VALUES
('SCR-EVALUATION-ELEMENT-MGMT-ITEMS', '/admin/evaluation-element-management-items', '/api/business/evaluation-element-management-items', 565, 'T001 backend/frontend/infra/docker-compose.yml/PostgreSQL 확인; T003 기존 V55 menu maxExistingMenuId=564; T005 V56 신규 migration 예약', 'no-new-auth|no-new-user-table|no-new-org-table|no-new-role-table|no-second-compose'),
('SCR-PARTICIPATION-RATE-OPERATION', '/admin/participation-rate-operation-settings', '/api/business/participation-rate-operation-settings', 566, 'T001 backend/frontend/infra/docker-compose.yml/PostgreSQL 확인; T003 기존 V55 menu maxExistingMenuId=564; T005 V56 신규 migration 예약', 'no-new-auth|no-new-user-table|no-new-org-table|no-new-role-table|no-second-compose'),
('SCR-MANAGEMENT-ITEM-EVAL-SCORES', '/admin/management-item-evaluation-scores', '/api/business/management-item-evaluation-scores', 567, 'T001 backend/frontend/infra/docker-compose.yml/PostgreSQL 확인; T003 기존 V55 menu maxExistingMenuId=564; T005 V56 신규 migration 예약', 'no-new-auth|no-new-user-table|no-new-org-table|no-new-role-table|no-second-compose'),
('SCR-COURSE-AREA-GROUP-GRADES', '/evaluation/course-area-group-grades', '/api/business/course-area-group-grades', 568, 'T001 backend/frontend/infra/docker-compose.yml/PostgreSQL 확인; T003 기존 V55 menu maxExistingMenuId=564; T005 V56 신규 migration 예약', 'no-new-auth|no-new-user-table|no-new-org-table|no-new-role-table|no-second-compose')
ON CONFLICT (screen_id) DO UPDATE SET
    route_path = EXCLUDED.route_path,
    api_prefix = EXCLUDED.api_prefix,
    reserved_menu_id = EXCLUDED.reserved_menu_id,
    basis_note = EXCLUDED.basis_note,
    review_checklist = EXCLUDED.review_checklist;
