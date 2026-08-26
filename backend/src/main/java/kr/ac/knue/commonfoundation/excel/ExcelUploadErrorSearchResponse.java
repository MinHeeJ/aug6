package kr.ac.knue.commonfoundation.excel;

import java.util.List;

public record ExcelUploadErrorSearchResponse(List<ExcelUploadErrorRow> errors, int page, int size, long totalElements) {
}
