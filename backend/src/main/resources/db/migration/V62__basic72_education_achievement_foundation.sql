-- BASIC-72 교육영역 전반 실적의 공통 저장소 및 학생지도 Excel 검증 이력.
CREATE TABLE IF NOT EXISTS education_achievements (
  achievement_id BIGSERIAL PRIMARY KEY,
  achievement_type VARCHAR(40) NOT NULL,
  faculty_user_id BIGINT NOT NULL REFERENCES users(user_id),
  management_item_code VARCHAR(100) NOT NULL,
  occurred_on DATE NOT NULL,
  detail_content TEXT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  deleted_yn CHAR(1) NOT NULL DEFAULT 'N',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NOT NULL
);
COMMENT ON TABLE education_achievements IS '교육영역 강의평가·강의·학생지도·학위배출 실적을 상태와 함께 보관한다.';
COMMENT ON COLUMN education_achievements.status IS 'DRAFT:작성중|SUBMITTED:제출|DEPARTMENT_CONFIRMED:학과장확인|DEPARTMENT_REJECTED:학과장미승인|VERIFIED:인증|VERIFICATION_REJECTED:인증반려|EVALUATION_CONFIRMED:평가확정|DELETED:삭제';
COMMENT ON COLUMN education_achievements.faculty_user_id IS 'users.user_id 참조';
COMMENT ON COLUMN education_achievements.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN education_achievements.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';
CREATE INDEX IF NOT EXISTS idx_education_achievements_type_scope ON education_achievements (achievement_type, faculty_user_id, achievement_id DESC);

CREATE TABLE IF NOT EXISTS student_guidance_upload_histories (
  upload_history_id BIGSERIAL PRIMARY KEY,
  uploaded_by BIGINT NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  file_name VARCHAR(255) NOT NULL,
  total_count INTEGER NOT NULL,
  success_count INTEGER NOT NULL,
  failure_count INTEGER NOT NULL,
  error_file_ref VARCHAR(255),
  status VARCHAR(30) NOT NULL
);
COMMENT ON TABLE student_guidance_upload_histories IS '학생지도 Excel 업로드 검증 결과와 오류파일 참조를 보관한다.';
COMMENT ON COLUMN student_guidance_upload_histories.uploaded_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN student_guidance_upload_histories.status IS 'UPLOADED:업로드됨|VALIDATED:검증완료|APPLIED:반영완료|FAILED:실패';
CREATE INDEX IF NOT EXISTS idx_student_guidance_upload_histories_uploaded_by ON student_guidance_upload_histories (uploaded_by, upload_history_id DESC);

-- 도메인별 명시 테이블은 공통 상태·감사 컬럼을 공유하며, 상세 입력 필드는 FR-018 관리항목으로 확장한다.
CREATE TABLE IF NOT EXISTS lecture_evaluation_achievements (LIKE education_achievements INCLUDING ALL);
CREATE TABLE IF NOT EXISTS lecture_achievements (LIKE education_achievements INCLUDING ALL);
CREATE TABLE IF NOT EXISTS student_guidance_achievements (LIKE education_achievements INCLUDING ALL);
CREATE TABLE IF NOT EXISTS graduate_achievements (LIKE education_achievements INCLUDING ALL);
COMMENT ON TABLE lecture_evaluation_achievements IS '강의평가 실적의 입력·상태·이력 기준 데이터를 보관한다.';
COMMENT ON TABLE lecture_achievements IS '강의 실적의 입력·상태·이력 기준 데이터를 보관한다.';
COMMENT ON TABLE student_guidance_achievements IS '학생지도 실적의 입력·상태·이력 기준 데이터를 보관한다.';
COMMENT ON TABLE graduate_achievements IS '석·박사 배출 실적의 입력·상태·이력 기준 데이터를 보관한다.';

-- 기존 교수업적 메뉴 트리(300)에 BASIC-72 화면을 등록한다. 식별자는 현재 메뉴 최대값에서 계산해 충돌을 피한다.
INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by)
SELECT base.menu_id + item.ordinal, 300, 'SCREEN', item.menu_name, 70 + item.ordinal, item.screen_id, item.url, 'book-open', 'BUSINESS', item.description, 'Y', 'ACTIVE', 1
FROM (SELECT COALESCE(MAX(menu_id), 0) AS menu_id FROM menus) base
CROSS JOIN (VALUES
  (1, '강의평가 실적 관리', 'SCR-LECTURE-EVALUATION-ACHIEVEMENT', '/faculty/lecture-evaluations', '강의평가 실적을 입력하고 조회한다.'),
  (2, '강의실적 관리', 'SCR-LECTURE-ACHIEVEMENT', '/faculty/lecture-achievements', '강의 실적을 조회한다.'),
  (3, '학생지도 실적 관리', 'SCR-STUDENT-GUIDANCE-ACHIEVEMENT', '/faculty/student-guidance-achievements', '학생지도 실적과 일괄등록 화면으로 이동한다.'),
  (4, '학생지도 Excel 일괄등록', 'SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD', '/faculty/student-guidance-achievements/excel', '학생지도 Excel을 검증한다.'),
  (5, '석·박사 배출 실적 관리', 'SCR-GRADUATE-ACHIEVEMENT', '/faculty/graduate-achievements', '석·박사 배출 실적을 조회한다.')
) AS item(ordinal, menu_name, screen_id, url, description)
WHERE NOT EXISTS (SELECT 1 FROM menus existing WHERE existing.screen_id = item.screen_id);

-- API filter가 UI route의 유효 권한을 확인하므로 각 실적 메뉴에 최소 역할 권한을 함께 보장한다.
INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', permission.role_code, menu.menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-72 교육 실적 메뉴 기본 접근권한'
FROM (VALUES
  ('SCR-LECTURE-EVALUATION-ACHIEVEMENT', 'R01'),
  ('SCR-LECTURE-ACHIEVEMENT', 'R01'),
  ('SCR-STUDENT-GUIDANCE-ACHIEVEMENT', 'R01'),
  ('SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD', 'R07'),
  ('SCR-GRADUATE-ACHIEVEMENT', 'R01')
) AS permission(screen_id, role_code)
JOIN menus menu ON menu.screen_id = permission.screen_id
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE
SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP, updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;
