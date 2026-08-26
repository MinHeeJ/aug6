CREATE TABLE IF NOT EXISTS excel_upload_templates (
    template_id varchar(100) PRIMARY KEY,
    business_type varchar(50) NOT NULL,
    template_version varchar(50) NOT NULL,
    effective_date date NOT NULL,
    system_use_yn varchar(1) NOT NULL DEFAULT 'Y' CHECK (system_use_yn IN ('Y','N')),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    change_reason varchar(500),
    CONSTRAINT uq_excel_upload_templates_business_version_date UNIQUE (business_type, template_version, effective_date),
    CONSTRAINT fk_excel_upload_templates_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_excel_upload_templates_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE excel_upload_templates IS '업무별 Excel 업로드 양식의 버전과 시행일을 보존하고 이전 버전 재조회 기준을 제공한다.';
COMMENT ON COLUMN excel_upload_templates.system_use_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN excel_upload_templates.status IS 'ACTIVE:활성|INACTIVE:비활성|DELETED:삭제표시';

CREATE TABLE IF NOT EXISTS excel_upload_template_rules (
    rule_id varchar(100) PRIMARY KEY,
    template_id varchar(100) NOT NULL,
    required_column varchar(200) NOT NULL,
    column_order integer NOT NULL,
    code_rule_ref varchar(200) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT uq_excel_upload_template_rules_order UNIQUE (template_id, column_order),
    CONSTRAINT uq_excel_upload_template_rules_column UNIQUE (template_id, required_column),
    CONSTRAINT fk_excel_upload_template_rules_template FOREIGN KEY (template_id) REFERENCES excel_upload_templates(template_id),
    CONSTRAINT fk_excel_upload_template_rules_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_excel_upload_template_rules_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE excel_upload_template_rules IS '업로드 양식별 필수 열, 열 순서, 코드값 규칙 참조를 양식 버전과 연결해 보존한다.';
COMMENT ON COLUMN excel_upload_template_rules.code_rule_ref IS 'detail_codes 기준 참조 의도 (기존 코드 원장 변경 없음)';

CREATE TABLE IF NOT EXISTS excel_upload_template_files (
    file_token varchar(200) PRIMARY KEY,
    template_id varchar(100) NOT NULL,
    original_file_name varchar(255) NOT NULL,
    content_type varchar(100),
    file_size_bytes bigint,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    CONSTRAINT fk_excel_upload_template_files_template FOREIGN KEY (template_id) REFERENCES excel_upload_templates(template_id),
    CONSTRAINT fk_excel_upload_template_files_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
);
COMMENT ON TABLE excel_upload_template_files IS '업로드 양식 버전별 다운로드 파일 token과 원본 파일명을 보존하며 저장 경로와 실제 파일명은 노출하지 않는다.';
COMMENT ON COLUMN excel_upload_template_files.file_token IS 'TemplateFileService.saveTemplateFile 시 애플리케이션에서 발급하는 외부 노출용 파일 식별자';

CREATE TABLE IF NOT EXISTS excel_upload_files (
    upload_id varchar(100) PRIMARY KEY,
    business_type varchar(50) NOT NULL,
    template_id varchar(100),
    file_token varchar(200) NOT NULL,
    original_file_name varchar(255) NOT NULL,
    validation_status varchar(20) NOT NULL DEFAULT 'UPLOADED' CHECK (validation_status IN ('UPLOADED','VALIDATED','COMMITTED','REJECTED')),
    uploader_user_id bigint NOT NULL,
    uploaded_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    CONSTRAINT fk_excel_upload_files_template FOREIGN KEY (template_id) REFERENCES excel_upload_templates(template_id),
    CONSTRAINT fk_excel_upload_files_uploader FOREIGN KEY (uploader_user_id) REFERENCES users(user_id)
);
COMMENT ON TABLE excel_upload_files IS '업무별 Excel 업로드 파일의 upload_id, 원본 파일명, file token, 검증상태와 업로더를 보존한다.';
COMMENT ON COLUMN excel_upload_files.file_token IS 'ExcelUploadService.createExcelUpload 시 애플리케이션에서 발급하는 외부 노출용 파일 식별자';
COMMENT ON COLUMN excel_upload_files.validation_status IS 'UPLOADED:업로드됨|VALIDATED:검증완료|COMMITTED:반영완료|REJECTED:반영거부';
COMMENT ON COLUMN excel_upload_files.status IS 'ACTIVE:활성|INACTIVE:비활성|DELETED:삭제표시';

CREATE TABLE IF NOT EXISTS excel_upload_staging_rows (
    staging_row_id varchar(100) PRIMARY KEY,
    upload_id varchar(100) NOT NULL,
    row_number integer NOT NULL,
    row_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    validation_status varchar(20) NOT NULL CHECK (validation_status IN ('NORMAL','ERROR','EXCLUDED')),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_excel_upload_staging_rows_upload_row UNIQUE (upload_id, row_number),
    CONSTRAINT fk_excel_upload_staging_rows_upload FOREIGN KEY (upload_id) REFERENCES excel_upload_files(upload_id)
);
COMMENT ON TABLE excel_upload_staging_rows IS '엑셀 업로드 반영 전 원본 행 payload와 검증상태를 임시 staging으로 보존하며 반영 완료 후 정리 대상이다.';
COMMENT ON COLUMN excel_upload_staging_rows.row_payload IS 'ExcelUploadService.createExcelUpload 검증 시 원본 행 payload로 생성하고 commit/cleanup 시 애플리케이션에서 삭제';
COMMENT ON COLUMN excel_upload_staging_rows.validation_status IS 'NORMAL:정상|ERROR:오류|EXCLUDED:제외';

CREATE TABLE IF NOT EXISTS excel_upload_errors (
    error_id varchar(100) PRIMARY KEY,
    upload_id varchar(100) NOT NULL,
    row_number integer NOT NULL,
    column_name varchar(200) NOT NULL,
    input_value text,
    error_code varchar(50) NOT NULL,
    error_reason varchar(500) NOT NULL,
    correction_guide varchar(500),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_excel_upload_errors_location UNIQUE (upload_id, row_number, column_name),
    CONSTRAINT fk_excel_upload_errors_upload FOREIGN KEY (upload_id) REFERENCES excel_upload_files(upload_id)
);
COMMENT ON TABLE excel_upload_errors IS '엑셀 사전검증에서 발생한 업로드ID별 오류 행·컬럼·사유·수정안내를 업무자료 생성 없이 보존한다.';
COMMENT ON COLUMN excel_upload_errors.status IS 'ACTIVE:활성|INACTIVE:비활성|DELETED:삭제표시';

CREATE TABLE IF NOT EXISTS excel_upload_histories (
    upload_id varchar(100) PRIMARY KEY,
    total_count integer NOT NULL DEFAULT 0,
    success_count integer NOT NULL DEFAULT 0,
    error_count integer NOT NULL DEFAULT 0,
    excluded_count integer NOT NULL DEFAULT 0,
    saved_count integer NOT NULL DEFAULT 0,
    processing_time_millis bigint,
    processed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processor_user_id bigint,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    CONSTRAINT fk_excel_upload_histories_upload FOREIGN KEY (upload_id) REFERENCES excel_upload_files(upload_id),
    CONSTRAINT fk_excel_upload_histories_processor FOREIGN KEY (processor_user_id) REFERENCES users(user_id)
);
COMMENT ON TABLE excel_upload_histories IS '엑셀 업로드 작업별 원본·정상·오류·제외·저장 건수와 처리시간을 물리 삭제 없이 보존한다.';
COMMENT ON COLUMN excel_upload_histories.processing_time_millis IS 'ExcelUploadService validation/commit 완료 시 애플리케이션에서 산출해 갱신하는 파생 소요시간';
COMMENT ON COLUMN excel_upload_histories.status IS 'ACTIVE:활성|INACTIVE:비활성|DELETED:삭제표시';

CREATE TABLE IF NOT EXISTS excel_download_jobs (
    download_id varchar(100) PRIMARY KEY,
    requester_user_id bigint NOT NULL,
    output_type varchar(30) NOT NULL CHECK (output_type IN ('TARGET','STATUS','ERROR')),
    query_condition jsonb NOT NULL DEFAULT '{}'::jsonb,
    data_scope_ref varchar(200),
    file_token varchar(200) NOT NULL,
    original_file_name varchar(255),
    requested_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_at timestamp,
    status varchar(20) NOT NULL DEFAULT 'GENERATED' CHECK (status IN ('REQUESTED','GENERATED','FAILED','DELETED')),
    CONSTRAINT fk_excel_download_jobs_requester FOREIGN KEY (requester_user_id) REFERENCES users(user_id)
);
COMMENT ON TABLE excel_download_jobs IS '현재 조회조건과 사용자 데이터 범위 권한을 적용한 Excel 결과 생성 작업과 file token을 보존한다.';
COMMENT ON COLUMN excel_download_jobs.output_type IS 'TARGET:평가대상자|STATUS:현황|ERROR:오류자료';
COMMENT ON COLUMN excel_download_jobs.query_condition IS 'ExcelDownloadService.createExcelDownload 요청 시 현재 조회조건을 애플리케이션에서 저장';
COMMENT ON COLUMN excel_download_jobs.data_scope_ref IS 'menu_permissions 또는 기존 데이터 범위 권한 결과 참조 의도 (신규 권한정책 미정의)';
COMMENT ON COLUMN excel_download_jobs.file_token IS 'ExcelDownloadService.createExcelDownload 성공 시 애플리케이션에서 발급하는 외부 노출용 파일 식별자';
COMMENT ON COLUMN excel_download_jobs.status IS 'REQUESTED:요청됨|GENERATED:생성완료|FAILED:실패|DELETED:삭제표시';

CREATE INDEX IF NOT EXISTS idx_excel_upload_templates_lookup ON excel_upload_templates (business_type, effective_date, template_version);
CREATE INDEX IF NOT EXISTS idx_excel_upload_template_rules_template ON excel_upload_template_rules (template_id, column_order);
CREATE INDEX IF NOT EXISTS idx_excel_upload_template_files_template ON excel_upload_template_files (template_id);
CREATE INDEX IF NOT EXISTS idx_excel_upload_files_business ON excel_upload_files (business_type, uploaded_at);
CREATE INDEX IF NOT EXISTS idx_excel_upload_staging_rows_upload_status ON excel_upload_staging_rows (upload_id, validation_status);
CREATE INDEX IF NOT EXISTS idx_excel_upload_errors_upload ON excel_upload_errors (upload_id, row_number);
CREATE INDEX IF NOT EXISTS idx_excel_upload_histories_processed_at ON excel_upload_histories (processed_at);
CREATE INDEX IF NOT EXISTS idx_excel_download_jobs_requester ON excel_download_jobs (requester_user_id, requested_at);

INSERT INTO excel_upload_templates (template_id, business_type, template_version, effective_date, created_by, updated_by, change_reason)
SELECT 'SEED-EXCEL-TEMPLATE-001', 'PROFESSOR_ACHIEVEMENT', 'v1.0', DATE '2026-01-01', u.user_id, u.user_id, 'BASIC-26 업로드 양식 seed'
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO excel_upload_template_rules (rule_id, template_id, required_column, column_order, code_rule_ref, created_by, updated_by)
SELECT rule_id, 'SEED-EXCEL-TEMPLATE-001', required_column, column_order, code_rule_ref, u.user_id, u.user_id
FROM users u
CROSS JOIN (VALUES
    ('SEED-EXCEL-TEMPLATE-RULE-001', '교번', 1, 'COMMON_STATUS.ACTIVE'),
    ('SEED-EXCEL-TEMPLATE-RULE-002', '업적명', 2, 'COMMON_STATUS.ACTIVE')
) seed(rule_id, required_column, column_order, code_rule_ref)
WHERE u.login_id = 'admin'
ON CONFLICT (rule_id) DO NOTHING;

INSERT INTO excel_upload_template_files (file_token, template_id, original_file_name, content_type, file_size_bytes, created_by)
SELECT 'excel-template-token-001', 'SEED-EXCEL-TEMPLATE-001', '업로드양식_v1.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 2048, u.user_id
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (file_token) DO NOTHING;

INSERT INTO excel_upload_files (upload_id, business_type, template_id, file_token, original_file_name, validation_status, uploader_user_id)
SELECT upload_id, 'PROFESSOR_ACHIEVEMENT', 'SEED-EXCEL-TEMPLATE-001', file_token, original_file_name, validation_status, u.user_id
FROM users u
CROSS JOIN (VALUES
    ('SEED-EXCEL-UPLOAD-VALID', 'excel-upload-valid-token', '정상업로드.xlsx', 'VALIDATED'),
    ('SEED-EXCEL-UPLOAD-ERROR', 'excel-upload-error-token', '오류업로드.xlsx', 'REJECTED')
) seed(upload_id, file_token, original_file_name, validation_status)
WHERE u.login_id = 'admin'
ON CONFLICT (upload_id) DO NOTHING;

INSERT INTO excel_upload_staging_rows (staging_row_id, upload_id, row_number, row_payload, validation_status) VALUES
('SEED-EXCEL-STAGING-VALID-001', 'SEED-EXCEL-UPLOAD-VALID', 1, '{"employeeNo":"E1001","title":"검증용 정상 행"}'::jsonb, 'NORMAL')
ON CONFLICT (staging_row_id) DO NOTHING;

INSERT INTO excel_upload_errors (error_id, upload_id, row_number, column_name, input_value, error_code, error_reason, correction_guide) VALUES
('SEED-EXCEL-ERROR-001', 'SEED-EXCEL-UPLOAD-ERROR', 2, '교번', 'E9999', 'INVALID_CODE', '존재하지 않는 교번입니다.', 'KORUS 기준 교번을 확인하세요.')
ON CONFLICT (error_id) DO NOTHING;

INSERT INTO excel_upload_histories (upload_id, total_count, success_count, error_count, excluded_count, saved_count, processing_time_millis, processor_user_id)
SELECT upload_id, total_count, success_count, error_count, excluded_count, saved_count, processing_time_millis, u.user_id
FROM users u
CROSS JOIN (VALUES
    ('SEED-EXCEL-UPLOAD-VALID', 1, 1, 0, 0, 0, 1200),
    ('SEED-EXCEL-UPLOAD-ERROR', 1, 0, 1, 0, 0, 900)
) seed(upload_id, total_count, success_count, error_count, excluded_count, saved_count, processing_time_millis)
WHERE u.login_id = 'admin'
ON CONFLICT (upload_id) DO NOTHING;

INSERT INTO excel_download_jobs (download_id, requester_user_id, output_type, query_condition, data_scope_ref, file_token, original_file_name, generated_at)
SELECT 'SEED-EXCEL-DOWNLOAD-001', u.user_id, 'ERROR', '{"uploadId":"SEED-EXCEL-UPLOAD-ERROR"}'::jsonb, 'R09:ALL', 'excel-download-token-001', '오류자료.xlsx', CURRENT_TIMESTAMP
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (download_id) DO NOTHING;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by, change_reason) VALUES
(200, NULL, 'MAIN', '파일·데이터 관리', 3, NULL, NULL, 'folder-up', 'FILE_DATA', '파일 및 데이터 운영 관리 대메뉴', 'Y', 'ACTIVE', 1, 'BASIC-26 Excel 관리 메뉴 seed'),
(210, 200, 'MIDDLE', '엑셀 관리', 1, NULL, NULL, 'file-spreadsheet', 'FILE_DATA', '업로드 양식·업로드·이력·오류·다운로드 관리', 'Y', 'ACTIVE', 1, 'BASIC-26 Excel 관리 메뉴 seed'),
(211, 210, 'SCREEN', '업로드 양식 관리', 1, 'SCR-UPLOAD-TEMPLATE-MGMT', '/admin/excel-upload-templates', 'file-cog', 'FILE_DATA', '업무별 Excel 업로드 양식과 검증규칙 버전 관리', 'Y', 'ACTIVE', 1, 'BASIC-26 Excel 관리 메뉴 seed'),
(212, 210, 'SCREEN', '엑셀 업로드', 2, 'SCR-EXCEL-UPLOAD-MGMT', '/admin/excel-uploads', 'upload', 'FILE_DATA', '업무별 Excel 업로드·사전검증·반영 관리', 'Y', 'ACTIVE', 1, 'BASIC-26 Excel 관리 메뉴 seed'),
(213, 210, 'SCREEN', '업로드 이력', 3, 'SCR-UPLOAD-HISTORY-MGMT', '/admin/excel-upload-histories', 'history', 'FILE_DATA', '엑셀 업로드 작업 이력과 처리건수 조회', 'Y', 'ACTIVE', 1, 'BASIC-26 Excel 관리 메뉴 seed'),
(214, 210, 'SCREEN', '업로드 오류 관리', 4, 'SCR-UPLOAD-ERROR-MGMT', '/admin/excel-upload-errors', 'alert-triangle', 'FILE_DATA', '엑셀 업로드 오류행 조회와 오류목록 다운로드', 'Y', 'ACTIVE', 1, 'BASIC-26 Excel 관리 메뉴 seed'),
(215, 210, 'SCREEN', '엑셀 다운로드', 5, 'SCR-EXCEL-DOWNLOAD-MGMT', '/admin/excel-downloads', 'download', 'FILE_DATA', '조회조건과 권한 범위를 적용한 Excel 결과 생성', 'Y', 'ACTIVE', 1, 'BASIC-26 Excel 관리 메뉴 seed')
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, parent_menu_id = EXCLUDED.parent_menu_id, menu_type = EXCLUDED.menu_type, display_order = EXCLUDED.display_order, screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, icon = EXCLUDED.icon, business_category = EXCLUDED.business_category, description = EXCLUDED.description, system_use_yn = EXCLUDED.system_use_yn, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP, updated_by = EXCLUDED.updated_by, change_reason = EXCLUDED.change_reason;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1
FROM menus
WHERE menu_id BETWEEN 211 AND 215
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, icon = EXCLUDED.icon, business_category = EXCLUDED.business_category, description = EXCLUDED.description, updated_at = CURRENT_TIMESTAMP, updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-26 Excel 관리 화면 접근'
FROM menus
WHERE menu_id BETWEEN 211 AND 215
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP, updated_by = EXCLUDED.updated_by, change_reason = EXCLUDED.change_reason;
