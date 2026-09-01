package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record ResearcherProfileSummary(String employeeNo, String name, String organizationCode, String organizationName,
                                       String rankName, String appointmentId, String contact, String researcherRegistrationNo,
                                       String externalProvisionYn, String informationPublicYn, String finalDegreeType,
                                       boolean degreePrerequisiteMissing, LocalDateTime updatedAt) {
}
