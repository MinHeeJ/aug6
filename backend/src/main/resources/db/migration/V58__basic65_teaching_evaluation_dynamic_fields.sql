ALTER TABLE teaching_evaluation_achievements
    ADD COLUMN IF NOT EXISTS dynamic_fields jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS attachment_refs jsonb NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN teaching_evaluation_achievements.dynamic_fields IS '관리항목 설정에 따라 입력된 동적 필드 JSON. TeachingEvaluationAchievementService 저장 시 검증·갱신한다.';
COMMENT ON COLUMN teaching_evaluation_achievements.attachment_refs IS '기존 FileStoragePort가 반환한 외부 비노출 첨부 참조 JSON 배열. TeachingEvaluationAchievementService 저장 시 갱신한다.';

CREATE INDEX IF NOT EXISTS idx_teaching_evaluation_achievements_dynamic_fields
    ON teaching_evaluation_achievements USING gin (dynamic_fields);
