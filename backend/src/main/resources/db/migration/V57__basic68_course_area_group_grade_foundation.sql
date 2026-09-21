-- BASIC-68 교과영역 그룹평가 성적 조회의 메뉴·권한과 B68-SEED-001 fixture.
-- 기존 projection은 V56에서 생성되었으므로 이 migration은 조회용 seed와 접근 경계를 증분 보강한다.

WITH next_menu AS (
    SELECT COALESCE(MAX(menu_id), 0) + 1 AS menu_id
    FROM menus
),
inserted_menu AS (
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
        updated_by
    )
    SELECT
        next_menu.menu_id,
        300,
        'SCREEN',
        '교과영역 그룹평가 성적 조회',
        60,
        'SCR-COURSE-AREA-GROUP-GRADE-QUERY',
        '/faculty/course-area-group-grades',
        'graduation-cap',
        'BUSINESS',
        '교원별 교과영역 그룹평가 성적을 조회 전용으로 제공한다.',
        'Y',
        'ACTIVE',
        1
    FROM next_menu
    WHERE NOT EXISTS (
        SELECT 1
        FROM menus
        WHERE screen_id = 'SCR-COURSE-AREA-GROUP-GRADE-QUERY'
           OR url = '/faculty/course-area-group-grades'
    )
    RETURNING menu_id
)
SELECT menu_id
FROM inserted_menu;

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
    'SCR-COURSE-AREA-GROUP-GRADE-QUERY',
    '/faculty/course-area-group-grades',
    'graduation-cap',
    'BUSINESS',
    '교원별 교과영역 그룹평가 성적을 조회 전용으로 제공한다.',
    1
FROM menus menu
WHERE menu.screen_id = 'SCR-COURSE-AREA-GROUP-GRADE-QUERY'
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
    'BASIC-68 교과영역 그룹평가 성적 조회 메뉴 접근'
FROM (VALUES ('R01'), ('R04'), ('R09')) AS role_seed(role_code)
CROSS JOIN (
    SELECT menu_id
    FROM menus
    WHERE screen_id = 'SCR-COURSE-AREA-GROUP-GRADE-QUERY'
) AS menu
WHERE NOT EXISTS (
    SELECT 1
    FROM menu_permissions permission
    WHERE permission.target_type = 'ROLE'
      AND permission.target_id = role_seed.role_code
      AND permission.menu_id = menu.menu_id
);

DELETE FROM menu_permissions
WHERE menu_id IN (
    SELECT menu_id
    FROM menus
    WHERE screen_id = 'SCR-COURSE-AREA-GROUP-GRADE-QUERY'
)
  AND target_type = 'ROLE'
  AND target_id = 'R08';

DELETE FROM function_permissions
WHERE screen_id = 'SCR-COURSE-AREA-GROUP-GRADE-QUERY'
  AND function_type = 'READ'
  AND role_code = 'R08';

INSERT INTO user_roles (
    user_id,
    role_code,
    assignment_type,
    approver_user_id,
    status,
    change_reason
)
SELECT
    professor.user_id,
    'R01',
    'MANUAL',
    1,
    'ACTIVE',
    'B68-SEED-001 본인 성적 조회 범위 fixture'
FROM users professor
WHERE professor.login_id = 'professor1'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles role_assignment
      WHERE role_assignment.user_id = professor.user_id
        AND role_assignment.role_code = 'R01'
        AND role_assignment.status = 'ACTIVE'
  );

INSERT INTO course_area_group_grade_results (
    faculty_user_id,
    employee_no,
    faculty_name,
    completion_type,
    semester,
    course_area,
    group_grade,
    detail_summary,
    published_yn,
    evaluated_at
)
SELECT
    professor.user_id,
    professor.employee_no,
    personnel.name,
    seed.completion_type,
    seed.semester,
    seed.course_area,
    seed.group_grade,
    seed.detail_summary,
    seed.published_yn,
    CURRENT_TIMESTAMP
FROM users professor
JOIN korus_personnel_snapshots personnel ON personnel.employee_no = professor.employee_no
CROSS JOIN (
    VALUES
        ('LIBERAL', '2026-1', 'LECTURE', 91.25, 'B68-SEED-001 교양 1학기 그룹평가 성적', 'Y'),
        ('MAJOR', '2026-2', 'PRACTICE', 87.50, 'B68-SEED-001 전공 2학기 그룹평가 성적', 'Y')
) AS seed(completion_type, semester, course_area, group_grade, detail_summary, published_yn)
WHERE professor.login_id = 'professor1'
  AND NOT EXISTS (
      SELECT 1
      FROM course_area_group_grade_results result
      WHERE result.faculty_user_id = professor.user_id
        AND result.completion_type = seed.completion_type
        AND result.semester = seed.semester
        AND result.course_area = seed.course_area
  );

INSERT INTO course_area_group_grade_results (
    faculty_user_id,
    employee_no,
    faculty_name,
    completion_type,
    semester,
    course_area,
    group_grade,
    detail_summary,
    published_yn,
    evaluated_at
)
SELECT
    administrator.user_id,
    administrator.employee_no,
    personnel.name,
    'OTHER',
    '2026-2',
    'ONLINE',
    73.00,
    'B68-SEED-001 R01 범위 밖 다른 교과영역 성적',
    'Y',
    CURRENT_TIMESTAMP
FROM users administrator
JOIN korus_personnel_snapshots personnel ON personnel.employee_no = administrator.employee_no
WHERE administrator.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM course_area_group_grade_results result
      WHERE result.faculty_user_id = administrator.user_id
        AND result.completion_type = 'OTHER'
        AND result.semester = '2026-2'
        AND result.course_area = 'ONLINE'
  );
