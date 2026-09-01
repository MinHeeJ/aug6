package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record ResearcherCertificationRow(Long certificationId, String employeeNo, String acquiredYm,
                                         String certificationName, String issuingOrganizationName, Long changedBy,
                                         LocalDateTime changedAt) {
}
