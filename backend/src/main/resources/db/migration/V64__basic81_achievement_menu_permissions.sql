-- BASIC-81 직접 API 요청을 기존 메뉴 권한 체계에 연결한다.

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
    COALESCE((SELECT MAX(menu_id) FROM menus), 0) + seed.row_number,
    NULL,
    'SCREEN',
    seed.menu_name,
    1000 + seed.row_number,
    seed.screen_id,
    seed.url,
    seed.icon,
    'EDUCATION_ACHIEVEMENT',
    seed.description,
    'Y',
    'ACTIVE',
    admin_user.user_id,
    'BASIC-81 교육실적 화면 권한 연결'
FROM users admin_user
CROSS JOIN (
    VALUES
        (1, '강의평가 실적 관리', 'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
            '/achievements/education/lecture-evaluations', 'clipboard-check',
            'R01, R02, R04 강의평가 실적 조회 및 저장 화면'),
        (2, '강의실적 관리', 'SCR-LECTURE-ACHIEVEMENT-MGMT',
            '/achievements/education/lecture-achievements', 'book-open',
            'R01, R02, R04 강의실적 조회 및 저장 화면'),
        (3, '석·박사 배출 실적 관리', 'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT',
            '/achievements/education/masters-doctoral-graduations', 'graduation-cap',
            'R01, R02, R04 석·박사 배출 실적 조회 및 저장 화면'),
        (4, '학생지도 실적 관리', 'SCR-STUDENT-GUIDANCE-ACHIEVEMENT-MGMT',
            '/achievements/education/student-guidance-achievements', 'users',
            'R01, R02, R04 학생지도 실적 조회 및 저장 화면')
) AS seed(row_number, menu_name, screen_id, url, icon, description)
WHERE admin_user.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM menus existing_menu
      WHERE existing_menu.screen_id = seed.screen_id
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
    admin_user.user_id,
    admin_user.user_id,
    'BASIC-81 교육실적 R01/R02/R04 접근'
FROM menus menu
CROSS JOIN users admin_user
CROSS JOIN (
    VALUES ('R01'), ('R02'), ('R04')
) AS role_seed(role_code)
WHERE menu.screen_id IN (
        'SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT',
        'SCR-LECTURE-ACHIEVEMENT-MGMT',
        'SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT',
        'SCR-STUDENT-GUIDANCE-ACHIEVEMENT-MGMT'
    )
  AND admin_user.login_id = 'admin'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE
SET access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;
