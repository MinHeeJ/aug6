package kr.ac.knue.commonfoundation.basic48;

public record ScoreRecalculationHistoryExcelDownload(
        String fileName,
        String contentType,
        long rowCount,
        String description,
        String requestId
) {
}
