package kr.ac.knue.commonfoundation.fileoperations;

import java.time.LocalDateTime;

public record FilePolicyRow(
        Long filePolicyId,
        String businessType,
        String allowedExtensions,
        Integer maxFileSizeMb,
        Integer maxFilesPerItem,
        Integer maxTotalSizeMb,
        Integer maxFilenameLength,
        String malwareScanEnabled,
        LocalDateTime updatedAt) {
}
