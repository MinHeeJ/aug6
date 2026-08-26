package kr.ac.knue.commonfoundation.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Basic26ScopeBoundaryTest {
    private static final List<String> EXCEL_SOURCE_ROOTS = List.of(
            "src/main/java/kr/ac/knue/commonfoundation/excel",
            "src/main/resources/mapper/excel",
            "src/main/resources/db/migration/V15__basic26_excel_foundation.sql");
    private static final List<String> FORBIDDEN_BUSINESS_RULE_TERMS = List.of(
            "교수업적평가 개별",
            "학술지원금",
            "점수판정",
            "신청 업무규칙",
            "evaluation_score",
            "grant_application");
    private static final List<String> FORBIDDEN_ATTACHMENT_POLICY_TERMS = List.of(
            "attachment_policy",
            "attachment_policies",
            "file_extension_policies",
            "첨부파일 정책 테이블",
            "신규 첨부파일 정책");

    @Test
    void excelImplementationDoesNotIntroduceIndividualAchievementOrGrantBusinessRules() throws Exception {
        String source = readExcelImplementationSources().toLowerCase(Locale.ROOT);

        for (String forbidden : FORBIDDEN_BUSINESS_RULE_TERMS) {
            assertThat(source)
                    .as("BASIC-26 must stay within common Excel operations and not add individual achievement/grant rules")
                    .doesNotContain(forbidden.toLowerCase(Locale.ROOT));
        }
    }

    @Test
    void excelImplementationReusesExistingAttachmentPolicyScopeWithoutCreatingNewPolicyArtifacts() throws Exception {
        String source = readExcelImplementationSources().toLowerCase(Locale.ROOT);

        for (String forbidden : FORBIDDEN_ATTACHMENT_POLICY_TERMS) {
            assertThat(source)
                    .as("BASIC-26 must not add a new attachment policy table/screen/API")
                    .doesNotContain(forbidden.toLowerCase(Locale.ROOT));
        }
    }

    private String readExcelImplementationSources() throws Exception {
        StringBuilder builder = new StringBuilder();
        for (String root : EXCEL_SOURCE_ROOTS) {
            Path path = Path.of(root);
            if (Files.isDirectory(path)) {
                try (Stream<Path> files = Files.walk(path)) {
                    files.filter(Files::isRegularFile)
                            .filter(file -> file.toString().endsWith(".java") || file.toString().endsWith(".xml") || file.toString().endsWith(".sql"))
                            .forEach(file -> appendFile(builder, file));
                }
            } else if (Files.exists(path)) {
                appendFile(builder, path);
            }
        }
        return builder.toString();
    }

    private void appendFile(StringBuilder builder, Path file) {
        try {
            builder.append('\n').append(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read " + file, ex);
        }
    }
}
