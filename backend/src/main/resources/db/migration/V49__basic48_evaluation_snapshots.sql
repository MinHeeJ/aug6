CREATE TABLE IF NOT EXISTS biz_eval_snapshots (
    snapshot_id varchar(100) PRIMARY KEY,
    evaluation_year varchar(4) NOT NULL,
    finalization_point varchar(100) NOT NULL,
    organization_code varchar(50) NOT NULL,
    target_user_id bigint NOT NULL,
    rule_snapshot_ref varchar(300) NOT NULL,
    material_snapshot_ref varchar(300) NOT NULL,
    preserved_result_ref varchar(300) NOT NULL,
    rule_snapshot_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    material_snapshot_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    preserved_result_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    snapshot_status varchar(30) NOT NULL DEFAULT 'PRESERVED',
    finalization_id bigint,
    captured_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_id varchar(100) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT ck_biz_eval_snapshots_year CHECK (evaluation_year ~ '^[0-9]{4}$'),
    CONSTRAINT ck_biz_eval_snapshots_status CHECK (snapshot_status IN ('PRESERVED','CANCELLED','RECONFIRMED')),
    CONSTRAINT fk_biz_eval_snapshots_organization FOREIGN KEY (organization_code) REFERENCES organizations(organization_code),
    CONSTRAINT fk_biz_eval_snapshots_target_user FOREIGN KEY (target_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_biz_eval_snapshots_finalization FOREIGN KEY (finalization_id) REFERENCES evaluation_finalizations(finalization_id),
    CONSTRAINT fk_biz_eval_snapshots_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_biz_eval_snapshots_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE biz_eval_snapshots IS '평가확정 시점의 기준정보, 평가자료, 보존 결과를 조회 전용으로 보관하는 BASIC-48 시점 데이터 snapshot 테이블.';
COMMENT ON COLUMN biz_eval_snapshots.snapshot_id IS 'B48-SEED-001 및 평가확정 시점 snapshot 식별자';
COMMENT ON COLUMN biz_eval_snapshots.organization_code IS 'organizations.organization_code 참조 의도 (데이터 범위 필터)';
COMMENT ON COLUMN biz_eval_snapshots.target_user_id IS 'users.user_id 참조 의도 (평가 대상자)';
COMMENT ON COLUMN biz_eval_snapshots.rule_snapshot_ref IS '확정 당시 평가 기준정보 snapshot 참조값';
COMMENT ON COLUMN biz_eval_snapshots.material_snapshot_ref IS '확정 당시 평가자료 snapshot 참조값';
COMMENT ON COLUMN biz_eval_snapshots.preserved_result_ref IS '확정 당시 평가 결과 재현 참조값';
COMMENT ON COLUMN biz_eval_snapshots.snapshot_status IS 'PRESERVED:보존|CANCELLED:확정취소|RECONFIRMED:재확정';
COMMENT ON COLUMN biz_eval_snapshots.finalization_id IS 'evaluation_finalizations.finalization_id 참조 의도 (확정 또는 취소 이력 연결)';
COMMENT ON COLUMN biz_eval_snapshots.rule_snapshot_json IS '확정 시점 기준정보 snapshot 원문 JSON, 조회 전용';
COMMENT ON COLUMN biz_eval_snapshots.material_snapshot_json IS '확정 시점 평가자료 snapshot 원문 JSON, 조회 전용';
COMMENT ON COLUMN biz_eval_snapshots.preserved_result_json IS '확정 시점 보존 결과 JSON, 조회 전용';
COMMENT ON COLUMN biz_eval_snapshots.request_id IS '요청 식별자 전 구간 추적을 위해 업무 서비스에서 기록';

CREATE INDEX IF NOT EXISTS idx_biz_eval_snapshots_search
    ON biz_eval_snapshots(evaluation_year, finalization_point, organization_code, captured_at DESC);
CREATE INDEX IF NOT EXISTS idx_biz_eval_snapshots_target
    ON biz_eval_snapshots(target_user_id, evaluation_year);

INSERT INTO biz_eval_snapshots (
    snapshot_id,
    evaluation_year,
    finalization_point,
    organization_code,
    target_user_id,
    rule_snapshot_ref,
    material_snapshot_ref,
    preserved_result_ref,
    rule_snapshot_json,
    material_snapshot_json,
    preserved_result_json,
    snapshot_status,
    finalization_id,
    captured_at,
    request_id,
    created_by,
    updated_by
)
SELECT seed.snapshot_id,
       seed.evaluation_year,
       seed.finalization_point,
       seed.organization_code,
       professor.user_id,
       seed.rule_snapshot_ref,
       seed.material_snapshot_ref,
       seed.preserved_result_ref,
       seed.rule_snapshot_json::jsonb,
       seed.material_snapshot_json::jsonb,
       seed.preserved_result_json::jsonb,
       seed.snapshot_status,
       finalization.finalization_id,
       seed.captured_at,
       seed.request_id,
       admin.user_id,
       admin.user_id
FROM users admin
CROSS JOIN users professor
CROSS JOIN (VALUES
    ('B48-SNAPSHOT-001', '2026', '2026-FINAL-01', 'KNUE-DEPT-COMP', 'B48-RULE-SNAPSHOT-001', 'B48-MATERIAL-SNAPSHOT-001', 'B48-RESULT-SNAPSHOT-001', '{"ruleSet":"B33-CONFIRMED-2026","status":"CONFIRMED"}', '{"materialCount":3,"status":"EVALUATION_CONFIRMED"}', '{"finalScore":27.50,"generation":"CONFIRM"}', 'PRESERVED', TIMESTAMP '2026-09-03 09:00:00', 'REQ-B48-SEED-SNAPSHOT-001'),
    ('B48-SNAPSHOT-002', '2026', '2026-FINAL-RECONFIRM-01', 'KNUE-DEPT-COMP', 'B48-RULE-SNAPSHOT-002', 'B48-MATERIAL-SNAPSHOT-002', 'B48-RESULT-SNAPSHOT-002', '{"ruleSet":"B33-CONFIRMED-2026","status":"RECONFIRMED"}', '{"materialCount":3,"afterCancel":true}', '{"finalScore":29.00,"generation":"RECONFIRM"}', 'RECONFIRMED', TIMESTAMP '2026-09-04 10:00:00', 'REQ-B48-SEED-SNAPSHOT-002'),
    ('B48-SNAPSHOT-003', '2025', '2025-FINAL-01', 'KNUE-DEPT-COMP', 'B48-RULE-SNAPSHOT-003', 'B48-MATERIAL-SNAPSHOT-003', 'B48-RESULT-SNAPSHOT-003', '{"ruleSet":"B33-CONFIRMED-2025","status":"CONFIRMED"}', '{"materialCount":1,"status":"EVALUATION_CONFIRMED"}', '{"finalScore":15.50,"generation":"CONFIRM"}', 'PRESERVED', TIMESTAMP '2025-12-31 17:00:00', 'REQ-B48-SEED-SNAPSHOT-003')
) AS seed(snapshot_id, evaluation_year, finalization_point, organization_code, rule_snapshot_ref, material_snapshot_ref,
          preserved_result_ref, rule_snapshot_json, material_snapshot_json, preserved_result_json, snapshot_status, captured_at, request_id)
LEFT JOIN LATERAL (
    SELECT ef.finalization_id
    FROM evaluation_finalizations ef
    WHERE ef.target_user_id = professor.user_id
      AND ef.evaluation_year = seed.evaluation_year
    ORDER BY ef.created_at DESC, ef.finalization_id DESC
    LIMIT 1
) finalization ON true
WHERE admin.login_id = 'admin'
  AND professor.login_id = 'professor1'
ON CONFLICT (snapshot_id) DO UPDATE SET
    evaluation_year = EXCLUDED.evaluation_year,
    finalization_point = EXCLUDED.finalization_point,
    organization_code = EXCLUDED.organization_code,
    target_user_id = EXCLUDED.target_user_id,
    rule_snapshot_ref = EXCLUDED.rule_snapshot_ref,
    material_snapshot_ref = EXCLUDED.material_snapshot_ref,
    preserved_result_ref = EXCLUDED.preserved_result_ref,
    rule_snapshot_json = EXCLUDED.rule_snapshot_json,
    material_snapshot_json = EXCLUDED.material_snapshot_json,
    preserved_result_json = EXCLUDED.preserved_result_json,
    snapshot_status = EXCLUDED.snapshot_status,
    finalization_id = EXCLUDED.finalization_id,
    captured_at = EXCLUDED.captured_at,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

UPDATE basic48_seed_fixture_registry
SET lifecycle_status = 'MATERIALIZED', updated_at = CURRENT_TIMESTAMP
WHERE seed_id = 'B48-SEED-001';

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
VALUES (4861, 216, 'SCREEN', '시점 데이터 관리', 3, 'SCR-EVAL-SNAPSHOT-HISTORY', '/admin/evaluation-snapshots', 'clock-3', 'FILE_DATA', '평가확정 시점의 기준정보·평가자료 snapshot과 보존 결과 조회', 'Y', 'ACTIVE', 1)
ON CONFLICT (menu_id) DO UPDATE SET
    parent_menu_id = EXCLUDED.parent_menu_id,
    menu_name = EXCLUDED.menu_name,
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
SELECT menu_id, screen_id, url, icon, business_category, description, 1
FROM menus
WHERE screen_id = 'SCR-EVAL-SNAPSHOT-HISTORY'
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, 4861, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-48 시점 데이터 관리 조회 메뉴 접근'
FROM (VALUES ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by)
SELECT 'SCR-EVAL-SNAPSHOT-HISTORY', role_seed.role_code, function_seed.function_type, 'ALLOW', 'BASIC-48 시점 데이터 관리 조회 전용 권한', 1, 1
FROM (VALUES ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
CROSS JOIN (VALUES ('READ'), ('EXECUTE')) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET
    permission_allowed = EXCLUDED.permission_allowed,
    updated_at = CURRENT_TIMESTAMP,
    change_reason = EXCLUDED.change_reason,
    updated_by = EXCLUDED.updated_by;
