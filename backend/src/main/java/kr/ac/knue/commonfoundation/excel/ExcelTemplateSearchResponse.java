package kr.ac.knue.commonfoundation.excel;

import java.util.List;

public record ExcelTemplateSearchResponse(List<ExcelTemplateRow> templates, int page, int size, long totalElements) {
}
