package kr.ac.knue.commonfoundation.faculty.achievement;

import java.util.List;

public record DegreeCompletionSearchCriteria(String managementNo, String teacherName, String certificationStatus, int page, int pageSize) {
    int safePageSize() { return List.of(20, 50, 100).contains(pageSize) ? pageSize : 20; }
}