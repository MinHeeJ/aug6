package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic16FileOperationsSchemaMigrationTest {
    private final String migrationSql;

    Basic16FileOperationsSchemaMigrationTest() throws Exception {
        migrationSql = new ClassPathResource("db/migration/V6__basic16_file_operations.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void createsFilePolicyAttachmentIntegrityAndMalwareTablesIdempotentlyWithCommentsForPhase2() {
        for (String table : new String[] {
                "file_policies",
                "attachment_files",
                "attachment_delete_history",
                "attachment_integrity_checks",
                "attachment_integrity_findings",
                "malware_scan_results"}) {
            assertThat(migrationSql).contains("create table if not exists " + table);
            assertThat(migrationSql).contains("comment on table " + table);
        }
        assertThat(migrationSql).contains("constraint uq_file_policies_business_type unique (business_type)");
        assertThat(migrationSql).contains("create index if not exists idx_attachment_files_business_record");
        assertThat(migrationSql).contains("create index if not exists idx_malware_scan_results_file");
    }

    @Test
    void seedsBasic16ScreensAndDoesNotCreateNewRoleDefinitions() {
        assertThat(migrationSql).contains("scr-file-policy-mgmt");
        assertThat(migrationSql).contains("/admin/file-policies");
        assertThat(migrationSql).contains("scr-attachment-metadata");
        assertThat(migrationSql).contains("scr-attachment-delete");
        assertThat(migrationSql).contains("scr-attachment-integrity");
        assertThat(migrationSql).contains("insert into menu_permissions");
        assertThat(migrationSql).contains("'r09'");
        assertThat(migrationSql).doesNotContain("insert into roles");
        assertThat(migrationSql).doesNotContain("'r10'");
    }

    @Test
    void attachmentStorageColumnsAreMarkedInternalAndSoftDeleteIsModeledWithoutPhysicalDelete() {
        assertThat(migrationSql).contains("stored_filename varchar(255) not null");
        assertThat(migrationSql).contains("storage_path varchar(1000) not null");
        assertThat(migrationSql).contains("api/ui 응답에 노출하지 않는다");
        assertThat(migrationSql).contains("deleted_at timestamp");
        assertThat(migrationSql).contains("deleted_by bigint");
        assertThat(migrationSql).contains("delete_reason varchar(500)");
        assertThat(migrationSql).doesNotContain("delete from attachment_files");
    }

    @Test
    void integrityAndMalwareEnumsMatchApprovedPhase2Contract() {
        assertThat(migrationSql).contains("'missing_business_ref'");
        assertThat(migrationSql).contains("'missing_storage_file'");
        assertThat(migrationSql).contains("'duplicate_file'");
        assertThat(migrationSql).contains("'infected'");
        assertThat(migrationSql).contains("'failed'");
        assertThat(migrationSql).contains("'timeout'");
        assertThat(migrationSql).contains("ck_malware_scan_block_reason");
    }
}
