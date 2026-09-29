package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;
import java.util.List;

public record SaveDegreeCompletionAchievementRequest(String managementNo, String evaluationYear, String managementItemCode,
                                                     LocalDate occurredDate, List<DegreeCompletionStudentRequest> students) { }