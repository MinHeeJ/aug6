INSERT INTO excel_upload_templates (
    template_id, business_type, template_version, effective_date, created_by, updated_by, change_reason
)
SELECT 'STUDENT-GUIDANCE-UPLOAD-V1', 'STUDENT_GUIDANCE_ACHIEVEMENT', 'v1.0', DATE '2026-01-01', u.user_id, u.user_id, '학생지도 일괄등록 현행 양식 등록'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (template_id) DO UPDATE SET
    business_type = EXCLUDED.business_type,
    template_version = EXCLUDED.template_version,
    effective_date = EXCLUDED.effective_date,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO excel_upload_template_rules (
    rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by
)
SELECT seed.rule_id, 'STUDENT-GUIDANCE-UPLOAD-V1', seed.required_column, seed.column_order, seed.code_rule_ref, u.user_id, u.user_id
FROM users u
CROSS JOIN (VALUES
    ('STUDENT-GUIDANCE-UPLOAD-RULE-001', '관리항목코드', 1, 'EVALUATION_MANAGEMENT_ITEM.STUDENT_GUIDANCE'),
    ('STUDENT-GUIDANCE-UPLOAD-RULE-002', '발생일', 2, 'EVALUATION_PERIOD.STUDENT_GUIDANCE'),
    ('STUDENT-GUIDANCE-UPLOAD-RULE-003', '지도학생', 3, 'KORUS_STUDENT.NAME'),
    ('STUDENT-GUIDANCE-UPLOAD-RULE-004', '지도시작일', 4, 'EVALUATION_PERIOD.STUDENT_GUIDANCE'),
    ('STUDENT-GUIDANCE-UPLOAD-RULE-005', '지도종료일', 5, 'EVALUATION_PERIOD.STUDENT_GUIDANCE'),
    ('STUDENT-GUIDANCE-UPLOAD-RULE-006', '학생수', 6, 'POSITIVE_INTEGER')
) AS seed(rule_id, required_column, column_order, code_rule_ref)
WHERE u.login_id = 'admin'
ON CONFLICT (rule_id) DO UPDATE SET
    required_column = EXCLUDED.required_column,
    column_order = EXCLUDED.column_order,
    code_rule_ref = EXCLUDED.code_rule_ref,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO excel_upload_template_files (
    file_token, template_id, original_file_name, content_type, file_size_bytes, created_by
)
SELECT 'student-guidance-upload-template-v1', 'STUDENT-GUIDANCE-UPLOAD-V1', '학생지도_일괄등록_양식.csv', 'text/csv', 0, u.user_id
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (file_token) DO UPDATE SET
    original_file_name = EXCLUDED.original_file_name,
    content_type = EXCLUDED.content_type,
    file_size_bytes = EXCLUDED.file_size_bytes;
