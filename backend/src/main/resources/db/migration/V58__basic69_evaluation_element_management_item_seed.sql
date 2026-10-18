-- BASIC-69 Phase 2의 B60-SEED-001: 평가요소별 관리항목 설정 화면 검증용 증분 fixture.
-- 정상 사용, 교원 입력 불가 경계, 미사용 상태를 함께 제공하며 기존 규정 버전만 참조한다.

INSERT INTO evaluation_element_management_item_settings (
    rule_version_id,
    target_scope,
    area_code,
    item_code,
    evaluation_year,
    element_code,
    management_item_code,
    management_item_name,
    sort_order,
    active_yn,
    teacher_editable_yn,
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
    fixture.management_item_name,
    fixture.sort_order,
    fixture.active_yn,
    fixture.teacher_editable_yn,
    fixture.effective_start_date,
    fixture.effective_end_date,
    fixture.evaluation_confirmed_yn,
    'B60-SEED-001 BASIC-69 평가요소별 관리항목 fixture',
    1,
    1
FROM evaluation_rule_versions rule_version
CROSS JOIN (
    VALUES
        ('COLLEGE_EDU', 'EDUCATION', 'LECTURE', '2026', 'COURSE_GROUP', 'B60_SEED_NORMAL', 'B60-SEED 정상 관리항목', 10, 'Y', 'Y', DATE '2026-01-01', DATE '2026-12-31', 'N'),
        ('COLLEGE_EDU', 'EDUCATION', 'LECTURE', '2026', 'COURSE_GROUP', 'B60_SEED_BOUNDARY', 'B60-SEED 교원입력 불가 관리항목', 999, 'Y', 'N', DATE '2026-01-01', DATE '2026-12-31', 'N'),
        ('COLLEGE_EDU', 'EDUCATION', 'LECTURE', '2026', 'COURSE_GROUP', 'B60_SEED_INACTIVE', 'B60-SEED 미사용 관리항목', 1000, 'N', 'N', DATE '2026-01-01', DATE '2026-12-31', 'N')
) AS fixture(
    target_scope,
    area_code,
    item_code,
    evaluation_year,
    element_code,
    management_item_code,
    management_item_name,
    sort_order,
    active_yn,
    teacher_editable_yn,
    effective_start_date,
    effective_end_date,
    evaluation_confirmed_yn
)
WHERE rule_version.version_code = 'B60-DRAFT-2026'
ON CONFLICT (
    rule_version_id,
    target_scope,
    area_code,
    item_code,
    evaluation_year,
    element_code,
    management_item_code
) DO NOTHING;
