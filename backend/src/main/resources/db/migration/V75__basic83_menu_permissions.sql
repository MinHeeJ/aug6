-- BASIC-83 교육영역 실적 API를 메뉴 기반 접근 제어에 연결한다.

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
    1100 + seed.row_number,
    seed.screen_id,
    seed.url,
    seed.icon,
    'EDUCATION_ACHIEVEMENT',
    seed.description,
    'Y',
    'ACTIVE',
    admin_user.user_id,
    'BASIC-83 교육영역 실적 화면 권한 연결'
FROM users admin_user
CROSS JOIN (
    VALUES
        (
            1,
            '취업률 제고 실적 관리',
            'SCR-EMPLOYMENT-RATE-IMPROVEMENTS',
            '/faculty/education/employment-rate-improvements',
            'trending-up',
            'R01, R02, R04 취업률 제고 실적 조회 및 저장 화면'
        ),
        (
            2,
            '강좌 개설·운영 실적 관리',
            'SCR-COURSE-OPERATIONS',
            '/faculty/education/course-operations',
            'book-open',
            'R01, R02, R04 강좌 개설·운영 실적 조회 및 저장 화면'
        ),
        (
            3,
            '강의개선 실적 관리',
            'SCR-LECTURE-IMPROVEMENTS',
            '/faculty/education/lecture-improvements',
            'presentation',
            'R01, R02, R04 강의개선 실적 조회 및 저장 화면'
        ),
        (
            4,
            '취업률 실적 관리',
            'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS',
            '/faculty/education/employment-rate-achievements',
            'file-spreadsheet',
            'R01, R02, R04 취업률 실적 조회와 R07 Excel·일괄 처리 화면'
        )
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
    'BASIC-83 교육영역 실적 역할별 접근'
FROM menus menu
CROSS JOIN users admin_user
JOIN (
    VALUES
        ('SCR-EMPLOYMENT-RATE-IMPROVEMENTS', 'R01'),
        ('SCR-EMPLOYMENT-RATE-IMPROVEMENTS', 'R02'),
        ('SCR-EMPLOYMENT-RATE-IMPROVEMENTS', 'R04'),
        ('SCR-COURSE-OPERATIONS', 'R01'),
        ('SCR-COURSE-OPERATIONS', 'R02'),
        ('SCR-COURSE-OPERATIONS', 'R04'),
        ('SCR-LECTURE-IMPROVEMENTS', 'R01'),
        ('SCR-LECTURE-IMPROVEMENTS', 'R02'),
        ('SCR-LECTURE-IMPROVEMENTS', 'R04'),
        ('SCR-EMPLOYMENT-RATE-ACHIEVEMENTS', 'R01'),
        ('SCR-EMPLOYMENT-RATE-ACHIEVEMENTS', 'R02'),
        ('SCR-EMPLOYMENT-RATE-ACHIEVEMENTS', 'R04'),
        ('SCR-EMPLOYMENT-RATE-ACHIEVEMENTS', 'R07')
) AS role_seed(screen_id, role_code)
    ON role_seed.screen_id = menu.screen_id
WHERE menu.screen_id IN (
        'SCR-EMPLOYMENT-RATE-IMPROVEMENTS',
        'SCR-COURSE-OPERATIONS',
        'SCR-LECTURE-IMPROVEMENTS',
        'SCR-EMPLOYMENT-RATE-ACHIEVEMENTS'
    )
  AND admin_user.login_id = 'admin'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE
SET access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;
