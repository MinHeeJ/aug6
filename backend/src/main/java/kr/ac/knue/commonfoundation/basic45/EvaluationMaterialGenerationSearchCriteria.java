package kr.ac.knue.commonfoundation.basic45;

public record EvaluationMaterialGenerationSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        String targetUserId) {
    public int safeSize() {
        if (size == 50 || size == 100) {
            return size;
        }
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() {
        return trimToNull(evaluationYear);
    }

    public String normalizedAreaCode() {
        return trimToNull(areaCode);
    }

    public String normalizedOrganizationCode() {
        return trimToNull(organizationCode);
    }

    public Long normalizedTargetUserId() {
        String value = trimToNull(targetUserId);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
