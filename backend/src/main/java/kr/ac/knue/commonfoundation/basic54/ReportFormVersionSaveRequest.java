package kr.ac.knue.commonfoundation.basic54;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReportFormVersionSaveRequest(Long formVersionId,
                                           @NotBlank(message = "보고서 ID를 선택하세요.") String reportId,
                                           @NotBlank(message = "버전명을 입력하세요.") String versionName,
                                           @NotNull(message = "시행일을 입력하세요.") LocalDate effectiveDate,
                                           @NotBlank(message = "양식 파일 참조를 입력하세요.") String formFileRef,
                                           String currentYn,
                                           @NotBlank(message = "변경 사유를 입력하세요.") String changeReason) {
}
