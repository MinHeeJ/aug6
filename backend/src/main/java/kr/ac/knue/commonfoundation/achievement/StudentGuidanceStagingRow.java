package kr.ac.knue.commonfoundation.achievement;
/** Internal staged upload row retained until the all-or-nothing commit completes. */
public record StudentGuidanceStagingRow(int rowNumber, String payload) {}
