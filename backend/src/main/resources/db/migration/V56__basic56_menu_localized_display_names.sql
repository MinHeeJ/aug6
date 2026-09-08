ALTER TABLE menus
    ADD COLUMN IF NOT EXISTS menu_name_en varchar(200);

COMMENT ON COLUMN menus.menu_name_en IS '영어 선택 시 메뉴 표시명. null 또는 blank이면 MenuStructureManagementService.getLocalizedMenuTree에서 menus.menu_name으로 fallback한다.';

UPDATE menus
SET menu_name_en = seed.menu_name_en,
    updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('시스템 관리', 'System Management'),
    ('사용자·조직 관리', 'User and Organization Management'),
    ('역할·권한 관리', 'Role and Permission Management'),
    ('메뉴 관리', 'Menu Management'),
    ('공통코드 관리', 'Common Code Management'),
    ('사용자 관리', 'User Management'),
    ('조직 관리', 'Organization Management'),
    ('역할 관리', 'Role Management'),
    ('사용자 역할 관리', 'User Role Management'),
    ('메뉴 권한 관리', 'Menu Permission Management'),
    ('메뉴 구조 관리', 'Menu Structure Management'),
    ('메뉴 정보 관리', 'Menu Information Management'),
    ('코드그룹 관리', 'Code Group Management'),
    ('상세코드 관리', 'Detail Code Management')
) AS seed(korean_menu_name, menu_name_en)
WHERE menu_name = seed.korean_menu_name
  AND (menus.menu_name_en IS NULL OR btrim(menus.menu_name_en) = '');
