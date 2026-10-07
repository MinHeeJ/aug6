package kr.ac.knue.commonfoundation.courseoperations;

/** Already-normalized bind parameters; no helper method is treated as a MyBatis property. */
public record CourseOperationSearch(
        int page, int pageSize, long offset, String managementItemCode, String achievementStatus) {
}
