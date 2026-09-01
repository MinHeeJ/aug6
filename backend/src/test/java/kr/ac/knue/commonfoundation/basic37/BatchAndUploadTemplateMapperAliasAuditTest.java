package kr.ac.knue.commonfoundation.basic37;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import kr.ac.knue.commonfoundation.excel.ExcelTemplateRow;
import org.junit.jupiter.api.Test;

class BatchAndUploadTemplateMapperAliasAuditTest {
    @Test
    void batchResultMapperUsesQuotedCamelCaseAliasesForResultAndLogDtos() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/batch/BatchResultMapper.xml"));

        assertThat(xml)
                .contains("ber.started_at as \"startedAt\"")
                .contains("ber.ended_at as \"endedAt\"")
                .contains("ber.total_count as \"totalCount\"")
                .contains("ber.success_count as \"successCount\"")
                .contains("ber.failure_count as \"failureCount\"")
                .contains("ber.excluded_count as \"excludedCount\"")
                .contains("ber.elapsed_millis as \"elapsedMillis\"")
                .contains("log_file_ref as \"logFileRef\"")
                .doesNotContain(" as startedAt")
                .doesNotContain(" as logFileRef");
    }

    @Test
    void uploadTemplateMapperUsesExplicitResultMapInsteadOfRecordAutoMappingForRulesProjection() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/excel/ExcelOperationsMapper.xml"));

        assertThat(xml)
                .contains("<resultMap id=\"ExcelTemplateRowMap\"")
                .contains("<select id=\"listUploadTemplates\" resultMap=\"ExcelTemplateRowMap\"")
                .contains("<select id=\"findUploadTemplate\" resultMap=\"ExcelTemplateRowMap\"")
                .doesNotContain("resultType=\"kr.ac.knue.commonfoundation.excel.ExcelTemplateRow\"");
    }

    @Test
    void excelTemplateRowProvidesMapperConstructorBeforeRulesAreAttachedByService() {
        List<Class<?>> constructorTypes = List.of(String.class, String.class, String.class,
                java.time.LocalDate.class, String.class, String.class, String.class, String.class);

        assertThat(Arrays.stream(ExcelTemplateRow.class.getDeclaredConstructors())
                .map(Constructor::getParameterTypes)
                .map(Arrays::asList))
                .contains(constructorTypes);
    }
}
