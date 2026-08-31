package kr.ac.knue.commonfoundation.basic33;

public record EvaluationItemSearchCriteria(
        int page,
        int size,
        Long ruleVersionId,
        String areaCode,
        String activeYn,
        String keyword) {
    public int safeSize() {
        if (size != 20 && size != 50 && size != 100) {
            return 20;
        }
        return size;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public String normalizedAreaCode() {
        return areaCode == null || areaCode.isBlank() ? null : areaCode.trim().toUpperCase();
    }
}
