package kr.ac.knue.commonfoundation.basic36;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ResearcherProfileMapperContractTest {
    private final String mapperXml;

    ResearcherProfileMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/basic36/ResearcherProfileMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void researcherProfileReadQueriesUseExplicitResultMapsForAllRecordDtos() {
        assertThat(mapperXml)
                .contains("<resultMap id=\"ResearcherResearchFieldRowMap\"")
                .contains("<resultMap id=\"ResearcherCareerRowMap\"")
                .contains("<resultMap id=\"ResearcherDegreeRowMap\"")
                .contains("<resultMap id=\"ResearcherCertificationRowMap\"")
                .contains("<select id=\"listResearchFields\" resultMap=\"ResearcherResearchFieldRowMap\">")
                .contains("<select id=\"listCareers\" resultMap=\"ResearcherCareerRowMap\">")
                .contains("<select id=\"listDegrees\" resultMap=\"ResearcherDegreeRowMap\">")
                .contains("<select id=\"listCertifications\" resultMap=\"ResearcherCertificationRowMap\">")
                .doesNotContain("resultType=\"kr.ac.knue.commonfoundation.basic36.ResearcherResearchFieldRow\"")
                .doesNotContain("resultType=\"kr.ac.knue.commonfoundation.basic36.ResearcherCareerRow\"")
                .doesNotContain("resultType=\"kr.ac.knue.commonfoundation.basic36.ResearcherDegreeRow\"")
                .doesNotContain("resultType=\"kr.ac.knue.commonfoundation.basic36.ResearcherCertificationRow\"");
    }
}
