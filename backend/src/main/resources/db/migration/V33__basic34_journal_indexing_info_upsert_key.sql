DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_journal_indexing_infos_upsert_key'
    ) THEN
        ALTER TABLE journal_indexing_infos
            ADD CONSTRAINT uq_journal_indexing_infos_upsert_key
            UNIQUE (rule_version_id, issn, valid_start_date, valid_end_date);
    END IF;
END $$;

COMMENT ON CONSTRAINT uq_journal_indexing_infos_upsert_key ON journal_indexing_infos IS '학술지 등재정보 동일 규정버전·ISSN·유효기간 upsert 기준. 기간 중복은 ex_journal_indexing_infos_issn_period로 별도 차단한다.';
