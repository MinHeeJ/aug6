ALTER TABLE exception_period_settings ALTER COLUMN teacher_user_id DROP NOT NULL;
COMMENT ON COLUMN exception_period_settings.teacher_user_id IS 'users.user_id 참조 의도 (예외기간 대상 교원, 과거 이관 데이터는 미지정 가능)';

ALTER TABLE researcher_profiles ALTER COLUMN degree_prerequisite_missing_yn DROP NOT NULL;
ALTER TABLE researcher_profiles ALTER COLUMN updated_at DROP NOT NULL;
COMMENT ON COLUMN researcher_profiles.degree_prerequisite_missing_yn IS 'Y:미충족|N:충족';
COMMENT ON COLUMN researcher_profiles.updated_at IS 'ResearcherProfileService 저장 시 직접관리 정보 변경일시로 애플리케이션에서 갱신, 이관 데이터는 미설정 가능';
