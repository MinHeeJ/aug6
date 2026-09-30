package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;
/** Read model returned by the student-guidance list and upload commit APIs. */
public record StudentGuidanceAchievementRow(Long achievementId, String managementNo, String evaluationYear, Long teacherUserId, String managementItemCode, LocalDate guidanceStartDate, LocalDate guidanceEndDate, Integer studentCount, String achievementStatus, List<StudentGuidanceStudentRow> students) {}
