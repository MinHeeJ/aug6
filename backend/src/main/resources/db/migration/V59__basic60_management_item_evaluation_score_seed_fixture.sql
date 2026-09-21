-- B60-SEED-003: normal active college score, upper-bound score, and inactive confirmed-impact boundary.
INSERT INTO management_item_evaluation_score_settings (
    rule_version_id, target_scope, area_code, item_code, evaluation_year, element_code,
    management_item_code, organization_code, organization_name, evaluation_score, max_score,
    sort_order, active_yn, effective_start_date, effective_end_date, evaluation_confirmed_yn,
    change_reason, created_by, updated_by
)
SELECT
    rv.rule_version_id, seed.target_scope, seed.area_code, seed.item_code, seed.evaluation_year,
    seed.element_code, seed.management_item_code, seed.organization_code, seed.organization_name,
    seed.evaluation_score, seed.max_score, seed.sort_order, seed.active_yn,
    seed.effective_start_date, seed.effective_end_date, seed.evaluation_confirmed_yn,
    'B60-SEED-003 관리항목별 평가점수 fixture', 1, 1
FROM evaluation_rule_versions rv
JOIN (VALUES
    ('COLLEGE_EDU', 'EDUCATION', 'LECTURE', '2026', 'COURSE_GROUP', 'ATTENDANCE', 'COL-EDU', '사범대학', 12.50, 20.00, 1, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
    ('COLLEGE_EDU', 'EDUCATION', 'LECTURE', '2026', 'COURSE_GROUP', 'SYLLABUS', 'COL-EDU', '사범대학', 10.00, 10.00, 2, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
    ('COLLEGE_RESEARCH', 'EDUCATION', 'LECTURE', '2026', 'COURSE_GROUP', 'LOCKED_SCORE', 'COL-RES', '연구대학', 3.00, 3.00, 99, 'N', DATE '2026-01-01', DATE '2026-12-31', 'Y')
) AS seed(target_scope, area_code, item_code, evaluation_year, element_code, management_item_code,
          organization_code, organization_name, evaluation_score, max_score, sort_order, active_yn,
          effective_start_date, effective_end_date, evaluation_confirmed_yn) ON TRUE
WHERE rv.version_code = 'B60-DRAFT-2026'
ON CONFLICT (rule_version_id, target_scope, management_item_code, organization_code,
             effective_start_date, effective_end_date)
DO UPDATE SET evaluation_score = EXCLUDED.evaluation_score,
              max_score = EXCLUDED.max_score,
              sort_order = EXCLUDED.sort_order,
              active_yn = EXCLUDED.active_yn,
              change_reason = EXCLUDED.change_reason,
              updated_by = EXCLUDED.updated_by,
              updated_at = CURRENT_TIMESTAMP;
