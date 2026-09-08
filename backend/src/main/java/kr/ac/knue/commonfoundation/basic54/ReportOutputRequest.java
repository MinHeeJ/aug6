package kr.ac.knue.commonfoundation.basic54;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

public record ReportOutputRequest(String reportId,
                                  @NotBlank(message = "출력 형식을 선택하세요.") String outputFormat,
                                  LocalDate outputBaseDate,
                                  @NotEmpty(message = "출력 대상을 선택하세요.") List<Long> targetPersonIds,
                                  @NotBlank(message = "대상 범위 설명을 입력하세요.") String targetSummary) {
}
