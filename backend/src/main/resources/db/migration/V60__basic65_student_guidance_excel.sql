ALTER TABLE student_guidance_achievements ADD COLUMN IF NOT EXISTS dynamic_fields jsonb NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE student_guidance_achievements ADD COLUMN IF NOT EXISTS attachment_refs jsonb NOT NULL DEFAULT '[]'::jsonb;
COMMENT ON COLUMN student_guidance_achievements.dynamic_fields IS '학생지도 관리항목의 동적 입력값. StudentGuidanceAchievementService 저장 시 애플리케이션에서 갱신';
COMMENT ON COLUMN student_guidance_achievements.attachment_refs IS '기존 FileStoragePort 첨부파일 외부 참조 목록. StudentGuidanceAchievementService 저장 시 애플리케이션에서 갱신';

INSERT INTO excel_upload_templates (template_id, business_type, template_version, effective_date, created_by, updated_by, change_reason)
SELECT 'STUDENT-GUIDANCE-V1', 'STUDENT_GUIDANCE', 'v1.0', DATE '2026-01-01', u.user_id, u.user_id, '학생지도 Excel 일괄등록 양식'
FROM users u WHERE u.login_id = 'admin' ON CONFLICT (template_id) DO NOTHING;
INSERT INTO excel_upload_template_rules (rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by)
SELECT 'STUDENT-GUIDANCE-RULE-' || seed.column_order, 'STUDENT-GUIDANCE-V1', seed.required_column, seed.column_order, seed.code_rule_ref, u.user_id, u.user_id
FROM users u CROSS JOIN (VALUES
  ('evaluationYear',1,'YEAR.YYYY'),('academicYear',2,'YEAR.YYYY'),('semester',3,'SEMESTER.ACTIVE'),('studentNo',4,'KORUS.STUDENT_NO'),('studentName',5,'KORUS.STUDENT_NAME'),('guidanceType',6,'GUIDANCE_TYPE.ACTIVE'),('guidanceDate',7,'DATE.ISO'),('guidanceContent',8,'TEXT.REQUIRED')
) seed(required_column,column_order,code_rule_ref) WHERE u.login_id='admin' ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO excel_upload_template_files (file_token, template_id, original_file_name, content_type, file_size_bytes, created_by)
SELECT 'student-guidance-template-v1', 'STUDENT-GUIDANCE-V1', '학생지도_일괄등록_양식.csv', 'text/csv', 0, u.user_id FROM users u WHERE u.login_id='admin' ON CONFLICT (file_token) DO NOTHING;
