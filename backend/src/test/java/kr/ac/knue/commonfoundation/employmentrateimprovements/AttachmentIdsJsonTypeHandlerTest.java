package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.sql.ResultSet;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

/** Verifies resilient attachment JSON materialization and mapper type-handler registration. */
class AttachmentIdsJsonTypeHandlerTest {
    private final AttachmentIdsJsonTypeHandler typeHandler = new AttachmentIdsJsonTypeHandler();

    @Test
    void materializesOnlyTextAttachmentIdsFromJsonArray() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("attachment_ids_json"))
                .thenReturn("[\"file-a\", \"  \", 17, null, \"file-b\"]");

        List<String> attachmentIds = typeHandler.getNullableResult(resultSet, "attachment_ids_json");

        assertThat(attachmentIds).containsExactly("file-a", "file-b");
    }

    @Test
    void treatsNullAndMalformedAttachmentJsonAsAnEmptyList() throws Exception {
        ResultSet nullResult = mock(ResultSet.class);
        when(nullResult.getString("attachment_ids_json")).thenReturn(null);
        ResultSet malformedResult = mock(ResultSet.class);
        when(malformedResult.getString("attachment_ids_json")).thenReturn("not-json");
        ResultSet objectResult = mock(ResultSet.class);
        when(objectResult.getString("attachment_ids_json")).thenReturn("{\"file\":\"file-a\"}");

        assertThat(typeHandler.getNullableResult(nullResult, "attachment_ids_json")).isEmpty();
        assertThat(typeHandler.getNullableResult(malformedResult, "attachment_ids_json")).isEmpty();
        assertThat(typeHandler.getNullableResult(objectResult, "attachment_ids_json")).isEmpty();
    }

    @Test
    void mapperXmlParsesWithTheTypedAttachmentListHandler() throws Exception {
        String resource = "mapper/employmentrateimprovements/EmploymentRateImprovementMapper.xml";
        Configuration configuration = new Configuration();

        try (Reader reader = Resources.getResourceAsReader(resource)) {
            new XMLMapperBuilder(
                    reader,
                    configuration,
                    resource,
                    configuration.getSqlFragments())
                    .parse();
        }

        assertThat(configuration.getMappedStatementNames())
                .contains("kr.ac.knue.commonfoundation.employmentrateimprovements."
                        + "EmploymentRateImprovementMapper.list");
    }
}
