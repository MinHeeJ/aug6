package kr.ac.knue.commonfoundation.basic48;

public record EvaluationSnapshotSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String finalizationPoint,
        EvaluationSnapshotDataScope dataScope,
        String organizationCode) {
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
        return normalize(evaluationYear);
    }

    public String normalizedFinalizationPoint() {
        return normalize(finalizationPoint);
    }

    public String normalizedOrganizationCode() {
        return normalize(organizationCode);
    }

    public boolean organizationScope() {
        return dataScope == EvaluationSnapshotDataScope.ORGANIZATION;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
