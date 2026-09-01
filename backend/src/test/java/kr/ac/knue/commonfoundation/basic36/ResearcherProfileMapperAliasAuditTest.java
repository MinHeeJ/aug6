package kr.ac.knue.commonfoundation.basic36;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResearcherProfileMapperAliasAuditTest {
    @Test
    void researcherLookupSelectsUseQuotedCamelCaseAliasesForReq1152() throws Exception {
        String mapperXml = Files.readString(Path.of("src/main/resources/mapper/basic36/ResearcherProfileMapper.xml"));
        List<String> requiredAliases = List.of(
                "as \"facultyId\"",
                "as \"facultyName\"",
                "as \"organizationCode\"",
                "as \"organizationName\"",
                "as \"researcherProfileId\"",
                "as \"profileStatus\"",
                "as \"targetId\"",
                "as \"deficiencyReason\""
        );

        for (String alias : requiredAliases) {
            assertThat(mapperXml).contains(alias);
        }
        assertThat(mapperXml).doesNotContain("? IS NULL OR");
        assertThat(mapperXml).doesNotContain(":param IS NULL OR");
        assertThat(mapperXml).doesNotContain("COALESCE(:param");
        assertThat(mapperXml).doesNotContain("#{criteria.normalizedKeyword} IS NULL");
    }
}
