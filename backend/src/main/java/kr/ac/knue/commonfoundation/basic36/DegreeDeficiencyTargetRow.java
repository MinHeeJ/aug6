package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record DegreeDeficiencyTargetRow(String targetId, String researcherProfileId, String facultyId,
                                        String facultyName, String organizationCode, String organizationName,
                                        String finalDegreeType, String deficiencyReason, LocalDateTime updatedAt) {
}
