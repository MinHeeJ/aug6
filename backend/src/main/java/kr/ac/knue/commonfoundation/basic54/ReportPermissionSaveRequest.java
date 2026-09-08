package kr.ac.knue.commonfoundation.basic54;

import jakarta.validation.constraints.NotBlank;

public record ReportPermissionSaveRequest(Long permissionId,
                                          @NotBlank(message = "권한 대상 유형을 선택하세요.") String granteeType,
                                          @NotBlank(message = "권한 대상을 입력하세요.") String granteeId,
                                          @NotBlank(message = "보고서 ID를 선택하세요.") String reportId,
                                          @NotBlank(message = "조회 권한을 선택하세요.") String allowViewYn,
                                          @NotBlank(message = "미리보기 권한을 선택하세요.") String allowPreviewYn,
                                          @NotBlank(message = "출력 권한을 선택하세요.") String allowPrintYn,
                                          @NotBlank(message = "PDF 권한을 선택하세요.") String allowPdfYn,
                                          @NotBlank(message = "Excel 권한을 선택하세요.") String allowExcelYn,
                                          @NotBlank(message = "데이터 범위를 입력하세요.") String dataScope,
                                          @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
                                          String changeReason) {
}
