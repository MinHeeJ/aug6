package kr.ac.knue.commonfoundation.basic32;

import java.time.LocalDateTime;

public record DeletedBusinessDataRow(
        Long deletedDataId,
        String businessType,
        String originalKey,
        Long deletedBy,
        String deletedByLoginId,
        String deletedByName,
        LocalDateTime deletedAt,
        String deleteReason,
        String recoverableYn) {
}
