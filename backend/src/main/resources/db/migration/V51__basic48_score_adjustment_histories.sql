CREATE TABLE IF NOT EXISTS biz_score_adj_hist (
    adjustment_hist_id varchar(100) PRIMARY KEY,
    target_user_id bigint NOT NULL,
    evaluation_year varchar(4) NOT NULL,
    area_code varchar(50) NOT NULL,
    area_name varchar(100) NOT NULL,
    organization_code varchar(50) NOT NULL,
    management_item_code varchar(100) NOT NULL,
    adjustment_target varchar(30) NOT NULL,
    before_value numeric(10,2) NOT NULL,
    after_value numeric(10,2) NOT NULL,
    adjustment_reason varchar(500) NOT NULL,
    adjustment_remark text NOT NULL,
    adjusted_by bigint NOT NULL,
    approved_by bigint NOT NULL,
    adjusted_at timestamp NOT NULL,
    approved_at timestamp NOT NULL,
    approval_trace text NOT NULL,
    related_calc_hist_id varchar(100),
    request_id varchar(100) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT ck_biz_score_adj_hist_year CHECK (evaluation_year ~ '^[0-9]{4}$'),
    CONSTRAINT ck_biz_score_adj_hist_target CHECK (adjustment_target IN ('SCORE','PERCENTAGE')),
    CONSTRAINT ck_biz_score_adj_hist_values CHECK (before_value >= 0 AND after_value >= 0),
    CONSTRAINT fk_biz_score_adj_hist_target_user FOREIGN KEY (target_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_biz_score_adj_hist_organization FOREIGN KEY (organization_code) REFERENCES organizations(organization_code),
    CONSTRAINT fk_biz_score_adj_hist_adjusted_by FOREIGN KEY (adjusted_by) REFERENCES users(user_id),
    CONSTRAINT fk_biz_score_adj_hist_approved_by FOREIGN KEY (approved_by) REFERENCES users(user_id),
    CONSTRAINT fk_biz_score_adj_hist_calc_ref FOREIGN KEY (related_calc_hist_id) REFERENCES biz_score_calc_hist(calc_hist_id),
    CONSTRAINT fk_biz_score_adj_hist_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_biz_score_adj_hist_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE biz_score_adj_hist IS '수동 조정된 점수와 평가백분율의 조정 전후 값, 비고, 사유, 조정자, 승인자를 조회 전용으로 보존하는 BASIC-48 점수 조정 이력 테이블.';
COMMENT ON COLUMN biz_score_adj_hist.target_user_id IS 'users.user_id 참조 의도 (평가 대상자)';
COMMENT ON COLUMN biz_score_adj_hist.area_code IS 'evaluation_areas.area_code 참조 의도 (평가영역, FK 미선언)';
COMMENT ON COLUMN biz_score_adj_hist.organization_code IS 'organizations.organization_code 참조 의도 (데이터 범위 필터)';
COMMENT ON COLUMN biz_score_adj_hist.management_item_code IS 'evaluation_management_items.management_item_code 참조 의도 (관리항목, 버전별 코드라 FK 미선언)';
COMMENT ON COLUMN biz_score_adj_hist.adjustment_target IS 'SCORE:점수|PERCENTAGE:평가백분율';
COMMENT ON COLUMN biz_score_adj_hist.adjustment_remark IS '선택 행 상세에서 표시할 조정 비고 전문';
COMMENT ON COLUMN biz_score_adj_hist.adjusted_by IS 'users.user_id 참조 의도 (조정자)';
COMMENT ON COLUMN biz_score_adj_hist.approved_by IS 'users.user_id 참조 의도 (승인자)';
COMMENT ON COLUMN biz_score_adj_hist.approval_trace IS '승인 요청부터 승인 완료까지의 경위 전문';
COMMENT ON COLUMN biz_score_adj_hist.related_calc_hist_id IS 'biz_score_calc_hist.calc_hist_id 참조 의도 (조정 기준 산출 이력 연결)';
COMMENT ON COLUMN biz_score_adj_hist.request_id IS '요청 식별자 전 구간 추적을 위해 업무 서비스에서 기록';

CREATE INDEX IF NOT EXISTS idx_biz_score_adj_hist_search
    ON biz_score_adj_hist(evaluation_year, area_code, organization_code, target_user_id, adjusted_at DESC);
CREATE INDEX IF NOT EXISTS idx_biz_score_adj_hist_target
    ON biz_score_adj_hist(adjustment_target, adjusted_at DESC);
CREATE INDEX IF NOT EXISTS idx_biz_score_adj_hist_calc
    ON biz_score_adj_hist(related_calc_hist_id);

INSERT INTO biz_score_adj_hist (
    adjustment_hist_id,
    target_user_id,
    evaluation_year,
    area_code,
    area_name,
    organization_code,
    management_item_code,
    adjustment_target,
    before_value,
    after_value,
    adjustment_reason,
    adjustment_remark,
    adjusted_by,
    approved_by,
    adjusted_at,
    approved_at,
    approval_trace,
    related_calc_hist_id,
    request_id,
    created_by,
    updated_by
)
SELECT seed.adjustment_hist_id,
       professor.user_id,
       seed.evaluation_year,
       seed.area_code,
       seed.area_name,
       seed.organization_code,
       seed.management_item_code,
       seed.adjustment_target,
       seed.before_value,
       seed.after_value,
       seed.adjustment_reason,
       seed.adjustment_remark,
       admin.user_id,
       admin.user_id,
       seed.adjusted_at,
       seed.approved_at,
       seed.approval_trace,
       seed.related_calc_hist_id,
       seed.request_id,
       admin.user_id,
       admin.user_id
FROM users admin
CROSS JOIN users professor
CROSS JOIN (VALUES
    ('B48-ADJ-001', '2026', 'RESEARCH', '논문', 'KNUE-DEPT-COMP', 'MI-RESEARCH-PAPER', 'SCORE', 30.00::numeric, 32.00::numeric, '우수 학술지 가점 반영', '상향 조정 근거: 학술지 등급 확인 후 기준 배점 가점 2점을 반영했습니다.', TIMESTAMP '2026-09-03 10:10:00', TIMESTAMP '2026-09-03 10:40:00', '조정 요청 접수 -> 점수산출 감사자 검토 -> R09 승인 완료', 'B48-CALC-001', 'REQ-B48-SEED-ADJ-001'),
    ('B48-ADJ-002', '2026', 'RESEARCH', '논문', 'KNUE-DEPT-COMP', 'MI-RESEARCH-PAPER', 'SCORE', 15.00::numeric, 12.00::numeric, '중복 인정 제외', '하향 조정 근거: 동일 실적 중복 반영분을 제외하여 3점을 차감했습니다.', TIMESTAMP '2026-09-03 11:10:00', TIMESTAMP '2026-09-03 11:40:00', '중복 실적 확인 -> 조정 입력 -> 감사자 승인 완료', 'B48-CALC-002', 'REQ-B48-SEED-ADJ-002'),
    ('B48-ADJ-003', '2026', 'SERVICE', '봉사', 'KNUE-DEPT-COMP', 'MI-SERVICE-COMMITTEE', 'PERCENTAGE', 100.00::numeric, 80.00::numeric, '평가백분율 조정', '평가백분율 조정 근거: 참여기간 경계 조건에 따라 80%만 인정했습니다.', TIMESTAMP '2026-09-03 12:10:00', TIMESTAMP '2026-09-03 12:40:00', '참여기간 검토 -> 백분율 조정 -> 최종 승인', 'B48-CALC-003', 'REQ-B48-SEED-ADJ-003')
) AS seed(adjustment_hist_id, evaluation_year, area_code, area_name, organization_code, management_item_code,
          adjustment_target, before_value, after_value, adjustment_reason, adjustment_remark, adjusted_at, approved_at,
          approval_trace, related_calc_hist_id, request_id)
WHERE admin.login_id = 'admin'
  AND professor.login_id = 'professor1'
ON CONFLICT (adjustment_hist_id) DO UPDATE SET
    target_user_id = EXCLUDED.target_user_id,
    evaluation_year = EXCLUDED.evaluation_year,
    area_code = EXCLUDED.area_code,
    area_name = EXCLUDED.area_name,
    organization_code = EXCLUDED.organization_code,
    management_item_code = EXCLUDED.management_item_code,
    adjustment_target = EXCLUDED.adjustment_target,
    before_value = EXCLUDED.before_value,
    after_value = EXCLUDED.after_value,
    adjustment_reason = EXCLUDED.adjustment_reason,
    adjustment_remark = EXCLUDED.adjustment_remark,
    adjusted_by = EXCLUDED.adjusted_by,
    approved_by = EXCLUDED.approved_by,
    adjusted_at = EXCLUDED.adjusted_at,
    approved_at = EXCLUDED.approved_at,
    approval_trace = EXCLUDED.approval_trace,
    related_calc_hist_id = EXCLUDED.related_calc_hist_id,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

UPDATE basic48_seed_fixture_registry
SET lifecycle_status = 'MATERIALIZED', updated_at = CURRENT_TIMESTAMP
WHERE seed_id = 'B48-SEED-003';

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
VALUES (4863, 216, 'SCREEN', '점수 조정 이력', 5, 'SCR-SCORE-ADJUSTMENT-HISTORY', '/admin/score-adjustment-histories', 'history', 'FILE_DATA', '수동 조정 전후 값과 승인 경위 조회', 'Y', 'ACTIVE', 1)
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
WHERE screen_id = 'SCR-SCORE-ADJUSTMENT-HISTORY'
ON CONFLICT (menu_id) DO UPDATE SET
    screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, 4863, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-48 점수 조정 이력 조회 메뉴 접근'
FROM (VALUES ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET
    access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO function_permissions (screen_id, role_code, function_type, permission_allowed, change_reason, created_by, updated_by)
SELECT 'SCR-SCORE-ADJUSTMENT-HISTORY', role_seed.role_code, function_seed.function_type, 'ALLOW', 'BASIC-48 점수 조정 이력 조회 전용 권한', 1, 1
FROM (VALUES ('R04'), ('R08'), ('R09')) AS role_seed(role_code)
CROSS JOIN (VALUES ('READ'), ('EXECUTE')) AS function_seed(function_type)
ON CONFLICT (screen_id, role_code, function_type) DO UPDATE SET
    permission_allowed = EXCLUDED.permission_allowed,
    updated_at = CURRENT_TIMESTAMP,
    change_reason = EXCLUDED.change_reason,
    updated_by = EXCLUDED.updated_by;
