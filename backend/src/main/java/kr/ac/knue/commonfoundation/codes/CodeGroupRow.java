package kr.ac.knue.commonfoundation.codes;

import java.time.LocalDateTime;

public record CodeGroupRow(
        String groupId,
        String groupName,
        String description,
        String managingDepartment,
        String systemUseYn,
        String status,
        long detailCodeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
