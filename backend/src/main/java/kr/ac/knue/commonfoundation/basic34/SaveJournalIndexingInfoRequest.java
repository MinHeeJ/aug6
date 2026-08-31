package kr.ac.knue.commonfoundation.basic34;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SaveJournalIndexingInfoRequest(
        @NotNull(message = "규정버전을 선택하세요.") Long ruleVersionId,
        @NotBlank(message = "ISSN을 입력하세요.") String issn,
        @NotBlank(message = "학술지명을 입력하세요.") String journalName,
        @NotBlank(message = "등재구분을 선택하세요.") String indexingType,
        @NotBlank(message = "발행국가를 입력하세요.") String publicationCountry,
        @NotNull(message = "유효시작일을 입력하세요.") LocalDate validStartDate,
        @NotNull(message = "유효종료일을 입력하세요.") LocalDate validEndDate,
        @NotBlank(message = "출처를 입력하세요.") String sourceName,
        @NotNull(message = "갱신일시를 입력하세요.") LocalDateTime sourceUpdatedAt,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason
) {}
