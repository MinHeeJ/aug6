package kr.ac.knue.commonfoundation.basic33;

public record EvaluationManagementItemSearchCriteria(
        int page,
        int size,
        Long ruleVersionId,
        String areaCode,
        String itemCode,
        String evaluationYear,
        String elementCode,
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

    public String normalizedItemCode() {
        return itemCode == null || itemCode.isBlank() ? null : itemCode.trim().toUpperCase();
    }

    public String normalizedElementCode() {
        return elementCode == null || elementCode.isBlank() ? null : elementCode.trim().toUpperCase();
    }
}
