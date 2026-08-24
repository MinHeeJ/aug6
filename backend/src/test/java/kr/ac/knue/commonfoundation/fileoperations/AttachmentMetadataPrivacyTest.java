package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AttachmentMetadataPrivacyTest {
    @Test
    void publicAttachmentMetadataSerializationDoesNotExposeStoragePathOrStoredFilenameForT006() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        AttachmentFileRow row = new AttachmentFileRow(1001L, "FACULTY_EVALUATION", "FE-2026-0001", "IN_PROGRESS",
                "업적증빙.pdf", "pdf", 102400L, 2L, LocalDateTime.parse("2026-08-24T09:00:00"), "CLEAN", null);

        String json = objectMapper.writeValueAsString(row);

        assertThat(json).contains("originalFilename");
        assertThat(json).contains("업적증빙.pdf");
        assertThat(json).doesNotContain("storagePath");
        assertThat(json).doesNotContain("storedFilename");
        assertThat(json).doesNotContain("/secure/attachments");
    }

    @Test
    void attachmentMapperPublicColumnsNeverSelectInternalStorageFieldsForT007() throws Exception {
        String mapperXml = new ClassPathResource("mapper/fileoperations/AttachmentFileMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();

        String publicColumns = mapperXml.substring(
                mapperXml.indexOf("<sql id=\"publicattachmentcolumns\""),
                mapperXml.indexOf("</sql>", mapperXml.indexOf("<sql id=\"publicattachmentcolumns\"")));
        assertThat(publicColumns).doesNotContain("stored_filename");
        assertThat(publicColumns).doesNotContain("storage_path");
        assertThat(mapperXml).contains("select id=\"listvisiblebybusinessrecordid\"");
        assertThat(mapperXml).contains("business_record_id = #{businessrecordid}");
        assertThat(mapperXml).contains("deleted_at is null");
    }
}
