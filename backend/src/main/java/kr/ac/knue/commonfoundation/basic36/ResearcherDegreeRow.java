package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record ResearcherDegreeRow(Long degreeId, String employeeNo, String degreeType, String universityName,
                                  String startYm, String acquiredYm, String countryName, String collegeName,
                                  String advisorName, Long changedBy, LocalDateTime changedAt) {
}
