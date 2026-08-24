package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttachmentIntegrityClassifierTest {
    private final AttachmentIntegrityClassifier classifier = new AttachmentIntegrityClassifier();

    @Test
    void classifiesMissingBusinessRefMissingStorageFileAndDuplicateFileForT010AndT011() {
        AttachmentFileInternalRow existing = file(1001L, "exists.bin", "/store");
        AttachmentFileInternalRow missingStorage = file(1002L, "missing.bin", "/store");
        AttachmentFileInternalRow duplicateA = file(1003L, "dup.bin", "/store");
        AttachmentFileInternalRow duplicateB = file(1004L, "dup.bin", "/store");
        List<StorageObjectSnapshot> storageObjects = List.of(
                new StorageObjectSnapshot("/store/exists.bin"),
                new StorageObjectSnapshot("/store/dup.bin"),
                new StorageObjectSnapshot("/store/orphan.bin"));

        List<AttachmentIntegrityFindingDraft> findings = classifier.classify(
                List.of(existing, missingStorage, duplicateA, duplicateB), storageObjects);

        assertThat(findings).extracting(AttachmentIntegrityFindingDraft::anomalyType)
                .contains(
                        AttachmentIntegrityClassifier.MISSING_BUSINESS_REF,
                        AttachmentIntegrityClassifier.MISSING_STORAGE_FILE,
                        AttachmentIntegrityClassifier.DUPLICATE_FILE);
        assertThat(findings).anySatisfy(finding -> {
            assertThat(finding.anomalyType()).isEqualTo(AttachmentIntegrityClassifier.MISSING_BUSINESS_REF);
            assertThat(finding.fileId()).isNull();
            assertThat(finding.storageObjectRef()).isEqualTo("/store/orphan.bin");
        });
        assertThat(findings).anySatisfy(finding -> {
            assertThat(finding.anomalyType()).isEqualTo(AttachmentIntegrityClassifier.MISSING_STORAGE_FILE);
            assertThat(finding.fileId()).isEqualTo(1002L);
        });
        assertThat(findings.stream().filter(finding -> AttachmentIntegrityClassifier.DUPLICATE_FILE.equals(finding.anomalyType())).count())
                .isEqualTo(2L);
    }

    private AttachmentFileInternalRow file(Long fileId, String storedFilename, String storagePath) {
        return new AttachmentFileInternalRow(fileId, "FACULTY_EVALUATION", "FE-2026", "IN_PROGRESS", "원본.pdf",
                storedFilename, storagePath, "pdf", 1024L, 2L, LocalDateTime.parse("2026-08-24T09:00:00"),
                "CLEAN", null, null, null);
    }
}
