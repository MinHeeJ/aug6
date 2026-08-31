package kr.ac.knue.commonfoundation.basic34;

import java.util.List;

public record JournalIndexingInfoSearchResponse(
        List<JournalIndexingInfoRow> journalIndexingInfos,
        int page,
        int pageSize,
        long totalElements
) {}
