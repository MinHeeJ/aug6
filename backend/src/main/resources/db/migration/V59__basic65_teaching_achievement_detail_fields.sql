ALTER TABLE teaching_achievements
    ADD COLUMN IF NOT EXISTS dynamic_fields jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS attachment_refs jsonb NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN teaching_achievements.dynamic_fields IS 'FR-018 동적 관리항목 값. TeachingAchievementService 저장 시 애플리케이션에서 갱신';
COMMENT ON COLUMN teaching_achievements.attachment_refs IS '기존 FileStoragePort 첨부 보안 참조값 목록. TeachingAchievementService 저장 시 애플리케이션에서 갱신';
