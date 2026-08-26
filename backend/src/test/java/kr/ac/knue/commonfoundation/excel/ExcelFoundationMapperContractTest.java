package kr.ac.knue.commonfoundation.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExcelFoundationMapperContractTest {
    private final String mapperXml;

    ExcelFoundationMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/excel/ExcelFoundationMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void mapperUsesQuotedCamelCaseAliasesForMapResults() {
        assertThat(mapperXml)
                .contains("as \"templateId\"", "as \"businessType\"", "as \"templateVersion\"")
                .contains("as \"uploadId\"", "as \"originalFileName\"", "as \"validationStatus\"")
                .contains("as \"rowNumber\"", "as \"columnName\"", "as \"errorReason\"")
                .contains("as \"totalCount\"", "as \"successCount\"", "as \"savedCount\"")
                .contains("as \"downloadId\"", "as \"outputType\"", "as \"fileToken\"");
    }

    @Test
    void optionalFiltersAreAddedDynamicallyWithoutNullBoundPredicates() {
        assertThat(mapperXml)
                .contains("<if test=\"businessType != null and businessType != ''\">")
                .contains("<if test=\"effectiveDate != null and effectiveDate != ''\">")
                .contains("<if test=\"originalFileName != null and originalFileName != ''\">")
                .contains("<if test=\"requesterUserId != null\">")
                .doesNotContain("IS NULL OR", "is null or", "COALESCE(", "coalesce(:", "? IS NULL", "#{businessType} IS NULL");
    }

    @Test
    void mapperCoversFoundationTablesWithReadQueriesForLaterSlices() {
        assertThat(mapperXml)
                .contains("from excel_upload_templates")
                .contains("excel_upload_template_files")
                .contains("from excel_upload_files")
                .contains("from excel_upload_errors")
                .contains("from excel_upload_histories")
                .contains("from excel_download_jobs")
                .contains("limit #{limit} offset #{offset}");
    }
}
