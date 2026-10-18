-- BASIC-69 Phase 3의 B60-SEED-002: 참여구분별 배분율 설정 화면 검증용 증분 fixture.
-- 논문 단독저자, 공동저자 2인, 공동저자 3인 이상 매트릭스를 기존 작성중 규정 버전에 제공한다.

INSERT INTO participation_allocation_rate_settings (
    rule_version_id,
    target_scope,
    area_code,
    item_code,
    evaluation_year,
    element_code,
    management_item_code,
    researcher_count,
    participation_type,
    allocation_rate,
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
    fixture.researcher_count,
    fixture.participation_type,
    fixture.allocation_rate,
    fixture.active_yn,
    fixture.effective_start_date,
    fixture.effective_end_date,
    fixture.evaluation_confirmed_yn,
    'B60-SEED-002 BASIC-69 참여구분별 배분율 fixture',
    1,
    1
FROM evaluation_rule_versions rule_version
CROSS JOIN (
    VALUES
        ('COLLEGE_EDU', 'RESEARCH', 'PAPER', '2026', 'AUTHORSHIP', 'B60_SEED_RATE_MATRIX', 1, 'SOLE', 1.0000, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
        ('COLLEGE_EDU', 'RESEARCH', 'PAPER', '2026', 'AUTHORSHIP', 'B60_SEED_RATE_MATRIX', 2, 'LEAD', 0.7000, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
        ('COLLEGE_EDU', 'RESEARCH', 'PAPER', '2026', 'AUTHORSHIP', 'B60_SEED_RATE_MATRIX', 3, 'CO', 0.5000, 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N')
) AS fixture(
    target_scope,
    area_code,
    item_code,
    evaluation_year,
    element_code,
    management_item_code,
    researcher_count,
    participation_type,
    allocation_rate,
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
    researcher_count,
    participation_type,
    effective_start_date,
    effective_end_date
) DO NOTHING;
