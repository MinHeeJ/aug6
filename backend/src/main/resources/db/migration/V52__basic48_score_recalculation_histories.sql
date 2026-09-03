CREATE TABLE IF NOT EXISTS biz_recalc_hist (
    recalc_hist_id varchar(100) PRIMARY KEY,
    job_id varchar(100) NOT NULL UNIQUE,
    target_user_id bigint NOT NULL,
    evaluation_year varchar(4) NOT NULL,
    organization_code varchar(50) NOT NULL,
    formula_version_id varchar(100) NOT NULL,
    target_scope varchar(50) NOT NULL,
    changed_count integer NOT NULL,
    before_total_score numeric(12,2) NOT NULL,
    after_total_score numeric(12,2) NOT NULL,
    executed_at timestamp NOT NULL,
    criteria_detail text NOT NULL,
    target_change_summary_json jsonb NOT NULL,
    request_id varchar(100) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT ck_biz_recalc_hist_year CHECK (evaluation_year ~ '^[0-9]{4}$'),
    CONSTRAINT ck_biz_recalc_hist_scope CHECK (target_scope IN ('FORMULA_VERSION_CHANGE','TARGET_SCOPE','NO_CHANGE')),
    CONSTRAINT ck_biz_recalc_hist_counts CHECK (changed_count >= 0),
    CONSTRAINT ck_biz_recalc_hist_scores CHECK (before_total_score >= 0 AND after_total_score >= 0),
    CONSTRAINT fk_biz_recalc_hist_target_user FOREIGN KEY (target_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_biz_recalc_hist_organization FOREIGN KEY (organization_code) REFERENCES organizations(organization_code),
    CONSTRAINT fk_biz_recalc_hist_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_biz_recalc_hist_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE biz_recalc_hist IS '재계산 작업별 작업ID, 실행일시, 산식버전, 대상범위, 변경건수, 전후 총점, 대상자별 주요 변경내역을 조회 전용으로 보존하는 BASIC-48 재계산 이력 테이블.';
COMMENT ON COLUMN biz_recalc_hist.job_id IS 'evaluation_batch_jobs 또는 score_calculation_generations의 재계산 작업 식별자 참조 의도 (FK 미선언)';
COMMENT ON COLUMN biz_recalc_hist.target_user_id IS 'users.user_id 참조 의도 (대표 평가 대상자)';
COMMENT ON COLUMN biz_recalc_hist.organization_code IS 'organizations.organization_code 참조 의도 (데이터 범위 필터)';
COMMENT ON COLUMN biz_recalc_hist.formula_version_id IS 'calculation_formula_versions.formula_version_id 참조 의도 (재계산 적용 산식버전, FK 미선언)';
COMMENT ON COLUMN biz_recalc_hist.target_scope IS 'FORMULA_VERSION_CHANGE:산식버전 변경|TARGET_SCOPE:대상 범위|NO_CHANGE:변경 없음';
COMMENT ON COLUMN biz_recalc_hist.criteria_detail IS '상세 화면에서 표시할 재계산 사용기준 전문';
COMMENT ON COLUMN biz_recalc_hist.target_change_summary_json IS '대상자별 주요 변경내역 JSON, 재계산 작업 완료 시 기존 그룹 G 처리에서 생성된 결과를 조회 전용 보존';
COMMENT ON COLUMN biz_recalc_hist.request_id IS '요청 식별자 전 구간 추적을 위해 업무 서비스에서 기록';

CREATE INDEX IF NOT EXISTS idx_biz_recalc_hist_search
    ON biz_recalc_hist(evaluation_year, organization_code, target_user_id, executed_at DESC);
CREATE INDEX IF NOT EXISTS idx_biz_recalc_hist_scope
    ON biz_recalc_hist(target_scope, executed_at DESC);
CREATE INDEX IF NOT EXISTS idx_biz_recalc_hist_job
    ON biz_recalc_hist(job_id);

INSERT INTO biz_recalc_hist (
    recalc_hist_id,
    job_id,
    target_user_id,
    evaluation_year,
    organization_code,
    formula_version_id,
    target_scope,
    changed_count,
    before_total_score,
    after_total_score,
    executed_at,
    criteria_detail,
    target_change_summary_json,
    request_id,
    created_by,
    updated_by
)
SELECT seed.recalc_hist_id,
       seed.job_id,
       professor.user_id,
       seed.evaluation_year,
       seed.organization_code,
       seed.formula_version_id,
       seed.target_scope,
       seed.changed_count,
       seed.before_total_score,
       seed.after_total_score,
       seed.executed_at,
       seed.criteria_detail,
       cast(seed.target_change_summary_json as jsonb),
       seed.request_id,
       admin.user_id,
       admin.user_id
FROM users admin
CROSS JOIN users professor
CROSS JOIN (VALUES
    ('B48-RECALC-001', 'B48-JOB-RECALC-001', '2026', 'KNUE-DEPT-COMP', 'FORMULA-2026-V2', 'FORMULA_VERSION_CHANGE', 12, 1200.00::numeric, 1236.50::numeric, TIMESTAMP '2026-09-03 13:10:00', '산식버전 FORMULA-2026-V1에서 FORMULA-2026-V2로 변경된 관리항목 전체', '[{"targetUserId":2,"before":120.00,"after":123.50,"reason":"산식버전 변경"}]', 'REQ-B48-SEED-RECALC-001'),
    ('B48-RECALC-002', 'B48-JOB-RECALC-002', '2026', 'KNUE-DEPT-COMP', 'FORMULA-2026-V2', 'TARGET_SCOPE', 5, 820.00::numeric, 830.00::numeric, TIMESTAMP '2026-09-04 09:30:00', '컴퓨터교육과 연구영역 대상 범위 재계산', '[{"targetUserId":2,"before":80.00,"after":82.00,"reason":"대상 범위 재산정"}]', 'REQ-B48-SEED-RECALC-002'),
    ('B48-RECALC-003', 'B48-JOB-RECALC-003', '2026', 'KNUE-DEPT-COMP', 'FORMULA-2026-V2', 'NO_CHANGE', 0, 640.00::numeric, 640.00::numeric, TIMESTAMP '2026-09-05 15:20:00', '동일 산식과 동일 대상 범위 재계산 결과 변경 없음', '[{"targetUserId":2,"before":64.00,"after":64.00,"reason":"변경 없음"}]', 'REQ-B48-SEED-RECALC-003')
) AS seed(recalc_hist_id, job_id, evaluation_year, organization_code, formula_version_id, target_scope,
          changed_count, before_total_score, after_total_score, executed_at, criteria_detail, target_change_summary_json, request_id)
WHERE admin.login_id = 'admin'
  AND professor.login_id = 'professor1'
ON CONFLICT (recalc_hist_id) DO UPDATE SET
    job_id = EXCLUDED.job_id,
    target_user_id = EXCLUDED.target_user_id,
    evaluation_year = EXCLUDED.evaluation_year,
    organization_code = EXCLUDED.organization_code,
    formula_version_id = EXCLUDED.formula_version_id,
    target_scope = EXCLUDED.target_scope,
    changed_count = EXCLUDED.changed_count,
    before_total_score = EXCLUDED.before_total_score,
    after_total_score = EXCLUDED.after_total_score,
    executed_at = EXCLUDED.executed_at,
    criteria_detail = EXCLUDED.criteria_detail,
    target_change_summary_json = EXCLUDED.target_change_summary_json,
    request_id = EXCLUDED.request_id,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

UPDATE basic48_seed_fixture_registry
SET lifecycle_status = 'MATERIALIZED', updated_at = CURRENT_TIMESTAMP
WHERE seed_id = 'B48-SEED-004';

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
VALUES (4864, 216, 'SCREEN', '재계산 이력', 6, 'SCR-SCORE-RECALCULATION-HISTORY', '/admin/score-recalculation-histories', 'history', 'FILE_DATA', '재계산 작업별 기준과 전후 총점 비교 조회', 'Y', 'ACTIVE', 1)
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
WHERE screen_id = 'SCR-SCORE-RECALCULATION-HISTORY'
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, 4864, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-48 재계산 이력 조회 메뉴 접근'
FROM (VALUES ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by)
SELECT 'SCR-SCORE-RECALCULATION-HISTORY', role_seed.role_code, function_seed.function_type, 'ALLOW', 'BASIC-48 재계산 이력 조회 전용 권한', 1, 1
FROM (VALUES ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
CROSS JOIN (VALUES ('READ'), ('EXECUTE')) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET
    permission_allowed = EXCLUDED.permission_allowed,
    updated_at = CURRENT_TIMESTAMP,
    change_reason = EXCLUDED.change_reason,
    updated_by = EXCLUDED.updated_by;
