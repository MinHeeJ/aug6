-- 승인 데이터 모델의 정상·기간 경계·타인/확정 차이 사례를 DB에 적재한다.
-- 비밀번호/로그인 가능한 자격증명은 추가하지 않는다. R07 사례 계정은 기존 사용자 원장의 비활성 credential을 쓴다.
INSERT INTO users (
    login_id, password_hash, system_use_yn, status, created_by, updated_by, change_reason
)
SELECT
    'education-department-operator',
    'sha256:disabled',
    'Y',
    'ACTIVE',
    u.user_id,
    u.user_id,
    '교육영역 R07 범위 검증용 시드 (로그인 비밀번호 미설정)'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (login_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_code, assignment_type, approver_user_id, status, change_reason)
SELECT
    u.user_id,
    'R07',
    'MANUAL',
    a.user_id,
    'ACTIVE',
    '교육영역 실적부서 검토용 시드'
FROM users u
CROSS JOIN users a
WHERE u.login_id = 'education-department-operator'
  AND a.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles r
      WHERE r.user_id = u.user_id AND r.role_code = 'R07' AND r.status = 'ACTIVE'
  );

INSERT INTO organization_user_mappings (
    organization_code, user_id, mapping_type, effective_start_date, status
)
SELECT
    o.organization_code,
    u.user_id,
    'ORGANIZATION',
    DATE '2026-01-01',
    'ACTIVE'
FROM users u
CROSS JOIN organizations o
WHERE u.login_id = 'education-department-operator'
  AND o.organization_code = 'KNUE-DEPT-COMP'
  AND NOT EXISTS (
      SELECT 1
      FROM organization_user_mappings m
      WHERE m.user_id = u.user_id AND m.organization_code = o.organization_code AND m.status = 'ACTIVE'
  );

INSERT INTO evaluation_organization_mappings (
    user_id, organization_code, business_type, data_scope, change_reason, created_by, updated_by
)
SELECT
    u.user_id,
    o.organization_code,
    'FACULTY_ACHIEVEMENT',
    'BUSINESS',
    '교육영역 R07 시드의 허용 소속 (일괄 생성·삭제 정책 승인 아님)',
    a.user_id,
    a.user_id
FROM users u
CROSS JOIN organizations o
CROSS JOIN users a
WHERE u.login_id = 'education-department-operator'
  AND o.organization_code = 'KNUE-DEPT-COMP'
  AND a.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM evaluation_organization_mappings m
      WHERE m.user_id = u.user_id
        AND m.organization_code = o.organization_code
        AND m.business_type = 'FACULTY_ACHIEVEMENT'
  );

-- 기존 분류체계에 업무 관리항목만 보강한다. 규정/영역/항목의 기존 설정값은 덮어쓰지 않는다.
INSERT INTO evaluation_areas (
    rule_version_id, area_code, area_name, sort_order, active_yn, period_apply_method,
    change_reason, created_by, updated_by
)
SELECT
    v.rule_version_id,
    'EDUCATION',
    '교육영역',
    1,
    'Y',
    'AREA_PERIOD',
    '교육영역 후반 관리항목 시드',
    u.user_id,
    u.user_id
FROM evaluation_rule_versions v
CROSS JOIN users u
WHERE v.version_code = 'B33-CONFIRMED-2026' AND u.login_id = 'admin'
ON CONFLICT (rule_version_id, area_code) DO NOTHING;

INSERT INTO evaluation_items (
    area_id, item_code, item_name, sort_order, active_yn, score_apply_method,
    change_reason, created_by, updated_by
)
SELECT
    a.area_id,
    'EDUCATION_ACHIEVEMENT',
    '교육영역 실적',
    1,
    'Y',
    'MANAGEMENT_ITEM',
    '교육영역 후반 관리항목 시드 (점수 규칙 추가 없음)',
    u.user_id,
    u.user_id
FROM evaluation_areas a
JOIN evaluation_rule_versions v ON v.rule_version_id = a.rule_version_id
CROSS JOIN users u
WHERE v.version_code = 'B33-CONFIRMED-2026'
  AND a.area_code = 'EDUCATION' AND u.login_id = 'admin'
ON CONFLICT (area_id, item_code) DO NOTHING;

INSERT INTO evaluation_elements (
    item_id, evaluation_year, element_code, element_name, sort_order, active_yn,
    change_reason, created_by, updated_by
)
SELECT
    i.item_id,
    '2026',
    s.element_code,
    s.element_name,
    s.sort_order,
    'Y',
    '교육영역 후반 업무 관리항목 시드',
    u.user_id,
    u.user_id
FROM evaluation_items i
JOIN evaluation_areas a ON a.area_id = i.area_id
JOIN evaluation_rule_versions v ON v.rule_version_id = a.rule_version_id
CROSS JOIN users u
CROSS JOIN (
    VALUES
        ('FR-029', '취업률 제고', 1),
        ('FR-030', '강좌 개설·운영', 2),
        ('FR-031', '강의개선', 3),
        ('FR-032', '취업률 실적', 4)
) AS s(element_code, element_name, sort_order)
WHERE v.version_code = 'B33-CONFIRMED-2026'
  AND a.area_code = 'EDUCATION' AND i.item_code = 'EDUCATION_ACHIEVEMENT' AND u.login_id = 'admin'
ON CONFLICT (item_id, evaluation_year, element_code) DO NOTHING;

INSERT INTO evaluation_management_items (
    element_id, management_item_code, management_item_name, sort_order, active_yn,
    teacher_editable_yn, required_yn, data_type, change_reason, created_by, updated_by
)
SELECT
    e.element_id,
    e.element_code,
    e.element_name,
    1,
    'Y',
    'Y',
    'Y',
    'TEXT',
    '교육영역 후반 업무 입력 관리항목 시드',
    u.user_id,
    u.user_id
FROM evaluation_elements e
JOIN evaluation_items i ON i.item_id = e.item_id
JOIN evaluation_areas a ON a.area_id = i.area_id
JOIN evaluation_rule_versions v ON v.rule_version_id = a.rule_version_id
CROSS JOIN users u
WHERE v.version_code = 'B33-CONFIRMED-2026'
  AND a.area_code = 'EDUCATION' AND i.item_code = 'EDUCATION_ACHIEVEMENT'
  AND e.evaluation_year = '2026'
  AND e.element_code IN ('FR-029', 'FR-030', 'FR-031', 'FR-032')
  AND u.login_id = 'admin'
ON CONFLICT (element_id, management_item_code) DO NOTHING;

INSERT INTO education_achievements (
    achievement_type, management_no, teacher_user_id, organization_code, evaluation_year,
    management_item_code, achievement_date, achievement_name, achievement_detail,
    achievement_status, created_by, updated_by, request_id
)
SELECT
    t.achievement_type,
    CONCAT('EDU-SEED-', t.achievement_type, '-', c.case_no),
    u.user_id,
    p.organization_code,
    '2026',
    t.achievement_type,
    c.achievement_date,
    CONCAT('[시드] ', t.achievement_name, ' ', c.case_name),
    jsonb_build_object('fixture', true, 'case', c.case_name)::text,
    c.achievement_status,
    u.user_id,
    u.user_id,
    CONCAT('EDU-SEED-', t.achievement_type, '-', c.case_no)
FROM (
    VALUES
        ('FR-029', '취업률 제고'),
        ('FR-030', '강좌 개설·운영'),
        ('FR-031', '강의개선'),
        ('FR-032', '취업률 실적')
) AS t(achievement_type, achievement_name)
CROSS JOIN (
    VALUES
        ('001', 'professor1', DATE '2026-04-10', 'DRAFT', '정상 작성중'),
        ('002', 'professor2', DATE '2025-12-31', 'SUBMITTED', '기간 외 경고·타인'),
        ('003', 'business-owner', DATE '2026-04-12', 'EVALUATION_CONFIRMED', '확정 변경차단')
) AS c(case_no, login_id, achievement_date, achievement_status, case_name)
JOIN users u ON u.login_id = c.login_id
JOIN korus_personnel_snapshots p ON p.employee_no = u.employee_no
ON CONFLICT (management_no) DO NOTHING;

INSERT INTO employment_rate_improvement_achievement_details (
    achievement_id, special_lecture_start_date, special_lecture_end_date, mock_exam_question_period,
    created_by, updated_by
)
SELECT
    a.achievement_id,
    a.achievement_date,
    a.achievement_date,
    '[시드] 2026년 모의평가 출제기간 검증',
    a.created_by,
    a.updated_by
FROM education_achievements a
WHERE a.achievement_type = 'FR-029' AND a.management_no LIKE 'EDU-SEED-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO course_operation_achievement_details (achievement_id, performance_detail, created_by, updated_by)
SELECT
    a.achievement_id,
    a.achievement_name,
    a.created_by,
    a.updated_by
FROM education_achievements a
WHERE a.achievement_type = 'FR-030' AND a.management_no LIKE 'EDU-SEED-%'
ON CONFLICT (achievement_id) DO NOTHING;

INSERT INTO lecture_improvement_achievement_details (
    achievement_id, performance_content, academic_year, semester_code, created_by, updated_by
)
SELECT
    a.achievement_id,
    a.achievement_name,
    '2025',
    CASE WHEN a.achievement_status = 'SUBMITTED' THEN '2' ELSE '1' END,
    a.created_by,
    a.updated_by
FROM education_achievements a
WHERE a.achievement_type = 'FR-031' AND a.management_no LIKE 'EDU-SEED-%'
ON CONFLICT (achievement_id) DO NOTHING;

-- 시드 상태까지의 허용 경로를 모두 보존하여 확정 상태를 최초 CREATE로 위장하지 않는다.
INSERT INTO education_achievement_status_histories (
    achievement_type, achievement_id, previous_status, next_status, action_type,
    opinion, processed_by, processed_at, request_id
)
SELECT
    a.achievement_type,
    a.achievement_id,
    s.previous_status,
    s.next_status,
    s.action_type,
    '[시드] 교육영역 상태 경로 검증',
    a.created_by,
    a.created_at + s.step_no * INTERVAL '1 second',
    a.request_id
FROM education_achievements a
CROSS JOIN (
    VALUES
        (0, NULL::varchar(30), 'DRAFT', 'CREATE'),
        (1, 'DRAFT', 'SUBMITTED', 'SUBMIT'),
        (2, 'SUBMITTED', 'DEPARTMENT_CONFIRMED', 'DEPARTMENT_CONFIRM'),
        (3, 'DEPARTMENT_CONFIRMED', 'CERTIFIED', 'CERTIFY'),
        (4, 'CERTIFIED', 'EVALUATION_CONFIRMED', 'EVALUATION_CONFIRM')
) AS s(step_no, previous_status, next_status, action_type)
WHERE a.management_no LIKE 'EDU-SEED-%'
  AND (
      s.step_no = 0
      OR (a.achievement_status = 'SUBMITTED' AND s.step_no = 1)
      OR a.achievement_status = 'EVALUATION_CONFIRMED'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM education_achievement_status_histories h
      WHERE h.achievement_type = a.achievement_type
        AND h.achievement_id = a.achievement_id AND h.action_type = s.action_type
  );

INSERT INTO data_change_histories (
    target_business, target_key, change_type, field_name, before_value, after_value,
    changed_by, change_reason, request_id
)
SELECT
    'education_achievements',
    a.achievement_id::text,
    'CREATE',
    'snapshot',
    NULL,
    to_jsonb(a)::text,
    a.created_by,
    '[시드] 교육영역 최초 입력 snapshot',
    a.request_id
FROM education_achievements a
WHERE a.management_no LIKE 'EDU-SEED-%'
  AND NOT EXISTS (
      SELECT 1
      FROM data_change_histories h
      WHERE h.target_business = 'education_achievements'
        AND h.target_key = a.achievement_id::text AND h.change_type = 'CREATE'
  );

-- 세 작업과 대상별 결과는 정책 승인 전 읽기/범위 검증용이다. 실적 생성·삭제는 실행하지 않는다.
INSERT INTO employment_rate_batch_jobs (
    batch_job_id, evaluation_year, target_condition_json, action_type, requester_user_id,
    organization_code, status, total_count, processed_count, unprocessed_count, seed_yn,
    request_id, created_by, updated_by
)
SELECT
    s.batch_job_id,
    '2026',
    jsonb_build_object('fixture', true, 'organizationCode', m.organization_code, 'policyApproved', false),
    s.action_type,
    u.user_id,
    m.organization_code,
    'SEED',
    3,
    0,
    3,
    'Y',
    s.batch_job_id,
    u.user_id,
    u.user_id
FROM users u
JOIN organization_user_mappings m ON m.user_id = u.user_id
CROSS JOIN (
    VALUES
        ('EDU-SEED-JOB-001', 'GENERATE'),
        ('EDU-SEED-JOB-002', 'DELETE'),
        ('EDU-SEED-JOB-003', 'GENERATE')
) AS s(batch_job_id, action_type)
WHERE u.login_id = 'education-department-operator'
  AND m.mapping_type = 'ORGANIZATION' AND m.status = 'ACTIVE'
  AND m.organization_code = 'KNUE-DEPT-COMP'
ON CONFLICT (batch_job_id) DO NOTHING;

INSERT INTO employment_rate_batch_job_items (
    batch_job_id, target_user_id, processed_yn, unprocessed_reason, created_by, updated_by
)
SELECT
    j.batch_job_id,
    u.user_id,
    'N',
    '[시드] OQ-83-02/03 정책 미승인. 실제 실행 결과가 아닌 검토용 사례',
    j.requester_user_id,
    j.requester_user_id
FROM employment_rate_batch_jobs j
CROSS JOIN users u
WHERE j.seed_yn = 'Y' AND j.batch_job_id LIKE 'EDU-SEED-JOB-%'
  AND u.login_id IN ('professor1', 'professor2', 'business-owner')
ON CONFLICT (batch_job_id, target_user_id) DO NOTHING;
