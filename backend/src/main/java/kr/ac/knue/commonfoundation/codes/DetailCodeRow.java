package kr.ac.knue.commonfoundation.codes;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DetailCodeRow(
        String groupId,
        String codeValue,
        String codeName,
        String parentCodeValue,
        int sortOrder,
        String additionalAttributes,
        String systemUseYn,
        LocalDate validStartDate,
        LocalDate validEndDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
