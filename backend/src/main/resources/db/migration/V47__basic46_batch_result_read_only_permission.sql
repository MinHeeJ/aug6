INSERT INTO function_permissions (
    screen_id,
    role_code,
    function_type,
    permission_allowed,
    change_reason,
    created_by,
    updated_by
)
VALUES
    ('SCR-EVAL-BATCH-RESULT', 'R09', 'READ', 'ALLOW', 'BASIC-46 처리 결과 조회 화면은 조회 전용', 1, 1),
    ('SCR-EVAL-BATCH-RESULT', 'R09', 'EXECUTE', 'DENY', 'BASIC-46 처리 결과 조회 화면에서 일괄작업 실행·재실행 금지', 1, 1)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET
    permission_allowed = EXCLUDED.permission_allowed,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;
