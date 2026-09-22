-- BASIC-70은 관리항목의 단순 입력 가능 여부 대신 실제 교수 입력 가능 부분을 운영값으로 저장한다.
ALTER TABLE evaluation_element_management_item_settings
    ADD COLUMN IF NOT EXISTS teacher_editable_part varchar(100);

UPDATE evaluation_element_management_item_settings
SET teacher_editable_part = CASE teacher_editable_yn
    WHEN 'Y' THEN 'SELF_REPORT'
    ELSE 'NONE'
END
WHERE teacher_editable_part IS NULL;

ALTER TABLE evaluation_element_management_item_settings
    ALTER COLUMN teacher_editable_part SET NOT NULL;

COMMENT ON COLUMN evaluation_element_management_item_settings.teacher_editable_part IS '교수 입력 가능 부분 코드 또는 명칭';
