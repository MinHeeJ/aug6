package kr.ac.knue.commonfoundation.excel;

import java.util.List;

public record ExcelUploadHistorySearchResponse(List<ExcelUploadHistoryRow> histories, int page, int size, long totalElements) {
}
