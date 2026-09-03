CREATE TABLE IF NOT EXISTS biz_score_calc_hist (
    calc_hist_id varchar(100) PRIMARY KEY,
    target_user_id bigint NOT NULL,
    evaluation_year varchar(4) NOT NULL,
    area_code varchar(50) NOT NULL,
    area_name varchar(100) NOT NULL,
    organization_code varchar(50) NOT NULL,
    source_achievement_id bigint NOT NULL,
    source_achievement_title varchar(300) NOT NULL,
    source_achievement_link varchar(500) NOT NULL,
    management_item_code varchar(100) NOT NULL,
    base_score numeric(10,2) NOT NULL,
    participation_type varchar(50) NOT NULL,
    distribution_rate numeric(7,4) NOT NULL,
    cap_applied_yn char(1) NOT NULL DEFAULT 'N',
    formula_version_id varchar(100) NOT NULL,
    generation_no integer NOT NULL,
    calculated_score numeric(10,2) NOT NULL,
    calculation_steps_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    calculation_generation_id bigint,
    evaluation_material_id bigint,
    calculated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_id varchar(100) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT ck_biz_score_calc_hist_year CHECK (evaluation_year ~ '^[0-9]{4}$'),
    CONSTRAINT ck_biz_score_calc_hist_scores CHECK (base_score >= 0 AND calculated_score >= 0 AND distribution_rate >= 0),
    CONSTRAINT ck_biz_score_calc_hist_generation CHECK (generation_no > 0),
    CONSTRAINT ck_biz_score_calc_hist_cap CHECK (cap_applied_yn IN ('Y','N')),
    CONSTRAINT fk_biz_score_calc_hist_target_user FOREIGN KEY (target_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_biz_score_calc_hist_organization FOREIGN KEY (organization_code) REFERENCES organizations(organization_code),
    CONSTRAINT fk_biz_score_calc_hist_generation_ref FOREIGN KEY (calculation_generation_id) REFERENCES score_calculation_generations(calculation_generation_id),
    CONSTRAINT fk_biz_score_calc_hist_material_ref FOREIGN KEY (evaluation_material_id) REFERENCES evaluation_materials(evaluation_material_id),
    CONSTRAINT fk_biz_score_calc_hist_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_biz_score_calc_hist_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE biz_score_calc_hist IS '원천 실적, 적용 규정버전, 기준배점, 참여구분, 배분율, 상한 적용 여부, 산출점수와 계산 세대를 조회 전용으로 보존하는 BASIC-48 점수 산출 이력 테이블.';
COMMENT ON COLUMN biz_score_calc_hist.target_user_id IS 'users.user_id 참조 의도 (평가 대상자, R01 본인 범위 필터)';
COMMENT ON COLUMN biz_score_calc_hist.area_code IS 'evaluation_areas.area_code 참조 의도 (평가영역, FK 미선언)';
COMMENT ON COLUMN biz_score_calc_hist.organization_code IS 'organizations.organization_code 참조 의도 (데이터 범위 필터)';
COMMENT ON COLUMN biz_score_calc_hist.source_achievement_id IS 'source achievement record 참조 의도 (원천 실적 상세 이동, FK 미선언)';
COMMENT ON COLUMN biz_score_calc_hist.source_achievement_link IS '원천 업적 상세 화면으로 이동하기 위한 애플리케이션 route link';
COMMENT ON COLUMN biz_score_calc_hist.management_item_code IS 'evaluation_management_items.management_item_code 참조 의도 (관리항목, 버전별 코드라 FK 미선언)';
COMMENT ON COLUMN biz_score_calc_hist.participation_type IS 'SOLE:단독|CO_AUTHOR:공동저자|OTHER:기타';
COMMENT ON COLUMN biz_score_calc_hist.cap_applied_yn IS 'Y:상한적용|N:상한미적용';
COMMENT ON COLUMN biz_score_calc_hist.formula_version_id IS 'calculation_formula_versions.formula_version_id 참조 의도 (적용 산식버전)';
COMMENT ON COLUMN biz_score_calc_hist.calculation_steps_json IS '원천 실적에서 산출점수까지 단계별 산출근거 JSON, 점수 계산 서비스가 생성 시점에 저장';
COMMENT ON COLUMN biz_score_calc_hist.calculation_generation_id IS 'score_calculation_generations.calculation_generation_id 참조 의도 (계산 세대 연결)';
COMMENT ON COLUMN biz_score_calc_hist.evaluation_material_id IS 'evaluation_materials.evaluation_material_id 참조 의도 (평가자료 연결)';
COMMENT ON COLUMN biz_score_calc_hist.request_id IS '요청 식별자 전 구간 추적을 위해 업무 서비스에서 기록';

CREATE INDEX IF NOT EXISTS idx_biz_score_calc_hist_search
    ON biz_score_calc_hist(evaluation_year, area_code, organization_code, target_user_id, calculated_at DESC);
CREATE INDEX IF NOT EXISTS idx_biz_score_calc_hist_generation
    ON biz_score_calc_hist(evaluation_material_id, generation_no DESC);
CREATE INDEX IF NOT EXISTS idx_biz_score_calc_hist_source
    ON biz_score_calc_hist(source_achievement_id);

INSERT INTO biz_score_calc_hist (
    calc_hist_id,
    target_user_id,
    evaluation_year,
    area_code,
    area_name,
    organization_code,
    source_achievement_id,
    source_achievement_title,
    source_achievement_link,
    management_item_code,
    base_score,
    participation_type,
    distribution_rate,
    cap_applied_yn,
    formula_version_id,
    generation_no,
    calculated_score,
    calculation_steps_json,
    calculation_generation_id,
    evaluation_material_id,
    calculated_at,
    request_id,
    created_by,
    updated_by
)
SELECT seed.calc_hist_id,
       professor.user_id,
       seed.evaluation_year,
       seed.area_code,
       seed.area_name,
       seed.organization_code,
       seed.source_achievement_id,
       seed.source_achievement_title,
       seed.source_achievement_link,
       seed.management_item_code,
       seed.base_score,
       seed.participation_type,
       seed.distribution_rate,
       seed.cap_applied_yn,
       seed.formula_version_id,
       seed.generation_no,
       seed.calculated_score,
       seed.calculation_steps_json::jsonb,
       generation.calculation_generation_id,
       material.evaluation_material_id,
       seed.calculated_at,
       seed.request_id,
       admin.user_id,
       admin.user_id
FROM users admin
CROSS JOIN users professor
CROSS JOIN (VALUES
    ('B48-CALC-001', '2026', 'RESEARCH', '논문', 'KNUE-DEPT-COMP', 9001::bigint, '교육학술지 논문', '/admin/achievement-data-histories?sourceAchievementId=9001', 'MI-RESEARCH-PAPER', 30.00::numeric, 'SOLE', 1.0000::numeric, 'N', 'FORMULA-2026-01', 1, 30.00::numeric, '[{"step":"원천 실적","value":"교육학술지 논문"},{"step":"관리항목","value":"MI-RESEARCH-PAPER"},{"step":"기준점수","value":30.00},{"step":"참여구분","value":"SOLE"},{"step":"배분율","value":1.0000},{"step":"산식","value":"FORMULA-2026-01"},{"step":"산출점수","value":30.00}]', TIMESTAMP '2026-09-03 09:10:00', 'REQ-B48-SEED-CALC-001'),
    ('B48-CALC-002', '2026', 'RESEARCH', '논문', 'KNUE-DEPT-COMP', 9002::bigint, '공동저자 연구논문', '/admin/achievement-data-histories?sourceAchievementId=9002', 'MI-RESEARCH-PAPER', 30.00::numeric, 'CO_AUTHOR', 0.5000::numeric, 'N', 'FORMULA-2026-01', 1, 15.00::numeric, '[{"step":"원천 실적","value":"공동저자 연구논문"},{"step":"관리항목","value":"MI-RESEARCH-PAPER"},{"step":"기준점수","value":30.00},{"step":"참여구분","value":"CO_AUTHOR"},{"step":"배분율","value":0.5000},{"step":"산식","value":"FORMULA-2026-01"},{"step":"산출점수","value":15.00}]', TIMESTAMP '2026-09-03 09:20:00', 'REQ-B48-SEED-CALC-002'),
    ('B48-CALC-003', '2026', 'SERVICE', '봉사', 'KNUE-DEPT-COMP', 9003::bigint, '상한 적용 봉사실적', '/admin/achievement-data-histories?sourceAchievementId=9003', 'MI-SERVICE-COMMITTEE', 25.00::numeric, 'OTHER', 1.0000::numeric, 'Y', 'FORMULA-2026-02', 2, 20.00::numeric, '[{"step":"원천 실적","value":"상한 적용 봉사실적"},{"step":"관리항목","value":"MI-SERVICE-COMMITTEE"},{"step":"기준점수","value":25.00},{"step":"참여구분","value":"OTHER"},{"step":"배분율","value":1.0000},{"step":"상한적용","value":"Y"},{"step":"산출점수","value":20.00}]', TIMESTAMP '2026-09-03 09:30:00', 'REQ-B48-SEED-CALC-003')
) AS seed(calc_hist_id, evaluation_year, area_code, area_name, organization_code, source_achievement_id, source_achievement_title,
          source_achievement_link, management_item_code, base_score, participation_type, distribution_rate, cap_applied_yn,
          formula_version_id, generation_no, calculated_score, calculation_steps_json, calculated_at, request_id)
LEFT JOIN LATERAL (
    SELECT em.evaluation_material_id
    FROM evaluation_materials em
    WHERE em.target_user_id = professor.user_id
      AND em.evaluation_year = seed.evaluation_year
      AND em.source_achievement_id = seed.source_achievement_id
    ORDER BY em.created_at DESC, em.evaluation_material_id DESC
    LIMIT 1
) material ON true
LEFT JOIN LATERAL (
    SELECT scg.calculation_generation_id
    FROM score_calculation_generations scg
    WHERE scg.evaluation_material_id = material.evaluation_material_id
      AND scg.generation_no = seed.generation_no
    ORDER BY scg.created_at DESC, scg.calculation_generation_id DESC
    LIMIT 1
) generation ON true
WHERE admin.login_id = 'admin'
  AND professor.login_id = 'professor1'
ON CONFLICT (calc_hist_id) DO UPDATE SET
    target_user_id = EXCLUDED.target_user_id,
    evaluation_year = EXCLUDED.evaluation_year,
    area_code = EXCLUDED.area_code,
    area_name = EXCLUDED.area_name,
    organization_code = EXCLUDED.organization_code,
    source_achievement_id = EXCLUDED.source_achievement_id,
    source_achievement_title = EXCLUDED.source_achievement_title,
    source_achievement_link = EXCLUDED.source_achievement_link,
    management_item_code = EXCLUDED.management_item_code,
    base_score = EXCLUDED.base_score,
    participation_type = EXCLUDED.participation_type,
    distribution_rate = EXCLUDED.distribution_rate,
    cap_applied_yn = EXCLUDED.cap_applied_yn,
    formula_version_id = EXCLUDED.formula_version_id,
    generation_no = EXCLUDED.generation_no,
    calculated_score = EXCLUDED.calculated_score,
    calculation_steps_json = EXCLUDED.calculation_steps_json,
    calculation_generation_id = EXCLUDED.calculation_generation_id,
    evaluation_material_id = EXCLUDED.evaluation_material_id,
    calculated_at = EXCLUDED.calculated_at,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

UPDATE basic48_seed_fixture_registry
SET lifecycle_status = 'MATERIALIZED', updated_at = CURRENT_TIMESTAMP
WHERE seed_id = 'B48-SEED-002';

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
VALUES (4862, 216, 'SCREEN', '점수 산출 이력', 4, 'SCR-SCORE-CALC-HISTORY', '/admin/score-calculation-histories', 'list-tree', 'FILE_DATA', '개인별 점수 산출근거와 원천 업적 이동 링크 조회', 'Y', 'ACTIVE', 1)
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
WHERE screen_id = 'SCR-SCORE-CALC-HISTORY'
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, 4862, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-48 점수 산출 이력 조회 메뉴 접근'
FROM (VALUES ('R01'), ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by)
SELECT 'SCR-SCORE-CALC-HISTORY', role_seed.role_code, function_seed.function_type, 'ALLOW', 'BASIC-48 점수 산출 이력 조회 전용 권한', 1, 1
FROM (VALUES ('R01'), ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
CROSS JOIN (VALUES ('READ'), ('EXECUTE')) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET
    permission_allowed = EXCLUDED.permission_allowed,
    updated_at = CURRENT_TIMESTAMP,
    change_reason = EXCLUDED.change_reason,
    updated_by = EXCLUDED.updated_by;
