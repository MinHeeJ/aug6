package kr.ac.knue.commonfoundation.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic38SeedAndQueryContractTest {
    @Test
    void basic38AddsRelatedExampleDataForResearcherBatchAndExcelInquiryPages() throws Exception {
        String sql = new ClassPathResource("db/migration/V40__basic38_menu_search_and_sample_data.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("BASIC-38")
                .contains("E1002", "E1003")
                .contains("researcher_profiles", "researcher_degrees")
                .contains("korus_faculty_sync_runs", "korus_faculty_sync_results")
                .contains("SEED-BASIC38-BATCH-RESULT-001")
                .contains("SEED-BASIC38-EXCEL-TEMPLATE-002")
                .contains("SEED-BASIC38-EXCEL-UPLOAD-ERROR")
                .contains("excel_upload_histories", "excel_upload_errors")
                .contains("ON CONFLICT")
                .contains("COMMENT ON");
    }

    @Test
    void relevantInquiryMappersUseDynamicPredicatesWithoutNullBoundFilters() throws Exception {
        String researcher = resource("mapper/basic36/ResearcherProfileMapper.xml");
        String korus = resource("mapper/basic36/KorusFacultySyncMapper.xml");
        String batch = resource("mapper/batch/BatchResultMapper.xml");
        String excel = resource("mapper/excel/ExcelOperationsMapper.xml");
        String all = String.join("\n", researcher, korus, batch, excel).toLowerCase();

        assertThat(all)
                .doesNotContain("is null or")
                .doesNotContain("? is null")
                .doesNotContain("coalesce(:")
                .doesNotContain("#{param} is null");
        assertThat(researcher).contains("<if test=\"criteria.normalizedEmployeeNo != null\">");
        assertThat(korus).contains("<if test=\"criteria.normalizedEmployeeNo != null\">");
        assertThat(batch).contains("<where>", "<if test=\"criteria.batchId != null and criteria.batchId != ''\">");
        assertThat(excel).contains("<if test=\"businessType != null and businessType != ''\">");
    }

    private String resource(String path) throws Exception {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
