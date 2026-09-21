-- B60-SEED-002: single author, two-author collaboration, and three-or-more-author collaboration.
INSERT INTO participation_allocation_rate_settings (
    rule_version_id, target_scope, area_code, item_code, evaluation_year, element_code,
    management_item_code, researcher_count, participation_type, allocation_rate, active_yn,
    effective_start_date, effective_end_date, evaluation_confirmed_yn, change_reason, created_by, updated_by
)
SELECT
    rv.rule_version_id, seed.target_scope, seed.area_code, seed.item_code, seed.evaluation_year,
    seed.element_code, seed.management_item_code, seed.researcher_count,
    seed.participation_type, seed.allocation_rate, seed.active_yn,
    seed.effective_start_date, seed.effective_end_date, seed.evaluation_confirmed_yn,
    'B60-SEED-002 참여구분별 배분율 fixture', 1, 1
FROM evaluation_rule_versions rv
JOIN (VALUES
    ('COLLEGE_EDU', 'RESEARCH', 'PAPER', '2026', 'AUTHORSHIP', 'PAPER_SCORE', 1, 'SOLE', 1.0000, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
    ('COLLEGE_EDU', 'RESEARCH', 'PAPER', '2026', 'AUTHORSHIP', 'PAPER_SCORE', 2, 'CO', 0.5000, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
    ('COLLEGE_EDU', 'RESEARCH', 'PAPER', '2026', 'AUTHORSHIP', 'PAPER_SCORE', 3, 'LEAD', 0.7000, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N')
) AS seed(target_scope, area_code, item_code, evaluation_year, element_code, management_item_code,
          researcher_count, participation_type, allocation_rate, active_yn, effective_start_date,
          effective_end_date, evaluation_confirmed_yn) ON TRUE
WHERE rv.version_code = 'B60-DRAFT-2026'
ON CONFLICT (rule_version_id, target_scope, management_item_code, researcher_count, participation_type, effective_start_date, effective_end_date)
DO UPDATE SET allocation_rate = EXCLUDED.allocation_rate,
              active_yn = EXCLUDED.active_yn,
              change_reason = EXCLUDED.change_reason,
              updated_by = EXCLUDED.updated_by,
              updated_at = CURRENT_TIMESTAMP;
