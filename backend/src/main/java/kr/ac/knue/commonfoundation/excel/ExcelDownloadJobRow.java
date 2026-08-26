package kr.ac.knue.commonfoundation.excel;

public record ExcelDownloadJobRow(String downloadId, Long requesterUserId, String outputType, String queryCondition,
        String dataScopeRef, String fileToken, String originalFileName, String status) {
}
