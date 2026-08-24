package kr.ac.knue.commonfoundation.fileoperations;

public record AttachmentDeleteTargetResponse(
        Long fileId,
        String businessType,
        String businessRecordId,
        String businessRecordStatus,
        String businessRecordSummary,
        String originalFilename,
        String extension,
        Long fileSizeBytes,
        Long uploadedBy,
        String uploadedAt,
        String malwareScanStatus,
        boolean finalizedBlocked) {
    public static AttachmentDeleteTargetResponse from(AttachmentFileRow row) {
        boolean finalized = "EVALUATION_CONFIRMED".equals(row.businessRecordStatus());
        return new AttachmentDeleteTargetResponse(
                row.fileId(),
                row.businessType(),
                row.businessRecordId(),
                row.businessRecordStatus(),
                row.businessType() + " / " + row.businessRecordId() + " / " + statusLabel(row.businessRecordStatus()),
                row.originalFilename(),
                row.extension(),
                row.fileSizeBytes(),
                row.uploadedBy(),
                row.uploadedAt() == null ? null : row.uploadedAt().toString(),
                row.malwareScanStatus(),
                finalized);
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "IN_PROGRESS" -> "진행중";
            case "EVALUATION_CONFIRMED" -> "평가확정";
            case "UNPUBLISHED" -> "미공개";
            case "INACTIVE" -> "비활성";
            default -> status;
        };
    }
}
