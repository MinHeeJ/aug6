package kr.ac.knue.commonfoundation.exceptionperiod;

import java.time.LocalDateTime;

public record ExceptionPeriodRow(
        Long settingId,
        String evaluationYear,
        Long teacherUserId,
        String teacherName,
        String areaCode,
        String targetFunctionCode,
        LocalDateTime exceptionStartAt,
        LocalDateTime exceptionEndAt,
        String approvalReason,
        String activeYn,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String changeReason
) {
}
