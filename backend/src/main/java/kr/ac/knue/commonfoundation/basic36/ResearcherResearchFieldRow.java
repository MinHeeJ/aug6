package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record ResearcherResearchFieldRow(Long researchFieldId, String employeeNo, String majorName,
                                         String detailMajorName, String majorSeries, Long changedBy,
                                         LocalDateTime changedAt) {
}
