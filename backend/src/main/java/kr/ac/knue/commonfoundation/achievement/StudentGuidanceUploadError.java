package kr.ac.knue.commonfoundation.achievement;
/** A row-level Excel validation failure safe to expose to the uploader. */
public record StudentGuidanceUploadError(int rowNumber, String columnName, String errorCode, String errorReason) {}
