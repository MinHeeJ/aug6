-- BASIC-78 교육영역 실적 API가 실제 UI 메뉴 권한으로 평가되도록 화면·권한을 등록한다.
-- 화면 URL은 frontend/src/app/router.tsx와 AuthenticationFilter.pathToUiRoute의 단일 계약이다.
WITH next_menu AS (
    SELECT COALESCE(MAX(menu_id), 0) AS maximum_menu_id
    FROM menus
),
requested_menus AS (
    SELECT *
    FROM (VALUES
        (1, '강의평가 실적 관리', 10, 'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT', '/achievements/education/lecture-evaluations', 'clipboard-check', '강의평가 실적을 조회하고 등록한다.'),
        (2, '강의 실적 관리', 20, 'SCR-LECTURE-ACHIEVEMENT-MGMT', '/achievements/education/lecture-achievements', 'book-open', '강의 실적을 조회하고 등록한다.'),
        (3, '석·박사 배출 실적 관리', 30, 'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT', '/achievements/education/masters-doctoral-graduations', 'graduation-cap', '석·박사 배출 실적과 지도학생 정보를 조회하고 등록한다.')
    ) AS requested(menu_sequence, menu_name, display_order, screen_id, url, icon, description)
),
missing_menus AS (
    SELECT requested.*, ROW_NUMBER() OVER (ORDER BY requested.menu_sequence) AS missing_sequence
    FROM requested_menus requested
    WHERE NOT EXISTS (
        SELECT 1
        FROM menus existing_menu
        WHERE existing_menu.screen_id = requested.screen_id
           OR existing_menu.url = requested.url
    )
)
INSERT INTO menus (
    menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon,
    business_category, description, system_use_yn, status, updated_by, change_reason
)
SELECT
    next_menu.maximum_menu_id + missing_menus.missing_sequence, NULL, 'SCREEN', missing_menus.menu_name,
    missing_menus.display_order, missing_menus.screen_id, missing_menus.url, missing_menus.icon,
    'BUSINESS', missing_menus.description, 'Y', 'ACTIVE', 1, 'BASIC-78 교육영역 실적 메뉴 접근 보강'
FROM missing_menus
CROSS JOIN next_menu;

INSERT INTO menu_execution_info (
    menu_id, screen_id, url, icon, business_category, description, updated_by
)
SELECT menu.menu_id, menu.screen_id, menu.url, menu.icon, menu.business_category, menu.description, 1
FROM menus menu
WHERE menu.screen_id IN (
    'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
    'SCR-LECTURE-ACHIEVEMENT-MGMT',
    'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT'
)
  AND NOT EXISTS (
      SELECT 1 FROM menu_execution_info execution WHERE execution.menu_id = menu.menu_id
  );

INSERT INTO menu_permissions (
    target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason
)
SELECT 'ROLE', role_seed.role_code, menu.menu_id, 'ALLOW', 'ACTIVE', 1, 1,
       'BASIC-78 교육영역 실적 메뉴 접근'
FROM (VALUES ('R01'), ('R02'), ('R04'), ('R09')) AS role_seed(role_code)
CROSS JOIN (
    SELECT menu_id
    FROM menus
    WHERE screen_id IN (
        'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
        'SCR-LECTURE-ACHIEVEMENT-MGMT',
        'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT'
    )
) AS menu
WHERE NOT EXISTS (
    SELECT 1
    FROM menu_permissions permission
    WHERE permission.target_type = 'ROLE'
      AND permission.target_id = role_seed.role_code
      AND permission.menu_id = menu.menu_id
);
