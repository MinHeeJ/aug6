package kr.ac.knue.commonfoundation.basic54;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

public record BulkReportJobCreateRequest(@NotBlank(message = "보고서 ID를 선택하세요.") String reportId,
                                         @NotEmpty(message = "출력 대상을 선택하세요.") List<Long> targetPersonIds,
                                         @NotBlank(message = "대상 조건 해시를 입력하세요.") String targetHash,
                                         String outputFormat,
                                         LocalDate outputBaseDate) {
}
