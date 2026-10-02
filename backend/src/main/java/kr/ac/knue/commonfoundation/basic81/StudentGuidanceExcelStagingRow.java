package kr.ac.knue.commonfoundation.basic81;

/** CSV staging row retained until the entire student-guidance upload is committed. */
public record StudentGuidanceExcelStagingRow(int rowNumber, String payload) {
}
