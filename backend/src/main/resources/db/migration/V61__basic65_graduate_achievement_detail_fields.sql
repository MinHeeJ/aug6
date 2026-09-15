ALTER TABLE graduate_achievements
    ADD COLUMN IF NOT EXISTS dynamic_fields jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS attachment_refs jsonb NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN graduate_achievements.dynamic_fields IS 'FR-018 관리항목 설정에 따라 입력된 동적 필드 JSON. GraduateAchievementService 저장 시 애플리케이션에서 갱신';
COMMENT ON COLUMN graduate_achievements.attachment_refs IS '기존 FileStoragePort가 반환한 외부 비노출 첨부 참조 JSON 배열. GraduateAchievementService 저장 시 애플리케이션에서 갱신';

CREATE INDEX IF NOT EXISTS idx_graduate_achievements_dynamic_fields
    ON graduate_achievements USING gin (dynamic_fields);
