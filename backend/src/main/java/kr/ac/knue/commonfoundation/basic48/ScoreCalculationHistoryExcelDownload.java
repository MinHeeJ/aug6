package kr.ac.knue.commonfoundation.basic48;

public record ScoreCalculationHistoryExcelDownload(
        String fileName,
        String contentType,
        long rowCount,
        String description,
        String requestId
) {
}
