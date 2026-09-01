package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record ResearcherProfileListRow(String researcherProfileId, String facultyId, String facultyName,
                                       String organizationCode, String organizationName, String rankName,
                                       String employmentStatus, String finalDegreeType, String profileStatus,
                                       String informationPublicYn, LocalDateTime updatedAt) {
}
