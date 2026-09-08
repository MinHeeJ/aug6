package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDate;

public record ReportOutputResponse(String reportId,
                                   String reportName,
                                   String datasetCode,
                                   String outputFormat,
                                   LocalDate outputBaseDate,
                                   Long formVersionId,
                                   String formVersionName,
                                   String formFileRef,
                                   int outputCount,
                                   String fileRef,
                                   String requestId) {
}
