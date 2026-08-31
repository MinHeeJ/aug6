package kr.ac.knue.commonfoundation.basic33;

public record AreaElementSystemSearchCriteria(
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
        if (size == 50 || size == 100) {
            return size;
        }
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedAreaCode() {
        return normalize(areaCode);
    }

    public String normalizedItemCode() {
        return normalize(itemCode);
    }

    public String normalizedElementCode() {
        return normalize(elementCode);
    }

    public String normalizedKeyword() {
        return normalize(keyword);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
