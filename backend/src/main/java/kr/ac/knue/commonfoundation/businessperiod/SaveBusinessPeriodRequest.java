package kr.ac.knue.commonfoundation.businessperiod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SaveBusinessPeriodRequest(
        Long settingId,
        @NotBlank(message = "평가연도를 입력하세요.") String evaluationYear,
        String areaCode,
        @NotBlank(message = "소속/학과 코드를 입력하세요.") String organizationCode,
        String userTypeCode,
        @NotNull(message = "시작일시를 입력하세요.") LocalDateTime startAt,
        @NotNull(message = "종료일시를 입력하세요.") LocalDateTime endAt,
        @NotNull(message = "기준일자를 입력하세요.") LocalDate baseDate,
        @NotBlank(message = "사용여부를 선택하세요.") String activeYn,
        @NotBlank(message = "변경 사유를 입력하세요.") String changeReason
) {}
