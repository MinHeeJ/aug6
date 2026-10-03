package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;

/** Shared Excel validation outcome exposed by the employment-rate upload operation. */
public record EmploymentRateExcelUploadResult(
        String uploadId,
        String originalFileName,
        int totalCount,
        int successCount,
        int errorCount,
        List<ExcelUploadErrorRow> errors) {
}
