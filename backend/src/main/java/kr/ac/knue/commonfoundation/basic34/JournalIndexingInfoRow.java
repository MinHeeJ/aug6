package kr.ac.knue.commonfoundation.basic34;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JournalIndexingInfoRow(
        Long journalIndexingInfoId,
        Long ruleVersionId,
        String versionCode,
        String versionStatus,
        String issn,
        String journalName,
        String indexingType,
        String indexingTypeName,
        String publicationCountry,
        LocalDate validStartDate,
        LocalDate validEndDate,
        String sourceName,
        LocalDateTime sourceUpdatedAt,
        String activeYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt
) {}
