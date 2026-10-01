package kr.ac.knue.commonfoundation.achievement;

/** Atomic commit result for a validated student-guidance Excel upload. */
public record StudentGuidanceExcelCommitResult(String uploadId, int savedCount) {
}
