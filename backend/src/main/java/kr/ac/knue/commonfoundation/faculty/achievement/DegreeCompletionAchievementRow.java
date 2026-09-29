package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;
import java.util.List;

public record DegreeCompletionAchievementRow(Long achievementId, String managementNo, String evaluationYear, String managementItemCode,
                                             LocalDate occurredDate, String certificationStatus, boolean attachmentAvailable,
                                             List<DegreeCompletionStudentRow> students) { }