CREATE INDEX IF NOT EXISTS idx_session_termination_history_session_id ON session_termination_history(session_id);

COMMENT ON TABLE session_termination_history IS '로그아웃·유휴만료·절대만료·관리자 강제종료 원인을 장기 보존하는 조회 전용 이력 테이블.';
COMMENT ON COLUMN session_termination_history.termination_type IS 'LOGOUT:로그아웃|IDLE_TIMEOUT:유휴만료|ABSOLUTE_TIMEOUT:절대만료|ADMIN_TERMINATED:관리자강제종료';
COMMENT ON COLUMN session_termination_history.session_id IS 'sessions.session_id 참조 의도 (종료된 세션 식별자)';
COMMENT ON COLUMN session_termination_history.terminated_at IS '로그아웃·만료·강제종료 처리 시 애플리케이션에서 기록';
COMMENT ON COLUMN session_termination_history.termination_reason IS '로그아웃·만료·강제종료 처리 시 애플리케이션에서 기록';

INSERT INTO sessions (session_id, user_id, login_at, expires_at, status, last_accessed_at, ip_address)
SELECT 'SEED-SESSION-HISTORY-001', u.user_id, CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours', 'EXPIRED', CURRENT_TIMESTAMP - INTERVAL '2 hours', '127.0.0.1'
FROM users u
WHERE u.login_id = 'professor1'
ON CONFLICT (session_id) DO NOTHING;

INSERT INTO session_termination_history (session_id, termination_type, termination_reason, terminated_at)
SELECT 'SEED-SESSION-HISTORY-001', 'IDLE_TIMEOUT', '유휴시간 30분 초과로 자동 만료', CURRENT_TIMESTAMP - INTERVAL '2 hours'
WHERE EXISTS (SELECT 1 FROM sessions WHERE session_id = 'SEED-SESSION-HISTORY-001')
  AND NOT EXISTS (SELECT 1 FROM session_termination_history WHERE session_id = 'SEED-SESSION-HISTORY-001' AND termination_type = 'IDLE_TIMEOUT');

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(223,221,'SCREEN','로그아웃·만료 이력',2,'SCR-SESSION-TERMINATION-HISTORY','/admin/security/session-termination-histories','history','SYSTEM','세션 종료 유형과 사유 조회','Y','ACTIVE',1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, parent_menu_id = EXCLUDED.parent_menu_id, url = EXCLUDED.url, screen_id = EXCLUDED.screen_id, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1 FROM menus WHERE screen_id = 'SCR-SESSION-TERMINATION-HISTORY'
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-29 세션 종료 이력 조회 권한'
FROM menus WHERE screen_id = 'SCR-SESSION-TERMINATION-HISTORY'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;
