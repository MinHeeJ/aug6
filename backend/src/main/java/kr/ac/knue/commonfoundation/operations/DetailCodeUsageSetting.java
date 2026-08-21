package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DetailCodeUsageSetting(String groupId, String codeValue, String codeName, String systemUseYn, LocalDate validStartDate, LocalDate validEndDate, String usageChangeReason, LocalDateTime updatedAt) {
}
