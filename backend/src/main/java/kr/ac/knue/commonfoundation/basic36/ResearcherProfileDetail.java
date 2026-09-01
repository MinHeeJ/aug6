package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;
import java.util.List;

public record ResearcherProfileDetail(String employeeNo, String name, String organizationCode, String organizationName,
                                      String rankName, String appointmentId, String contact, String researcherRegistrationNo,
                                      String externalProvisionYn, String informationPublicYn, String finalDegreeType,
                                      LocalDateTime updatedAt, List<ResearcherResearchFieldRow> researchFields,
                                      List<ResearcherCareerRow> careers, List<ResearcherDegreeRow> degrees,
                                      List<ResearcherCertificationRow> certifications, boolean degreePrerequisiteMissing) {
    public ResearcherProfileDetail(ResearcherProfileSummary summary, List<ResearcherResearchFieldRow> researchFields,
                                   List<ResearcherCareerRow> careers, List<ResearcherDegreeRow> degrees,
                                   List<ResearcherCertificationRow> certifications, boolean degreePrerequisiteMissing) {
        this(summary.employeeNo(), summary.name(), summary.organizationCode(), summary.organizationName(), summary.rankName(),
                summary.appointmentId(), summary.contact(), summary.researcherRegistrationNo(), summary.externalProvisionYn(),
                summary.informationPublicYn(), summary.finalDegreeType(), summary.updatedAt(), researchFields, careers, degrees,
                certifications, degreePrerequisiteMissing);
    }
}
