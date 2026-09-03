package kr.ac.knue.commonfoundation.basic48;

public record EvaluationSnapshotExcelDownload(
        String fileName,
        String contentType,
        long rowCount,
        String downloadScope,
        String requestId) {
}
