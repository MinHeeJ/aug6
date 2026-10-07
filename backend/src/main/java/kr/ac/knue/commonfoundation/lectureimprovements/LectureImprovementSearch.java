package kr.ac.knue.commonfoundation.lectureimprovements;

/** Validated pagination and optional filters; mapper bindings are actual record properties. */
public record LectureImprovementSearch(
        int page, int pageSize, long offset, String managementItemCode, Integer academicYear, Integer semester) {
}
