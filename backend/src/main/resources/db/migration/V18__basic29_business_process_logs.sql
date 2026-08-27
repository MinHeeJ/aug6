COMMENT ON TABLE business_process_audit_logs IS '등록·수정·삭제·확인·인증·승인·취소·일괄처리와 세션 강제종료 처리 결과를 보존하는 불변 감사로그.';
COMMENT ON COLUMN business_process_audit_logs.action_type IS 'CREATE:등록|UPDATE:수정|DELETE:삭제|CONFIRM:확인|AUTH:인증|APPROVE:승인|CANCEL:취소|BATCH:일괄처리|SESSION_TERMINATE:세션강제종료';
COMMENT ON COLUMN business_process_audit_logs.result_status IS 'SUCCESS:성공|FAILURE:실패';
COMMENT ON COLUMN business_process_audit_logs.actor_user_id IS 'users.user_id 참조 의도 (업무처리 로그 처리자)';
COMMENT ON COLUMN business_process_audit_logs.request_id IS '요청 식별자 전 구간 추적을 위해 애플리케이션에서 기록';
COMMENT ON COLUMN business_process_audit_logs.before_state IS '보호대상 원문값은 마스킹 후 업무 서비스에서 저장';
COMMENT ON COLUMN business_process_audit_logs.after_state IS '보호대상 원문값은 마스킹 후 업무 서비스에서 저장';

CREATE INDEX IF NOT EXISTS idx_business_process_audit_logs_actor ON business_process_audit_logs(actor_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_business_process_audit_logs_target_key ON business_process_audit_logs(target_key);

INSERT INTO business_process_audit_logs (action_type, target_key, before_state, after_state, actor_user_id, result_status, request_id, created_at)
SELECT 'UPDATE', 'SEED-BUSINESS-AUDIT-001', '{"status":"BEFORE"}'::jsonb, '{"status":"AFTER"}'::jsonb, u.user_id, 'SUCCESS', 'REQ-BUSINESS-AUDIT-SEED', CURRENT_TIMESTAMP - INTERVAL '20 minutes'
FROM users u
WHERE u.login_id = 'admin'
  AND NOT EXISTS (SELECT 1 FROM business_process_audit_logs WHERE target_key = 'SEED-BUSINESS-AUDIT-001');

INSERT INTO business_process_audit_logs (action_type, target_key, before_state, after_state, actor_user_id, result_status, request_id, created_at)
SELECT 'BATCH', 'SEED-BUSINESS-AUDIT-FAILURE', '{"step":"검증"}'::jsonb, '{"error":"마스킹된 오류"}'::jsonb, u.user_id, 'FAILURE', 'REQ-BUSINESS-AUDIT-FAILURE', CURRENT_TIMESTAMP - INTERVAL '15 minutes'
FROM users u
WHERE u.login_id = 'admin'
  AND NOT EXISTS (SELECT 1 FROM business_process_audit_logs WHERE target_key = 'SEED-BUSINESS-AUDIT-FAILURE');

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(224,220,'MIDDLE','감사로그 관리',2,NULL,NULL,'clipboard-list','SYSTEM','업무처리 및 중요정보 감사 로그 관리','Y','ACTIVE',1),
(225,224,'SCREEN','업무처리 로그',1,'SCR-BUSINESS-PROCESS-LOG','/admin/audit/business-process-logs','list-checks','SYSTEM','업무처리 행위별 전후상태와 처리결과 조회','Y','ACTIVE',1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, parent_menu_id = EXCLUDED.parent_menu_id, url = EXCLUDED.url, screen_id = EXCLUDED.screen_id, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1 FROM menus WHERE screen_id = 'SCR-BUSINESS-PROCESS-LOG'
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-29 업무처리 로그 조회 권한'
FROM menus WHERE screen_id = 'SCR-BUSINESS-PROCESS-LOG'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;
