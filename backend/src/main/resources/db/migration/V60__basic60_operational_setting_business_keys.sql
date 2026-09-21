-- Align BASIC-60 setting identities with the classification fields required by the save APIs.
ALTER TABLE participation_allocation_rate_settings
    DROP CONSTRAINT uq_participation_alloc_rate_settings_key,
    ADD CONSTRAINT uq_participation_alloc_rate_settings_key
        UNIQUE (rule_version_id, target_scope, area_code, item_code, evaluation_year, element_code, management_item_code, researcher_count, participation_type, effective_start_date, effective_end_date);

ALTER TABLE management_item_evaluation_score_settings
    DROP CONSTRAINT uq_mgmt_item_eval_score_settings_key,
    ADD CONSTRAINT uq_mgmt_item_eval_score_settings_key
        UNIQUE (rule_version_id, target_scope, area_code, item_code, evaluation_year, element_code, management_item_code, organization_code, effective_start_date, effective_end_date);