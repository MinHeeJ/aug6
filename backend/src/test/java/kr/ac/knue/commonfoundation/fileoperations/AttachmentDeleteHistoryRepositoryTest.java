package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AttachmentDeleteHistoryRepositoryTest {
    private final String mapperXml;

    AttachmentDeleteHistoryRepositoryTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/fileoperations/AttachmentFileMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void logicalDeletePersistsSoftDeleteFieldsAndAuditHistoryForT008AndT009() {
        assertThat(mapperXml).contains("update id=\"marklogicaldeleted\"");
        assertThat(mapperXml).contains("deleted_at = current_timestamp");
        assertThat(mapperXml).contains("deleted_by = #{deletedby}");
        assertThat(mapperXml).contains("delete_reason = #{deletereason}");
        assertThat(mapperXml).contains("insert id=\"insertdeletehistory\"");
        assertThat(mapperXml).contains("attachment_delete_history");
        assertThat(mapperXml).contains("'logical'");
    }

    @Test
    void logicalDeleteDoesNotPhysicallyDeleteAndBlocksEvaluationConfirmedRows() {
        assertThat(mapperXml).doesNotContain("delete from attachment_files");
        assertThat(mapperXml).doesNotContain("delete id=");
        assertThat(mapperXml).contains("business_record_status != 'evaluation_confirmed'");
    }
}
