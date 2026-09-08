CREATE TABLE IF NOT EXISTS reports (
    report_id VARCHAR(100) PRIMARY KEY,
    report_name VARCHAR(200) NOT NULL,
    business_category VARCHAR(100) NOT NULL,
    template_file_ref VARCHAR(500) NOT NULL,
    dataset_code VARCHAR(100) NOT NULL,
    active_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_yn IN ('Y','N')),
    change_reason VARCHAR(500) NOT NULL DEFAULT '초기 등록',
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE reports IS '논리 보고서ID와 템플릿·데이터셋·사용여부를 관리하는 보고서 목록 기준정보.';
COMMENT ON COLUMN reports.active_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN reports.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN reports.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS report_form_versions (
    form_version_id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(100) NOT NULL REFERENCES reports(report_id),
    version_name VARCHAR(100) NOT NULL,
    effective_date DATE NOT NULL,
    form_file_ref VARCHAR(500) NOT NULL,
    current_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (current_yn IN ('Y','N')),
    change_reason VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_report_form_versions_report_version UNIQUE (report_id, version_name),
    CONSTRAINT uq_report_form_versions_report_effective UNIQUE (report_id, effective_date)
);
COMMENT ON TABLE report_form_versions IS '보고서별 양식 버전과 시행일 및 파일 참조를 보존하여 기준일 출력 양식을 식별한다.';
COMMENT ON COLUMN report_form_versions.current_yn IS 'Y:현재적용|N:과거버전';
COMMENT ON COLUMN report_form_versions.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN report_form_versions.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS report_permissions (
    permission_id BIGSERIAL PRIMARY KEY,
    grantee_type VARCHAR(20) NOT NULL CHECK (grantee_type IN ('ROLE','ORG','USER')),
    grantee_id VARCHAR(100) NOT NULL,
    report_id VARCHAR(100) NOT NULL REFERENCES reports(report_id),
    allow_view_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (allow_view_yn IN ('Y','N')),
    allow_preview_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (allow_preview_yn IN ('Y','N')),
    allow_print_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (allow_print_yn IN ('Y','N')),
    allow_pdf_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (allow_pdf_yn IN ('Y','N')),
    allow_excel_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (allow_excel_yn IN ('Y','N')),
    data_scope VARCHAR(50) NOT NULL,
    active_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_yn IN ('Y','N')),
    change_reason VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_report_permissions_target UNIQUE (grantee_type, grantee_id, report_id)
);
COMMENT ON TABLE report_permissions IS '보고서별 역할·조직·사용자 권한과 출력 형식 및 데이터 범위를 관리한다.';
COMMENT ON COLUMN report_permissions.grantee_type IS 'ROLE:역할|ORG:조직|USER:사용자';
COMMENT ON COLUMN report_permissions.allow_view_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN report_permissions.allow_preview_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN report_permissions.allow_print_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN report_permissions.allow_pdf_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN report_permissions.allow_excel_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN report_permissions.active_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN report_permissions.grantee_id IS 'grantee_type에 따라 roles.role_code, organizations.organization_code, users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN report_permissions.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN report_permissions.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS report_print_histories (
    print_history_id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(100) NOT NULL REFERENCES reports(report_id),
    requester_id BIGINT NOT NULL,
    target_summary VARCHAR(1000) NOT NULL,
    output_format VARCHAR(20) NOT NULL CHECK (output_format IN ('PDF','EXCEL')),
    output_count INTEGER NOT NULL DEFAULT 0,
    result_code VARCHAR(20) NOT NULL CHECK (result_code IN ('SUCCESS','FAILED','FORBIDDEN')),
    output_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    file_ref VARCHAR(500),
    request_id VARCHAR(100)
);
COMMENT ON TABLE report_print_histories IS '보고서 출력·다운로드의 대상범위, 형식, 건수, 결과와 요청 식별자를 append-only로 추적한다.';
COMMENT ON COLUMN report_print_histories.output_format IS 'PDF:PDF|EXCEL:엑셀';
COMMENT ON COLUMN report_print_histories.result_code IS 'SUCCESS:성공|FAILED:실패|FORBIDDEN:권한부족';
COMMENT ON COLUMN report_print_histories.requester_id IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS bulk_report_jobs (
    job_id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(100) NOT NULL REFERENCES reports(report_id),
    requester_id BIGINT NOT NULL,
    target_hash VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED','RUNNING','COMPLETED','FAILED','PARTIAL_FAILED')),
    progress_rate INTEGER NOT NULL DEFAULT 0 CHECK (progress_rate >= 0 AND progress_rate <= 100),
    total_count INTEGER NOT NULL DEFAULT 0 CHECK (total_count >= 0),
    success_count INTEGER NOT NULL DEFAULT 0,
    fail_count INTEGER NOT NULL DEFAULT 0,
    result_file_ref VARCHAR(500),
    request_id VARCHAR(100) NOT NULL DEFAULT 'MIGRATION-SEED',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
COMMENT ON TABLE bulk_report_jobs IS '대량 보고서 생성 비동기 작업의 상태, 진행률, 결과 건수 및 결과파일 참조를 보존한다.';
COMMENT ON COLUMN bulk_report_jobs.job_id IS '대량 출력 작업 식별자.';
COMMENT ON COLUMN bulk_report_jobs.status IS 'QUEUED:대기|RUNNING:진행중|COMPLETED:완료|FAILED:실패|PARTIAL_FAILED:일부실패';
COMMENT ON COLUMN bulk_report_jobs.requester_id IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN bulk_report_jobs.target_hash IS '동일 보고서 대상자 묶음 중복 실행 차단용 해시.';
COMMENT ON COLUMN bulk_report_jobs.total_count IS '대량 출력 작업 생성 시 애플리케이션에서 대상자 수로 갱신';

CREATE TABLE IF NOT EXISTS bulk_report_job_targets (
    job_target_id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES bulk_report_jobs(job_id),
    target_person_id BIGINT NOT NULL,
    target_person_name VARCHAR(100) NOT NULL,
    target_organization_code VARCHAR(50) NOT NULL,
    result_code VARCHAR(20) NOT NULL CHECK (result_code IN ('QUEUED','SUCCESS','FAILED','FORBIDDEN')),
    error_detail VARCHAR(1000),
    CONSTRAINT uq_bulk_report_job_targets_person UNIQUE (job_id, target_person_id)
);
COMMENT ON TABLE bulk_report_job_targets IS '대량 출력 작업의 대상자별 처리 결과와 실패·권한 제외 상세를 보존한다.';
COMMENT ON COLUMN bulk_report_job_targets.job_target_id IS '대량 출력 작업 대상자별 결과 식별자';
COMMENT ON COLUMN bulk_report_job_targets.job_id IS 'bulk_report_jobs의 대량 출력 작업 식별자 참조.';
COMMENT ON COLUMN bulk_report_job_targets.result_code IS 'QUEUED:대기|SUCCESS:성공|FAILED:실패|FORBIDDEN:권한제외';
COMMENT ON COLUMN bulk_report_job_targets.target_person_id IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN bulk_report_job_targets.target_person_name IS 'users.user_id 참조 조회 결과를 대량 작업 대상 생성 시 애플리케이션에서 스냅샷으로 갱신';
COMMENT ON COLUMN bulk_report_job_targets.target_organization_code IS 'organizations.organization_code 참조 의도 (FK 미선언)';

CREATE UNIQUE INDEX IF NOT EXISTS uq_bulk_report_jobs_running ON bulk_report_jobs(report_id, target_hash) WHERE status IN ('QUEUED','RUNNING');
CREATE INDEX IF NOT EXISTS idx_reports_search ON reports(active_yn, business_category, report_id);
CREATE INDEX IF NOT EXISTS idx_report_form_versions_lookup ON report_form_versions(report_id, effective_date DESC);
CREATE INDEX IF NOT EXISTS idx_report_permissions_lookup ON report_permissions(report_id, grantee_type, grantee_id, active_yn);
CREATE INDEX IF NOT EXISTS idx_report_print_histories_search ON report_print_histories(report_id, output_at DESC, result_code);
CREATE INDEX IF NOT EXISTS idx_bulk_report_jobs_search ON bulk_report_jobs(report_id, status, job_id DESC);
CREATE INDEX IF NOT EXISTS idx_bulk_report_job_targets_job ON bulk_report_job_targets(job_id, result_code);

INSERT INTO reports (report_id, report_name, business_category, template_file_ref, dataset_code, active_yn, change_reason, created_by, updated_by) VALUES
('FINAL_EVALUATION', '최종평가서', 'FACULTY_ACHIEVEMENT', 'templates/reports/final-evaluation-v2.hwp', 'FINAL_EVALUATION_DATASET', 'Y', 'BASIC-54 정상 보고서 seed', 1, 1),
('PERSONAL_RESULT_SUMMARY', '개인별결과총괄표', 'FACULTY_ACHIEVEMENT', 'templates/reports/personal-result-summary-v1.xlsx', 'PERSONAL_RESULT_DATASET', 'Y', 'BASIC-54 경계 보고서 seed', 1, 1),
('INACTIVE_TEST_REPORT', '테스트용 미사용 보고서', 'FACULTY_ACHIEVEMENT', 'templates/reports/inactive-test-v1.hwp', 'INACTIVE_TEST_DATASET', 'N', 'BASIC-54 미사용 제외 seed', 1, 1)
ON CONFLICT (report_id) DO NOTHING;

INSERT INTO report_form_versions (report_id, version_name, effective_date, form_file_ref, current_yn, change_reason, created_by, updated_by) VALUES
('FINAL_EVALUATION', 'v1.0', '2025-01-01', 'templates/reports/final-evaluation-v1.hwp', 'N', 'BASIC-54 과거 양식 seed', 1, 1),
('FINAL_EVALUATION', 'v2.0', '2026-01-01', 'templates/reports/final-evaluation-v2.hwp', 'Y', 'BASIC-54 현재 양식 seed', 1, 1),
('PERSONAL_RESULT_SUMMARY', 'v1.0', '2026-01-01', 'templates/reports/personal-result-summary-v1.xlsx', 'Y', 'BASIC-54 결과통보 양식 seed', 1, 1)
ON CONFLICT (report_id, version_name) DO NOTHING;

INSERT INTO report_permissions (grantee_type, grantee_id, report_id, allow_view_yn, allow_preview_yn, allow_print_yn, allow_pdf_yn, allow_excel_yn, data_scope, active_yn, change_reason, created_by, updated_by) VALUES
('ROLE', 'R04', 'FINAL_EVALUATION', 'Y', 'Y', 'Y', 'Y', 'Y', 'ALL', 'Y', 'BASIC-54 R04 전체 권한 seed', 1, 1),
('ROLE', 'R03', 'FINAL_EVALUATION', 'Y', 'Y', 'Y', 'Y', 'N', 'COLLEGE', 'Y', 'BASIC-54 R03 소속대학 PDF 권한 seed', 1, 1),
('ROLE', 'R01', 'PERSONAL_RESULT_SUMMARY', 'Y', 'N', 'N', 'N', 'N', 'SELF', 'Y', 'BASIC-54 R01 본인 조회 seed', 1, 1)
ON CONFLICT (grantee_type, grantee_id, report_id) DO NOTHING;

INSERT INTO report_print_histories (report_id, requester_id, target_summary, output_format, output_count, result_code, file_ref, request_id) VALUES
('FINAL_EVALUATION', 4, '2026학년도 전체 대상자', 'PDF', 10, 'SUCCESS', 'reports/output/final-evaluation-2026.pdf', 'B54-SEED-HIST-001'),
('PERSONAL_RESULT_SUMMARY', 4, '2026학년도 개인별 총괄', 'EXCEL', 8, 'SUCCESS', 'reports/output/personal-summary-2026.xlsx', 'B54-SEED-HIST-002'),
('FINAL_EVALUATION', 1, '권한 밖 대상자', 'PDF', 0, 'FORBIDDEN', NULL, 'B54-SEED-HIST-003')
ON CONFLICT DO NOTHING;

INSERT INTO bulk_report_jobs (report_id, requester_id, target_hash, status, progress_rate, total_count, success_count, fail_count, result_file_ref, request_id, requested_at, completed_at) VALUES
('FINAL_EVALUATION', 4, 'HASH-FINAL-10', 'COMPLETED', 100, 10, 10, 0, 'reports/bulk/final-10.zip', 'B54-SEED-JOB-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('FINAL_EVALUATION', 4, 'HASH-FINAL-PARTIAL', 'PARTIAL_FAILED', 100, 10, 8, 2, 'reports/bulk/final-partial.zip', 'B54-SEED-JOB-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PERSONAL_RESULT_SUMMARY', 3, 'HASH-SUMMARY-RUNNING', 'RUNNING', 40, 20, 8, 0, NULL, 'B54-SEED-JOB-003', CURRENT_TIMESTAMP, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO bulk_report_job_targets (job_id, target_person_id, target_person_name, target_organization_code, result_code, error_detail)
SELECT j.job_id, seed.target_person_id, seed.target_person_name, seed.target_organization_code, seed.result_code, seed.error_detail
FROM bulk_report_jobs j
JOIN (VALUES
    ('HASH-FINAL-10', 1, '김교수', 'COL-EDU', 'SUCCESS', NULL),
    ('HASH-FINAL-PARTIAL', 2, '이교수', 'COL-SCI', 'FAILED', '양식 데이터 생성 실패'),
    ('HASH-SUMMARY-RUNNING', 3, '박교수', 'COL-EDU', 'QUEUED', NULL)
) AS seed(target_hash, target_person_id, target_person_name, target_organization_code, result_code, error_detail) ON seed.target_hash = j.target_hash
ON CONFLICT DO NOTHING;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(560, NULL, 'SCREEN', '보고서 목록 관리', 560, 'SCR-REPORT-LIST-MGMT', '/admin/reports', 'file-text', 'BUSINESS', '보고서 ID·템플릿·데이터셋 사용여부 관리', 'Y', 'ACTIVE', 1),
(561, NULL, 'SCREEN', '보고서 양식 관리', 561, 'SCR-REPORT-FORM-VERSION-MGMT', '/admin/report-form-versions', 'file-cog', 'BUSINESS', '보고서별 양식 버전과 시행일 관리', 'Y', 'ACTIVE', 1),
(562, NULL, 'SCREEN', '보고서 권한 관리', 562, 'SCR-REPORT-PERMISSION-MGMT', '/admin/report-permissions', 'shield', 'BUSINESS', '보고서별 출력 권한과 데이터 범위 관리', 'Y', 'ACTIVE', 1),
(563, NULL, 'SCREEN', '보고서 출력 이력', 563, 'SCR-REPORT-PRINT-HISTORY', '/admin/report-print-histories', 'history', 'BUSINESS', '보고서 출력·다운로드 이력 조회', 'Y', 'ACTIVE', 1),
(564, NULL, 'SCREEN', '대량 출력 관리', 564, 'SCR-BULK-REPORT-JOB-MGMT', '/admin/bulk-report-jobs', 'files', 'BUSINESS', '대량 보고서 생성 작업과 결과 관리', 'Y', 'ACTIVE', 1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, m.menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-54 보고서 관리 메뉴 권한'
FROM menus m
JOIN (VALUES
    ('SCR-REPORT-LIST-MGMT', 'R04'),
    ('SCR-REPORT-LIST-MGMT', 'R09'),
    ('SCR-REPORT-FORM-VERSION-MGMT', 'R04'),
    ('SCR-REPORT-FORM-VERSION-MGMT', 'R09'),
    ('SCR-REPORT-PERMISSION-MGMT', 'R04'),
    ('SCR-REPORT-PERMISSION-MGMT', 'R09'),
    ('SCR-REPORT-PRINT-HISTORY', 'R04'),
    ('SCR-REPORT-PRINT-HISTORY', 'R08'),
    ('SCR-REPORT-PRINT-HISTORY', 'R09'),
    ('SCR-BULK-REPORT-JOB-MGMT', 'R03'),
    ('SCR-BULK-REPORT-JOB-MGMT', 'R04'),
    ('SCR-BULK-REPORT-JOB-MGMT', 'R09')
) AS role_seed(screen_id, role_code) ON role_seed.screen_id = m.screen_id
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;
