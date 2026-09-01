package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDateTime;

public record ResearcherCareerRow(Long careerId, String employeeNo, String workStartYm, String workEndYm,
                                  String workplace, String positionName, String duty, Long changedBy,
                                  LocalDateTime changedAt) {
}
