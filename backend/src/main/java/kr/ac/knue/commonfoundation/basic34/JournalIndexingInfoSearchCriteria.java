package kr.ac.knue.commonfoundation.basic34;

public record JournalIndexingInfoSearchCriteria(
        int page,
        int pageSize,
        Long ruleVersionId,
        String issn,
        String journalName,
        String indexingType,
        String publicationCountry,
        String activeYn,
        String keyword
) {
    public int safeSize() {
        if (pageSize == 50 || pageSize == 100) return pageSize;
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedIssn() { return normalize(issn); }
    public String normalizedJournalName() { return normalize(journalName); }
    public String normalizedIndexingType() { return normalize(indexingType); }
    public String normalizedPublicationCountry() { return normalize(publicationCountry); }
    public String normalizedActiveYn() { return normalize(activeYn); }
    public String normalizedKeyword() { return normalize(keyword); }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
