-- 학생지도 Excel 일괄등록에서 기존 Excel 공통 테이블을 재사용할 표준 양식을 등록한다.
INSERT INTO excel_upload_templates (template_id, business_type, template_version, effective_date, created_by, updated_by, change_reason)
SELECT 'STUDENT_GUIDANCE_ACHIEVEMENT', 'STUDENT_GUIDANCE_ACHIEVEMENT', '1.0', DATE '2026-01-01', admin.user_id, admin.user_id, 'BASIC-73 학생지도 Excel 표준 양식'
FROM users admin
WHERE admin.login_id = 'admin'
  AND NOT EXISTS (SELECT 1 FROM excel_upload_templates WHERE template_id = 'STUDENT_GUIDANCE_ACHIEVEMENT');

INSERT INTO excel_upload_template_files (file_token, template_id, original_file_name, content_type, file_size_bytes, created_by)
SELECT 'template-file-STUDENT_GUIDANCE_ACHIEVEMENT', 'STUDENT_GUIDANCE_ACHIEVEMENT', '학생지도실적_일괄등록_양식.csv', 'text/csv', 0, admin.user_id
FROM users admin
WHERE admin.login_id = 'admin'
  AND NOT EXISTS (SELECT 1 FROM excel_upload_template_files WHERE template_id = 'STUDENT_GUIDANCE_ACHIEVEMENT');

INSERT INTO excel_upload_template_rules (rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by)
SELECT 'STUDENT_GUIDANCE_ACHIEVEMENT-RULE-' || rule.column_order, 'STUDENT_GUIDANCE_ACHIEVEMENT', rule.required_column, rule.column_order, rule.code_rule_ref, admin.user_id, admin.user_id
FROM users admin
CROSS JOIN (VALUES
    ('managementItemCode', 1, 'FR-018'),
    ('occurrenceDate', 2, 'DATE'),
    ('guidanceStudentName', 3, 'REQUIRED'),
    ('guidanceStartDate', 4, 'DATE'),
    ('guidanceEndDate', 5, 'DATE'),
    ('studentCount', 6, 'POSITIVE_INTEGER')
) AS rule(required_column, column_order, code_rule_ref)
WHERE admin.login_id = 'admin'
  AND NOT EXISTS (SELECT 1 FROM excel_upload_template_rules existing WHERE existing.template_id = 'STUDENT_GUIDANCE_ACHIEVEMENT' AND existing.column_order = rule.column_order);
