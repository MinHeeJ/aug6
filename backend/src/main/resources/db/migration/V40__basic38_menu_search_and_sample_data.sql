-- BASIC-38: 조회 화면 공백을 줄이기 위한 연관 예시 데이터 보강.
-- COMMENT ON: 신규 테이블은 만들지 않으며 기존 테이블 주석 계약은 기존 migration을 유지한다.

INSERT INTO korus_personnel_snapshots (employee_no, name, organization_code, rank_name, employment_status, position_name, retirement_date, status, appointment_id)
VALUES
('E1002', '김미래', 'KNUE-DEPT-COMP', '부교수', 'ACTIVE', '교원', NULL, 'ACTIVE', 'E1002-APPT'),
('E1003', '이하늘', 'KNUE-DEPT-COMP', '조교수', 'ACTIVE', '교원', NULL, 'ACTIVE', 'E1003-APPT')
ON CONFLICT (employee_no) DO UPDATE SET name = EXCLUDED.name, organization_code = EXCLUDED.organization_code, rank_name = EXCLUDED.rank_name, employment_status = EXCLUDED.employment_status, position_name = EXCLUDED.position_name, status = EXCLUDED.status, appointment_id = EXCLUDED.appointment_id, updated_at = CURRENT_TIMESTAMP;

INSERT INTO users (login_id, password_hash, employee_no, system_use_yn, status, created_by, updated_by, change_reason)
VALUES
('professor2', 'sha256:disabled', 'E1002', 'Y', 'ACTIVE', 1, 1, 'BASIC-38 연구자 프로필 예시 사용자'),
('business-owner', 'sha256:disabled', 'E1003', 'Y', 'ACTIVE', 1, 1, 'BASIC-38 교수지원과 예시 사용자')
ON CONFLICT (login_id) DO UPDATE SET employee_no = EXCLUDED.employee_no, system_use_yn = EXCLUDED.system_use_yn, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

INSERT INTO user_roles (user_id, role_code, assignment_type, approver_user_id, status, change_reason)
SELECT u.user_id, seed.role_code, 'MANUAL', admin.user_id, 'ACTIVE', 'BASIC-38 조회 검증용 역할'
FROM users u
JOIN users admin ON admin.login_id = 'admin'
JOIN (VALUES
    ('professor2', 'R01'),
    ('business-owner', 'R04')
) seed(login_id, role_code) ON seed.login_id = u.login_id
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.user_id
      AND ur.role_code = seed.role_code
      AND ur.status = 'ACTIVE'
);

INSERT INTO organization_user_mappings (organization_code, user_id, position_name, mapping_type, effective_start_date, status)
SELECT 'KNUE-DEPT-COMP', u.user_id, seed.position_name, seed.mapping_type, DATE '2026-01-01', 'ACTIVE'
FROM users u
JOIN (VALUES
    ('professor2', '교원', 'POSITION'),
    ('professor2', NULL, 'ORGANIZATION'),
    ('business-owner', '교수지원과', 'POSITION'),
    ('business-owner', NULL, 'ORGANIZATION')
) seed(login_id, position_name, mapping_type) ON seed.login_id = u.login_id
WHERE NOT EXISTS (
    SELECT 1 FROM organization_user_mappings existing
    WHERE existing.user_id = u.user_id
      AND existing.organization_code = 'KNUE-DEPT-COMP'
      AND existing.mapping_type = seed.mapping_type
      AND existing.status = 'ACTIVE'
);

INSERT INTO researcher_profiles (employee_no, contact, researcher_registration_no, external_provision_yn, information_public_yn, final_degree_type, degree_prerequisite_missing_yn, created_by, updated_by)
SELECT seed.employee_no, seed.contact, seed.researcher_registration_no, seed.external_provision_yn, seed.information_public_yn, seed.final_degree_type, seed.degree_prerequisite_missing_yn, admin.user_id, admin.user_id
FROM users admin
JOIN (VALUES
    ('E1002', '010-1002-0000', 'RID-1002', 'Y', 'Y', 'MASTER', 'N'),
    ('E1003', '010-1003-0000', 'RID-1003', 'N', 'Y', 'DOCTOR', 'Y')
) seed(employee_no, contact, researcher_registration_no, external_provision_yn, information_public_yn, final_degree_type, degree_prerequisite_missing_yn) ON TRUE
WHERE admin.login_id = 'admin'
ON CONFLICT (employee_no) DO UPDATE SET contact = EXCLUDED.contact, researcher_registration_no = EXCLUDED.researcher_registration_no, external_provision_yn = EXCLUDED.external_provision_yn, information_public_yn = EXCLUDED.information_public_yn, final_degree_type = EXCLUDED.final_degree_type, degree_prerequisite_missing_yn = EXCLUDED.degree_prerequisite_missing_yn, updated_at = CURRENT_TIMESTAMP, updated_by = EXCLUDED.updated_by;

INSERT INTO researcher_degrees (employee_no, degree_type, university_name, start_ym, acquired_ym, country_name, college_name, advisor_name, changed_by)
SELECT seed.employee_no, seed.degree_type, seed.university_name, seed.start_ym, seed.acquired_ym, '대한민국', seed.college_name, seed.advisor_name, admin.user_id
FROM users admin
JOIN (VALUES
    ('E1002', 'BACHELOR', '한국교원대학교', '201003', '201402', '교육학과', '박지도'),
    ('E1002', 'MASTER', '한국교원대학교', '201503', '201702', '교육대학원', '최지도'),
    ('E1003', 'DOCTOR', '한국교원대학교', '202003', '202402', '교육대학원', '정지도')
) seed(employee_no, degree_type, university_name, start_ym, acquired_ym, college_name, advisor_name) ON TRUE
WHERE admin.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM researcher_degrees d
      WHERE d.employee_no = seed.employee_no
        AND d.degree_type = seed.degree_type
        AND d.university_name = seed.university_name
  );

INSERT INTO korus_faculty_sync_runs (request_id, run_type, target_start_date, target_end_date, run_status, total_count, success_count, failure_count, created_by, started_at, finished_at, failure_reason)
SELECT 'REQ-BASIC38-KORUS-SAMPLE', 'MANUAL', DATE '2026-08-01', DATE '2026-08-31', 'PARTIAL', 2, 1, 1, u.user_id, TIMESTAMP '2026-08-31 02:00:00', TIMESTAMP '2026-08-31 02:02:30', '일부 조직 검증 실패'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (request_id) DO UPDATE SET run_status = EXCLUDED.run_status, total_count = EXCLUDED.total_count, success_count = EXCLUDED.success_count, failure_count = EXCLUDED.failure_count, finished_at = EXCLUDED.finished_at;

INSERT INTO korus_faculty_sync_results (run_id, request_id, employee_no, name, organization_code, rank_name, appointment_id, sync_status, error_message)
SELECT r.run_id, 'REQ-BASIC38-KORUS-SAMPLE', seed.employee_no, seed.name, seed.organization_code, seed.rank_name, seed.appointment_id, seed.sync_status, seed.error_message
FROM korus_faculty_sync_runs r
JOIN (VALUES
    ('E1002', '김미래', 'KNUE-DEPT-COMP', '부교수', 'E1002-APPT', 'SUCCESS', NULL),
    ('E1003', '이하늘', 'KNUE-DEPT-COMP', '조교수', 'E1003-APPT', 'FAILED', '조직 매핑 검증 실패')
) seed(employee_no, name, organization_code, rank_name, appointment_id, sync_status, error_message) ON TRUE
WHERE r.request_id = 'REQ-BASIC38-KORUS-SAMPLE'
ON CONFLICT (run_id, employee_no, organization_code, appointment_id) DO UPDATE SET request_id = EXCLUDED.request_id, name = EXCLUDED.name, rank_name = EXCLUDED.rank_name, sync_status = EXCLUDED.sync_status, error_message = EXCLUDED.error_message, created_at = CURRENT_TIMESTAMP;

INSERT INTO batch_executions (execution_id, batch_id, process_type, reason, operator_user_id, execution_status, request_id, execution_parameter_json)
SELECT 'SEED-BASIC38-BATCH-RESULT-001', 'SEED-BATCH-DEF-001', 'MANUAL_RUN', 'BASIC-38 배치 결과 조회 예시', u.user_id, 'COMPLETED', 'seed-basic38', '{"year":2026,"scope":"BASIC38"}'::jsonb
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (execution_id) DO UPDATE SET execution_status = EXCLUDED.execution_status, updated_at = CURRENT_TIMESTAMP;

INSERT INTO batch_execution_results (execution_id, started_at, ended_at, total_count, success_count, failure_count, excluded_count, elapsed_millis)
VALUES ('SEED-BASIC38-BATCH-RESULT-001', TIMESTAMP '2026-09-01 02:00:00', TIMESTAMP '2026-09-01 02:04:10', 80, 78, 2, 0, 250000)
ON CONFLICT (execution_id) DO UPDATE SET started_at = EXCLUDED.started_at, ended_at = EXCLUDED.ended_at, total_count = EXCLUDED.total_count, success_count = EXCLUDED.success_count, failure_count = EXCLUDED.failure_count, excluded_count = EXCLUDED.excluded_count, elapsed_millis = EXCLUDED.elapsed_millis;

INSERT INTO batch_execution_logs (execution_id, log_file_ref)
VALUES ('SEED-BASIC38-BATCH-RESULT-001', 'logs/batch/SEED-BASIC38-BATCH-RESULT-001.log')
ON CONFLICT (execution_id) DO UPDATE SET log_file_ref = EXCLUDED.log_file_ref;

INSERT INTO excel_upload_templates (template_id, business_type, template_version, effective_date, created_by, updated_by, change_reason)
SELECT 'SEED-BASIC38-EXCEL-TEMPLATE-002', 'RESEARCHER_PROFILE', 'v1.1', DATE '2026-09-01', u.user_id, u.user_id, 'BASIC-38 업로드 양식 관리 예시'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (template_id) DO UPDATE SET business_type = EXCLUDED.business_type, template_version = EXCLUDED.template_version, effective_date = EXCLUDED.effective_date, updated_at = CURRENT_TIMESTAMP, updated_by = EXCLUDED.updated_by, change_reason = EXCLUDED.change_reason;

INSERT INTO excel_upload_template_rules (rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by)
SELECT seed.rule_id, 'SEED-BASIC38-EXCEL-TEMPLATE-002', seed.required_column, seed.column_order, seed.code_rule_ref, u.user_id, u.user_id
FROM users u
JOIN (VALUES
    ('SEED-BASIC38-EXCEL-RULE-001', '교번', 1, 'COMMON_STATUS.ACTIVE'),
    ('SEED-BASIC38-EXCEL-RULE-002', '연구자등록번호', 2, 'COMMON_STATUS.ACTIVE')
) seed(rule_id, required_column, column_order, code_rule_ref) ON TRUE
WHERE u.login_id = 'admin'
ON CONFLICT (rule_id) DO UPDATE SET required_column = EXCLUDED.required_column, column_order = EXCLUDED.column_order, code_rule_ref = EXCLUDED.code_rule_ref, updated_at = CURRENT_TIMESTAMP;

INSERT INTO excel_upload_template_files (file_token, template_id, original_file_name, content_type, file_size_bytes, created_by)
SELECT 'basic38-excel-template-token-002', 'SEED-BASIC38-EXCEL-TEMPLATE-002', '연구자프로필_업로드양식_v1.1.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 4096, u.user_id
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (file_token) DO UPDATE SET original_file_name = EXCLUDED.original_file_name;

INSERT INTO excel_upload_files (upload_id, business_type, template_id, file_token, original_file_name, validation_status, uploader_user_id)
SELECT 'SEED-BASIC38-EXCEL-UPLOAD-ERROR', 'RESEARCHER_PROFILE', 'SEED-BASIC38-EXCEL-TEMPLATE-002', 'basic38-excel-upload-error-token', '연구자프로필_오류예시.xlsx', 'REJECTED', u.user_id
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (upload_id) DO UPDATE SET business_type = EXCLUDED.business_type, template_id = EXCLUDED.template_id, original_file_name = EXCLUDED.original_file_name, validation_status = EXCLUDED.validation_status;

INSERT INTO excel_upload_errors (error_id, upload_id, row_number, column_name, input_value, error_code, error_reason, correction_guide)
VALUES ('SEED-BASIC38-EXCEL-ERROR-001', 'SEED-BASIC38-EXCEL-UPLOAD-ERROR', 3, '연구자등록번호', '', 'REQUIRED_VALUE', '필수값이 누락되었습니다.', '연구자등록번호를 입력하세요.')
ON CONFLICT (error_id) DO UPDATE SET input_value = EXCLUDED.input_value, error_code = EXCLUDED.error_code, error_reason = EXCLUDED.error_reason, correction_guide = EXCLUDED.correction_guide;

INSERT INTO excel_upload_histories (upload_id, total_count, success_count, error_count, excluded_count, saved_count, processing_time_millis, processor_user_id)
SELECT 'SEED-BASIC38-EXCEL-UPLOAD-ERROR', 3, 2, 1, 0, 0, 1500, u.user_id
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (upload_id) DO UPDATE SET total_count = EXCLUDED.total_count, success_count = EXCLUDED.success_count, error_count = EXCLUDED.error_count, excluded_count = EXCLUDED.excluded_count, saved_count = EXCLUDED.saved_count, processing_time_millis = EXCLUDED.processing_time_millis, processed_at = CURRENT_TIMESTAMP, processor_user_id = EXCLUDED.processor_user_id;
