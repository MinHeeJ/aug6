package kr.ac.knue.commonfoundation.basic54;

import jakarta.validation.constraints.NotBlank;

public record ReportSaveRequest(@NotBlank(message = "보고서 ID를 입력하세요.") String reportId,
                                @NotBlank(message = "보고서명을 입력하세요.") String reportName,
                                @NotBlank(message = "업무구분을 입력하세요.") String businessCategory,
                                @NotBlank(message = "템플릿 파일 참조를 입력하세요.") String templateFileRef,
                                @NotBlank(message = "데이터셋 코드를 입력하세요.") String datasetCode,
                                @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
                                @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}
