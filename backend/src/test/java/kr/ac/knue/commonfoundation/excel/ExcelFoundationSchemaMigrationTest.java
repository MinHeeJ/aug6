package kr.ac.knue.commonfoundation.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExcelFoundationSchemaMigrationTest {
    private final String sql;

    ExcelFoundationSchemaMigrationTest() throws Exception {
        sql = new ClassPathResource("db/migration/V15__basic26_excel_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void excelUploadTemplateTablesPreserveVersionRulesAndDownloadTokens() {
        assertTableCreatedWithComment("excel_upload_templates");
        assertTableCreatedWithComment("excel_upload_template_rules");
        assertTableCreatedWithComment("excel_upload_template_files");

        assertThat(sql)
                .contains("template_id varchar(100) primary key")
                .contains("business_type varchar(50) not null")
                .contains("template_version varchar(50) not null")
                .contains("effective_date date not null")
                .contains("unique (business_type, template_version, effective_date)")
                .contains("required_column varchar(200) not null")
                .contains("column_order integer not null")
                .contains("code_rule_ref varchar(200) not null")
                .contains("file_token varchar(200) primary key")
                .contains("original_file_name varchar(255) not null")
                .contains("create index if not exists idx_excel_upload_templates_lookup")
                .doesNotContain("storage_path", "stored_file_name");
    }

    @Test
    void excelUploadFilesErrorsAndHistoriesKeepCountsAndBlockPhysicalDeleteByStatus() {
        assertTableCreatedWithComment("excel_upload_files");
        assertTableCreatedWithComment("excel_upload_staging_rows");
        assertTableCreatedWithComment("excel_upload_errors");
        assertTableCreatedWithComment("excel_upload_histories");

        assertThat(sql)
                .contains("upload_id varchar(100) primary key")
                .contains("validation_status varchar(20) not null")
                .contains("check (validation_status in ('normal','error','excluded'))")
                .contains("row_number integer not null")
                .contains("column_name varchar(200) not null")
                .contains("error_code varchar(50) not null")
                .contains("error_reason varchar(500) not null")
                .contains("correction_guide varchar(500)")
                .contains("total_count integer not null default 0")
                .contains("success_count integer not null default 0")
                .contains("error_count integer not null default 0")
                .contains("excluded_count integer not null default 0")
                .contains("saved_count integer not null default 0")
                .contains("processing_time_millis bigint")
                .contains("comment on column excel_upload_errors.status is 'active:활성|inactive:비활성|deleted:삭제표시'");
    }

    @Test
    void excelDownloadJobsPreserveQueryConditionDataScopeAndFileTokenOnly() {
        assertTableCreatedWithComment("excel_download_jobs");

        assertThat(sql)
                .contains("download_id varchar(100) primary key")
                .contains("requester_user_id bigint not null")
                .contains("output_type varchar(30) not null")
                .contains("check (output_type in ('target','status','error'))")
                .contains("query_condition jsonb not null default '{}'::jsonb")
                .contains("data_scope_ref varchar(200)")
                .contains("file_token varchar(200) not null")
                .contains("foreign key (requester_user_id) references users(user_id)")
                .contains("create index if not exists idx_excel_download_jobs_requester")
                .doesNotContain("storage_path", "stored_file_name");
    }

    @Test
    void seedDataProvidesExcelFixturesAndMenuPermissionsForR09() {
        assertThat(sql)
                .contains("seed-excel-template-001")
                .contains("seed-excel-upload-valid")
                .contains("seed-excel-upload-error")
                .contains("seed-excel-error-001")
                .contains("scr-upload-template-mgmt")
                .contains("scr-excel-upload-mgmt")
                .contains("scr-upload-history-mgmt")
                .contains("scr-upload-error-mgmt")
                .contains("scr-excel-download-mgmt")
                .contains("'role', 'r09', menu_id, 'allow'");
    }

    private void assertTableCreatedWithComment(String table) {
        assertThat(sql)
                .contains("create table if not exists " + table)
                .contains("comment on table " + table);
    }
}
