package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import kr.ac.knue.commonfoundation.excel.LocalExcelFileStorageAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class EmploymentRateFileStorageTest {
    @TempDir Path root;

    @Test
    void referencesAreOpaqueOwnerBoundAndTraversalIsRejected() {
        var storage = new LocalExcelFileStorageAdapter(root.toString());
        String reference = storage.save(new byte[] {1, 2, 3}, 7L);
        assertThat(reference).doesNotContain(root.toString());
        assertThat(storage.readOwned(reference, 7L)).containsExactly(new byte[] {1, 2, 3});
        assertThat(storage.isOwned(reference, 8L)).isFalse();
        assertThat(storage.isOwned("../" + reference, 7L)).isFalse();
        assertThatThrownBy(() -> storage.readOwned(reference, 8L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rollbackDeletesOnlyNewFilesAndPreservesExistingAttachment() {
        var storage = new LocalExcelFileStorageAdapter(root.toString());
        String existing = storage.save(new byte[] {1}, 7L);
        TransactionSynchronizationManager.initSynchronization();
        try {
            String added = storage.save(new byte[] {2}, 7L);
            for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertThat(storage.isOwned(added, 7L)).isFalse();
            assertThat(storage.isOwned(existing, 7L)).isTrue();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void symlinkOwnerDirectoryIsRejected() throws Exception {
        Path outside = Files.createTempDirectory(root, "outside");
        Files.createSymbolicLink(root.resolve("7"), outside);
        var storage = new LocalExcelFileStorageAdapter(root.toString());
        assertThatThrownBy(() -> storage.save(new byte[] {1}, 7L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void committedFilesSurviveTransactionCompletion() {
        var storage = new LocalExcelFileStorageAdapter(root.toString());
        TransactionSynchronizationManager.initSynchronization();
        try {
            String added = storage.save(new byte[] {2}, 7L);
            for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
            assertThat(storage.readOwned(added, 7L)).containsExactly((byte) 2);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void storageRootIsRequiredForWrites() {
        var storage = new LocalExcelFileStorageAdapter("");
        assertThatThrownBy(() -> storage.save(new byte[] {1}, 7L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("EXCEL_STORAGE_ROOT");
    }
}
