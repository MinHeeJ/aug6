-- BASIC-34 demo data. This migration is idempotent and keeps the BASIC-33 hierarchy usable by BASIC-34.

INSERT INTO evaluation_rule_versions (
    version_code, effective_start_date, effective_end_date, version_status,
    classification_rule_version_id, change_reason, created_by, updated_by
)
SELECT v.version_code, v.effective_start_date, v.effective_end_date, v.version_status,
       b33.rule_version_id, 'BASIC-34 데모 예시데이터', 1, 1
FROM (
    VALUES
        ('B34-DEMO-2026-01', DATE '2026-01-01', DATE '2026-12-31', 'CONFIRMED'),
        ('B34-DEMO-2026-02', DATE '2026-01-01', DATE '2026-12-31', 'DRAFT'),
        ('B34-DEMO-2026-03', DATE '2026-01-01', DATE '2026-12-31', 'CONFIRMED'),
        ('B34-DEMO-2026-04', DATE '2026-01-01', DATE '2026-12-31', 'DRAFT'),
        ('B34-DEMO-2026-05', DATE '2026-01-01', DATE '2026-12-31', 'DISCARDED')
) AS v(version_code, effective_start_date, effective_end_date, version_status)
JOIN evaluation_rule_versions b33 ON b33.version_code = 'B33-CONFIRMED-2026'
ON CONFLICT (version_code) DO UPDATE SET
    effective_start_date = EXCLUDED.effective_start_date,
    effective_end_date = EXCLUDED.effective_end_date,
    version_status = EXCLUDED.version_status,
    classification_rule_version_id = EXCLUDED.classification_rule_version_id,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO evaluation_areas (
    rule_version_id, area_code, area_name, sort_order, active_yn,
    period_apply_method, change_reason, created_by, updated_by
)
SELECT rv.rule_version_id, v.area_code, v.area_name, v.sort_order, 'Y',
       v.period_apply_method, 'BASIC-34 데모용 BASIC-33 평가영역', 1, 1
FROM evaluation_rule_versions rv
CROSS JOIN (
    VALUES
        ('B34-AREA-01', '교육 및 강의', 1, 'EVALUATION_PERIOD'),
        ('B34-AREA-02', '연구 및 창작', 2, 'EVALUATION_PERIOD'),
        ('B34-AREA-03', '봉사 및 산학협력', 3, 'EVALUATION_PERIOD'),
        ('B34-AREA-04', '국제화 활동', 4, 'EVALUATION_PERIOD'),
        ('B34-AREA-05', '학생 지도', 5, 'EVALUATION_PERIOD')
) AS v(area_code, area_name, sort_order, period_apply_method)
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (rule_version_id, area_code) DO UPDATE SET
    area_name = EXCLUDED.area_name,
    sort_order = EXCLUDED.sort_order,
    active_yn = EXCLUDED.active_yn,
    period_apply_method = EXCLUDED.period_apply_method,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO evaluation_items (
    area_id, item_code, item_name, parent_item_code, sort_order, active_yn,
    score_apply_method, change_reason, created_by, updated_by
)
SELECT a.area_id, v.item_code, v.item_name, NULL, 1, 'Y',
       'RULE_BASED', 'BASIC-34 데모용 BASIC-33 평가항목', 1, 1
FROM evaluation_areas a
JOIN evaluation_rule_versions rv ON rv.rule_version_id = a.rule_version_id
JOIN (
    VALUES
        ('B34-AREA-01', 'B34-ITEM-01', '강의계획 및 운영'),
        ('B34-AREA-02', 'B34-ITEM-02', '연구성과 및 논문'),
        ('B34-AREA-03', 'B34-ITEM-03', '지역사회 협력'),
        ('B34-AREA-04', 'B34-ITEM-04', '국제 공동활동'),
        ('B34-AREA-05', 'B34-ITEM-05', '학생상담 및 지도')
) AS v(area_code, item_code, item_name) ON v.area_code = a.area_code
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (area_id, item_code) DO UPDATE SET
    item_name = EXCLUDED.item_name,
    parent_item_code = EXCLUDED.parent_item_code,
    sort_order = EXCLUDED.sort_order,
    active_yn = EXCLUDED.active_yn,
    score_apply_method = EXCLUDED.score_apply_method,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO evaluation_elements (
    item_id, evaluation_year, element_code, element_name, sort_order, active_yn,
    change_reason, created_by, updated_by
)
SELECT i.item_id, '2026', v.element_code, v.element_name, 1, 'Y',
       'BASIC-34 데모용 BASIC-33 평가요소', 1, 1
FROM evaluation_items i
JOIN evaluation_areas a ON a.area_id = i.area_id
JOIN evaluation_rule_versions rv ON rv.rule_version_id = a.rule_version_id
JOIN (
    VALUES
        ('B34-ITEM-01', 'B34-ELEMENT-01', '강의 운영 품질'),
        ('B34-ITEM-02', 'B34-ELEMENT-02', '연구 실적 기여도'),
        ('B34-ITEM-03', 'B34-ELEMENT-03', '협력 프로그램 참여'),
        ('B34-ITEM-04', 'B34-ELEMENT-04', '국제 공동연구 참여'),
        ('B34-ITEM-05', 'B34-ELEMENT-05', '학생 지도 실적')
) AS v(item_code, element_code, element_name) ON v.item_code = i.item_code
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (item_id, evaluation_year, element_code) DO UPDATE SET
    element_name = EXCLUDED.element_name,
    sort_order = EXCLUDED.sort_order,
    active_yn = EXCLUDED.active_yn,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO evaluation_management_items (
    element_id, management_item_code, management_item_name, sort_order, active_yn,
    teacher_editable_yn, required_yn, data_type, change_reason, created_by, updated_by
)
SELECT e.element_id, v.management_item_code, v.management_item_name, 1, 'Y',
       'Y', 'Y', v.data_type, 'BASIC-34 데모용 BASIC-33 관리항목', 1, 1
FROM evaluation_elements e
JOIN evaluation_items i ON i.item_id = e.item_id
JOIN evaluation_areas a ON a.area_id = i.area_id
JOIN evaluation_rule_versions rv ON rv.rule_version_id = a.rule_version_id
JOIN (
    VALUES
        ('B34-ELEMENT-01', 'B34-MGMT-01', '강의평가 평균점수', 'NUMBER'),
        ('B34-ELEMENT-02', 'B34-MGMT-02', '대표 연구성과 유형', 'CODE'),
        ('B34-ELEMENT-03', 'B34-MGMT-03', '협력기관 수', 'NUMBER'),
        ('B34-ELEMENT-04', 'B34-MGMT-04', '공동연구 국가', 'TEXT'),
        ('B34-ELEMENT-05', 'B34-MGMT-05', '상담 학생 수', 'NUMBER')
) AS v(element_code, management_item_code, management_item_name, data_type)
    ON v.element_code = e.element_code
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (element_id, management_item_code) DO UPDATE SET
    management_item_name = EXCLUDED.management_item_name,
    sort_order = EXCLUDED.sort_order,
    active_yn = EXCLUDED.active_yn,
    teacher_editable_yn = EXCLUDED.teacher_editable_yn,
    required_yn = EXCLUDED.required_yn,
    data_type = EXCLUDED.data_type,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO area_element_system_settings (
    area_id, item_id, element_id, target_scope, active_yn,
    change_reason, created_by, updated_by
)
SELECT a.area_id, i.item_id, e.element_id, v.target_scope, 'Y',
       'BASIC-34 데모용 영역별 평가요소 체계', 1, 1
FROM evaluation_areas a
JOIN evaluation_items i ON i.area_id = a.area_id
JOIN evaluation_elements e ON e.item_id = i.item_id
JOIN evaluation_rule_versions rv ON rv.rule_version_id = a.rule_version_id
JOIN (
    VALUES
        ('B34-AREA-01', 'B34-ITEM-01', 'B34-ELEMENT-01', 'KNUE'),
        ('B34-AREA-02', 'B34-ITEM-02', 'B34-ELEMENT-02', 'KNUE-COL-EDU'),
        ('B34-AREA-03', 'B34-ITEM-03', 'B34-ELEMENT-03', 'KNUE-DEPT-COMP'),
        ('B34-AREA-04', 'B34-ITEM-04', 'B34-ELEMENT-04', 'KNUE'),
        ('B34-AREA-05', 'B34-ITEM-05', 'B34-ELEMENT-05', 'KNUE-DEPT-COMP')
) AS v(area_code, item_code, element_code, target_scope)
    ON v.area_code = a.area_code AND v.item_code = i.item_code AND v.element_code = e.element_code
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (area_id, item_id, element_id, target_scope) DO UPDATE SET
    active_yn = EXCLUDED.active_yn,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO evaluation_score_rules (
    rule_version_id, management_item_id, organization_code, evaluation_year,
    base_score, max_score, effective_start_date, effective_end_date, active_yn,
    change_reason, created_by, updated_by
)
SELECT rv.rule_version_id, mi.management_item_id, v.organization_code, '2026',
       v.base_score, v.max_score, DATE '2026-01-01', DATE '2026-12-31', 'Y',
       'BASIC-34 평가점수 데모 예시데이터', 1, 1
FROM evaluation_rule_versions rv
JOIN evaluation_management_items mi ON mi.management_item_code IN
    ('B34-MGMT-01', 'B34-MGMT-02', 'B34-MGMT-03', 'B34-MGMT-04', 'B34-MGMT-05')
JOIN (
    VALUES
        ('B34-MGMT-01', 'KNUE-COL-EDU', 10.00::numeric, 20.00::numeric),
        ('B34-MGMT-02', 'KNUE-COL-EDU', 15.00::numeric, 30.00::numeric),
        ('B34-MGMT-03', 'KNUE-DEPT-COMP', 5.00::numeric, 10.00::numeric),
        ('B34-MGMT-04', 'KNUE', 20.00::numeric, 40.00::numeric),
        ('B34-MGMT-05', 'KNUE-DEPT-COMP', 8.00::numeric, 16.00::numeric)
) AS v(management_item_code, organization_code, base_score, max_score)
    ON v.management_item_code = mi.management_item_code
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (rule_version_id, management_item_id, organization_code, evaluation_year, effective_start_date, effective_end_date)
DO UPDATE SET
    base_score = EXCLUDED.base_score,
    max_score = EXCLUDED.max_score,
    active_yn = EXCLUDED.active_yn,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO participation_rate_rules (
    rule_version_id, management_item_id, researcher_count, participation_type,
    distribution_rate, effective_start_date, effective_end_date, active_yn,
    change_reason, created_by, updated_by
)
SELECT rv.rule_version_id, mi.management_item_id, v.researcher_count, v.participation_type,
       v.distribution_rate, DATE '2026-01-01', DATE '2026-12-31', 'Y',
       'BASIC-34 참여구분·배분율 데모 예시데이터', 1, 1
FROM evaluation_rule_versions rv
JOIN evaluation_management_items mi ON mi.management_item_code = 'B34-MGMT-01'
CROSS JOIN (
    VALUES
        (1, 'SOLE', 1.0000::numeric),
        (2, 'LEAD', 0.6000::numeric),
        (3, 'COAUTHOR', 0.4000::numeric),
        (4, 'ASSIST', 0.2500::numeric),
        (5, 'JOINT', 0.2000::numeric)
) AS v(researcher_count, participation_type, distribution_rate)
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (rule_version_id, management_item_id, researcher_count, participation_type, effective_start_date, effective_end_date)
DO UPDATE SET
    distribution_rate = EXCLUDED.distribution_rate,
    active_yn = EXCLUDED.active_yn,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO calculation_formula_versions (
    rule_version_id, formula_code, calculation_type, variable_definition,
    rounding_rule, lower_bound_score, upper_bound_score, evaluation_year,
    effective_start_date, effective_end_date, active_yn, change_reason, created_by, updated_by
)
SELECT rv.rule_version_id, v.formula_code, v.calculation_type, v.variable_definition::jsonb,
       v.rounding_rule, v.lower_bound_score, v.upper_bound_score, '2026',
       DATE '2026-01-01', DATE '2026-12-31', 'Y',
       'BASIC-34 계산식 데모 예시데이터', 1, 1
FROM evaluation_rule_versions rv
CROSS JOIN (
    VALUES
        ('B34-FORMULA-01', 'FIXED_SCORE', '{"variables":["baseScore"]}', 'HALF_UP', 0.00::numeric, 100.00::numeric),
        ('B34-FORMULA-02', 'DISTRIBUTION_RATE', '{"variables":["baseScore","distributionRate"]}', 'HALF_UP', 0.00::numeric, 100.00::numeric),
        ('B34-FORMULA-03', 'CAP', '{"variables":["calculatedScore","maxScore"]}', 'DOWN', 0.00::numeric, 50.00::numeric),
        ('B34-FORMULA-04', 'LADDER', '{"variables":["researcherCount","participationRate"]}', 'HALF_UP', 0.00::numeric, 100.00::numeric),
        ('B34-FORMULA-05', 'FIXED_SCORE', '{"variables":["manualAdjustment"]}', 'HALF_EVEN', 0.00::numeric, 200.00::numeric)
) AS v(formula_code, calculation_type, variable_definition, rounding_rule, lower_bound_score, upper_bound_score)
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (rule_version_id, formula_code, evaluation_year, effective_start_date, effective_end_date)
DO UPDATE SET
    calculation_type = EXCLUDED.calculation_type,
    variable_definition = EXCLUDED.variable_definition,
    rounding_rule = EXCLUDED.rounding_rule,
    lower_bound_score = EXCLUDED.lower_bound_score,
    upper_bound_score = EXCLUDED.upper_bound_score,
    active_yn = EXCLUDED.active_yn,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO evaluation_rule_sets (
    rule_version_id, target_scope, rule_set_name, rule_set_status, active_yn,
    effective_start_date, effective_end_date, change_reason, created_by, updated_by
)
SELECT rv.rule_version_id, v.target_scope, v.rule_set_name, v.rule_set_status, 'Y',
       DATE '2026-01-01', DATE '2026-12-31', 'BASIC-34 통합 평가규칙 데모 예시데이터', 1, 1
FROM evaluation_rule_versions rv
CROSS JOIN (
    VALUES
        ('KNUE', '교수업적평가 기본 규칙', 'CONFIRMED'),
        ('KNUE-COL-EDU', '교육과학대학 평가 규칙', 'DRAFT'),
        ('KNUE-DEPT-COMP', '컴퓨터교육과 평가 규칙', 'CONFIRMED'),
        ('RESEARCH', '연구성과 평가 규칙', 'DRAFT'),
        ('SERVICE', '봉사활동 평가 규칙', 'DISCARDED')
) AS v(target_scope, rule_set_name, rule_set_status)
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (rule_version_id, target_scope, rule_set_name, effective_start_date, effective_end_date)
DO UPDATE SET
    rule_set_status = EXCLUDED.rule_set_status,
    active_yn = EXCLUDED.active_yn,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO journal_indexing_infos (
    rule_version_id, issn, journal_name, indexing_type, publication_country,
    valid_start_date, valid_end_date, source_name, source_updated_at, active_yn,
    change_reason, created_by, updated_by
)
SELECT rv.rule_version_id, v.issn, v.journal_name, v.indexing_type, v.publication_country,
       DATE '2026-01-01', DATE '2026-12-31', 'BASIC-34 DEMO CATALOG',
       TIMESTAMP '2026-01-15 09:00:00', 'Y', 'BASIC-34 학술지 등재정보 데모 예시데이터', 1, 1
FROM evaluation_rule_versions rv
CROSS JOIN (
    VALUES
        ('2093-1234', '교육공학연구', 'KCI', '대한민국'),
        ('2093-2345', '한국컴퓨터교육학회지', 'KCI', '대한민국'),
        ('1234-5678', 'International Journal of Education', 'INTERNATIONAL', '미국'),
        ('8765-4321', '교육정책연구 후보지', 'CANDIDATE', '대한민국'),
        ('1357-2468', '융합교육과학저널', 'OTHER', '대한민국')
) AS v(issn, journal_name, indexing_type, publication_country)
WHERE rv.version_code = 'B34-DEMO-2026-01'
ON CONFLICT (rule_version_id, issn, valid_start_date, valid_end_date)
DO UPDATE SET
    journal_name = EXCLUDED.journal_name,
    indexing_type = EXCLUDED.indexing_type,
    publication_country = EXCLUDED.publication_country,
    source_name = EXCLUDED.source_name,
    source_updated_at = EXCLUDED.source_updated_at,
    active_yn = EXCLUDED.active_yn,
    change_reason = EXCLUDED.change_reason,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;
