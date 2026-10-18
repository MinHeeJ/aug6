-- BASIC-69 Phase 4의 B60-SEED-003: 관리항목별 평가점수 설정 화면 검증용 증분 fixture.
-- 정상, 상한점수 경계, 평가확정 영향 범위를 기존 작성중 규정 버전에 제공한다.

INSERT INTO management_item_evaluation_score_settings (
    rule_version_id,
    target_scope,
    area_code,
    item_code,
    evaluation_year,
    element_code,
    management_item_code,
    organization_code,
    organization_name,
    evaluation_score,
    max_score,
    sort_order,
    active_yn,
    effective_start_date,
    effective_end_date,
    evaluation_confirmed_yn,
    change_reason,
    created_by,
    updated_by
)
SELECT
    rule_version.rule_version_id,
    fixture.target_scope,
    fixture.area_code,
    fixture.item_code,
    fixture.evaluation_year,
    fixture.element_code,
    fixture.management_item_code,
    fixture.organization_code,
    fixture.organization_name,
    fixture.evaluation_score,
    fixture.max_score,
    fixture.sort_order,
    fixture.active_yn,
    fixture.effective_start_date,
    fixture.effective_end_date,
    fixture.evaluation_confirmed_yn,
    'B60-SEED-003 BASIC-69 관리항목별 평가점수 fixture',
    1,
    1
FROM evaluation_rule_versions rule_version
CROSS JOIN (
    VALUES
        ('COLLEGE_EDU', 'EDUCATION', 'LECTURE', '2026', 'COURSE_GROUP', 'B60_SEED_SCORE_LECTURE', 'COLLEGE-A', 'A대학', 92.50, 100.00, 1, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
        ('COLLEGE_EDU', 'EDUCATION', 'PRACTICE', '2026', 'COURSE_GROUP', 'B60_SEED_SCORE_MAXIMUM', 'COLLEGE-B', 'B대학', 100.00, 100.00, 2, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
        ('COLLEGE_RESEARCH', 'RESEARCH', 'PAPER', '2026', 'AUTHORSHIP', 'B60_SEED_SCORE_LOCKED', 'COLLEGE-C', 'C대학', 0.00, 0.00, 99, 'N', DATE '2026-01-01', DATE '2026-12-31', 'Y')
) AS fixture(
    target_scope,
    area_code,
    item_code,
    evaluation_year,
    element_code,
    management_item_code,
    organization_code,
    organization_name,
    evaluation_score,
    max_score,
    sort_order,
    active_yn,
    effective_start_date,
    effective_end_date,
    evaluation_confirmed_yn
)
WHERE rule_version.version_code = 'B60-DRAFT-2026'
ON CONFLICT (
    rule_version_id,
    target_scope,
    management_item_code,
    organization_code,
    effective_start_date,
    effective_end_date
) DO NOTHING;
