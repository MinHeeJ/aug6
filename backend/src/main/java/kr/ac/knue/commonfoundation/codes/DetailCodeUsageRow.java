package kr.ac.knue.commonfoundation.codes;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DetailCodeUsageRow(
        String groupId,
        String codeValue,
        String codeName,
        String systemUseYn,
        LocalDate validStartDate,
        LocalDate validEndDate,
        String status,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt,
        boolean selectableForNewInput) {
}
