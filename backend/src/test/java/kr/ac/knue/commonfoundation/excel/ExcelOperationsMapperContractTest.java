package kr.ac.knue.commonfoundation.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExcelOperationsMapperContractTest {
    private final String mapperXml;

    ExcelOperationsMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/excel/ExcelOperationsMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void uploadTemplateReadQueriesUseResultMapCompatibleWithTemplateRecordWithoutRulesColumn() {
        assertThat(mapperXml)
                .contains("<resultMap id=\"ExcelTemplateRowMap\"")
                .contains("<select id=\"listUploadTemplates\" resultMap=\"ExcelTemplateRowMap\">")
                .contains("<select id=\"findUploadTemplate\" resultMap=\"ExcelTemplateRowMap\">")
                .doesNotContain("<select id=\"listUploadTemplates\" resultType=\"kr.ac.knue.commonfoundation.excel.ExcelTemplateRow\">")
                .doesNotContain("<select id=\"findUploadTemplate\" resultType=\"kr.ac.knue.commonfoundation.excel.ExcelTemplateRow\">");
    }

    @Test
    void uploadTemplateRowProvidesMapperConstructorAndKeepsRulesDefaultEmpty() throws Exception {
        ExcelTemplateRow row = ExcelTemplateRow.class
                .getConstructor(String.class, String.class, String.class, java.time.LocalDate.class,
                        String.class, String.class, String.class, String.class)
                .newInstance("TPL-1", "PROFESSOR_ACHIEVEMENT", "v1", java.time.LocalDate.parse("2026-01-01"),
                        "Y", "ACTIVE", "token", "template.xlsx");

        assertThat(row.rules()).isEmpty();
    }
}
