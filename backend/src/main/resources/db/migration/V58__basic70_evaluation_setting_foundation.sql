-- BASIC-70 평가요소 세부설정의 메뉴/fixture 기반을 기존 BASIC-60 운영 설정에 증분 보강한다.
-- 이미 등록된 화면은 유지하고, 누락된 경우에만 현재 menus 최대 ID 뒤의 연속 번호를 사용한다.

WITH next_menu AS (
    SELECT COALESCE(MAX(menu_id), 0) AS maximum_menu_id
    FROM menus
),
requested_menus AS (
    SELECT *
    FROM (VALUES
        (1, 340::bigint, '평가요소별 관리항목 설정', 6, 'SCR-EVALUATION-ELEMENT-MANAGEMENT-ITEM-SETTINGS', '/admin/evaluation-element-management-item-settings', 'clipboard-list', '평가요소별 관리항목 운영 설정'),
        (2, 340::bigint, '참여구분별 배분율 설정', 8, 'SCR-PARTICIPATION-ALLOCATION-RATE-SETTINGS', '/admin/participation-allocation-rate-settings', 'percent', '참여구분별 배분율 운영 설정'),
        (3, 340::bigint, '관리항목별 평가점수 설정', 7, 'SCR-MANAGEMENT-ITEM-EVALUATION-SCORE-SETTINGS', '/admin/management-item-evaluation-score-settings', 'badge-plus', '관리항목별 평가점수 운영 설정')
    ) AS requested(menu_sequence, parent_menu_id, menu_name, display_order, screen_id, url, icon, description)
),
missing_menus AS (
    SELECT requested.*,
           ROW_NUMBER() OVER (ORDER BY requested.menu_sequence) AS missing_sequence
    FROM requested_menus requested
    WHERE NOT EXISTS (
        SELECT 1
        FROM menus existing_menu
        WHERE existing_menu.screen_id = requested.screen_id
           OR existing_menu.url = requested.url
    )
)
INSERT INTO menus (
    menu_id,
    parent_menu_id,
    menu_type,
    menu_name,
    display_order,
    screen_id,
    url,
    icon,
    business_category,
    description,
    system_use_yn,
    status,
    updated_by,
    change_reason
)
SELECT
    next_menu.maximum_menu_id + missing_menus.missing_sequence,
    missing_menus.parent_menu_id,
    'SCREEN',
    missing_menus.menu_name,
    missing_menus.display_order,
    missing_menus.screen_id,
    missing_menus.url,
    missing_menus.icon,
    'BUSINESS',
    missing_menus.description,
    'Y',
    'ACTIVE',
    1,
    'BASIC-70 평가요소 세부설정 메뉴 보강'
FROM missing_menus
CROSS JOIN next_menu;

INSERT INTO menu_execution_info (
    menu_id,
    screen_id,
    url,
    icon,
    business_category,
    description,
    updated_by
)
SELECT
    menu.menu_id,
    menu.screen_id,
    menu.url,
    menu.icon,
    menu.business_category,
    menu.description,
    1
FROM menus menu
WHERE menu.screen_id IN (
    'SCR-EVALUATION-ELEMENT-MANAGEMENT-ITEM-SETTINGS',
    'SCR-PARTICIPATION-ALLOCATION-RATE-SETTINGS',
    'SCR-MANAGEMENT-ITEM-EVALUATION-SCORE-SETTINGS'
)
  AND NOT EXISTS (
      SELECT 1
      FROM menu_execution_info execution
      WHERE execution.menu_id = menu.menu_id
  );

INSERT INTO menu_permissions (
    target_type,
    target_id,
    menu_id,
    access_allowed,
    status,
    created_by,
    updated_by,
    change_reason
)
SELECT
    'ROLE',
    role_seed.role_code,
    menu.menu_id,
    'ALLOW',
    'ACTIVE',
    1,
    1,
    'BASIC-70 평가요소 세부설정 메뉴 접근'
FROM (VALUES ('R04'), ('R09')) AS role_seed(role_code)
CROSS JOIN (
    SELECT menu_id
    FROM menus
    WHERE screen_id IN (
        'SCR-EVALUATION-ELEMENT-MANAGEMENT-ITEM-SETTINGS',
        'SCR-PARTICIPATION-ALLOCATION-RATE-SETTINGS',
        'SCR-MANAGEMENT-ITEM-EVALUATION-SCORE-SETTINGS'
    )
) AS menu
WHERE NOT EXISTS (
    SELECT 1
    FROM menu_permissions permission
    WHERE permission.target_type = 'ROLE'
      AND permission.target_id = role_seed.role_code
      AND permission.menu_id = menu.menu_id
);

CREATE TABLE IF NOT EXISTS basic70_seed_fixture_registry (
    seed_id varchar(20) PRIMARY KEY,
    target_table varchar(100) NOT NULL,
    fixture_purpose varchar(500) NOT NULL,
    active_yn char(1) NOT NULL,
    evaluation_confirmed_yn char(1) NOT NULL,
    minimum_case_count integer NOT NULL DEFAULT 3,
    lifecycle_status varchar(30) NOT NULL DEFAULT 'MATERIALIZED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_basic70_seed_fixture_registry_id CHECK (seed_id IN ('B60-SEED-001', 'B60-SEED-002', 'B60-SEED-003')),
    CONSTRAINT ck_basic70_seed_fixture_registry_active CHECK (active_yn IN ('Y', 'N')),
    CONSTRAINT ck_basic70_seed_fixture_registry_confirmed CHECK (evaluation_confirmed_yn IN ('Y', 'N')),
    CONSTRAINT ck_basic70_seed_fixture_registry_count CHECK (minimum_case_count >= 3),
    CONSTRAINT ck_basic70_seed_fixture_registry_status CHECK (lifecycle_status IN ('RESERVED', 'MATERIALIZED'))
);
COMMENT ON TABLE basic70_seed_fixture_registry IS 'BASIC-70 평가요소 세부설정의 기존 BASIC-60 운영 시드 검증 범위와 상태 차이 fixture를 등록한다.';
COMMENT ON COLUMN basic70_seed_fixture_registry.active_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN basic70_seed_fixture_registry.evaluation_confirmed_yn IS 'Y:평가확정 영향범위|N:일반';
COMMENT ON COLUMN basic70_seed_fixture_registry.lifecycle_status IS 'RESERVED:명명예약|MATERIALIZED:데이터적재완료';

CREATE INDEX IF NOT EXISTS idx_basic70_seed_fixture_registry_target
    ON basic70_seed_fixture_registry(target_table, lifecycle_status);

INSERT INTO basic70_seed_fixture_registry (
    seed_id,
    target_table,
    fixture_purpose,
    active_yn,
    evaluation_confirmed_yn,
    minimum_case_count,
    lifecycle_status
)
VALUES
    ('B60-SEED-001', 'evaluation_element_management_item_settings', '정상 사용, 사용중지 경계, 평가확정 변경차단 관리항목 설정을 검증한다.', 'Y', 'N', 3, 'MATERIALIZED'),
    ('B60-SEED-002', 'participation_allocation_rate_settings', '정상 배분율, 연구자수 경계, 평가확정 변경차단 참여구분 설정을 검증한다.', 'Y', 'N', 3, 'MATERIALIZED'),
    ('B60-SEED-003', 'management_item_evaluation_score_settings', '정상 점수, 상한점수 경계, 평가확정 변경차단 관리항목 점수를 검증한다.', 'Y', 'N', 3, 'MATERIALIZED')
ON CONFLICT (seed_id) DO UPDATE SET
    target_table = EXCLUDED.target_table,
    fixture_purpose = EXCLUDED.fixture_purpose,
    active_yn = EXCLUDED.active_yn,
    evaluation_confirmed_yn = EXCLUDED.evaluation_confirmed_yn,
    minimum_case_count = EXCLUDED.minimum_case_count,
    lifecycle_status = EXCLUDED.lifecycle_status,
    updated_at = CURRENT_TIMESTAMP;
