package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.common.storage.FileStorageTransactionSupport;
import kr.ac.knue.commonfoundation.common.storage.LocalFileStorageAdapter;
import kr.ac.knue.commonfoundation.common.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** Drives Spring's real synchronization lifecycle so rollback/commit file effects are exercised. */
class FileStorageTransactionSupportTest {
    @TempDir
    Path directory;

    @Test
    void definiteRollbackDeletesOnlyNewUploadAndKeepsExistingFile() throws Exception {
        FileStoragePort storage = new LocalFileStorageAdapter(directory.toString(), 10);
        StoredFile existing = storage.save(101L, "existing", null, new byte[] {1});
        FileStorageTransactionSupport support = new FileStorageTransactionSupport(storage);
        AtomicReference<StoredFile> fresh = new AtomicReference<>();
        TransactionTemplate transaction = new TransactionTemplate(new SynchronizingTransactionManager());

        assertThatThrownBy(() -> transaction.execute(status -> {
            fresh.set(save(support));
            throw new IllegalStateException("database write rejected");
        })).isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> storage.find(fresh.get().fileId(), 101L))
                .isInstanceOf(BusinessValidationException.class);
        assertThat(storage.read(existing.fileId(), 101L)).containsExactly((byte) 1);
    }

    @Test
    void successfulCommitPreservesNewUploadedBytes() throws Exception {
        FileStoragePort storage = new LocalFileStorageAdapter(directory.toString(), 10);
        FileStorageTransactionSupport support = new FileStorageTransactionSupport(storage);
        TransactionTemplate transaction = new TransactionTemplate(new SynchronizingTransactionManager());
        StoredFile file = transaction.execute(status -> save(support));
        assertThat(storage.read(file.fileId(), 101L)).containsExactly((byte) 2);
    }

    @Test
    void failedCompensationRetainsBytesAndWritesDurableRetryRecord() throws Exception {
        FileStoragePort storage = spy(new LocalFileStorageAdapter(directory.toString(), 10));
        FileStorageTransactionSupport support = new FileStorageTransactionSupport(storage);
        TransactionTemplate transaction = new TransactionTemplate(new SynchronizingTransactionManager());
        AtomicReference<StoredFile> file = new AtomicReference<>();
        transaction.execute(status -> {
            file.set(save(support));
            try {
                doThrow(new IOException("disk failure")).when(storage).delete(file.get().fileId(), 101L);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
            status.setRollbackOnly();
            return null;
        });
        assertThat(storage.read(file.get().fileId(), 101L)).containsExactly((byte) 2);
        assertThat(Files.readString(directory.resolve("cleanup-pending.log")))
                .contains(file.get().fileId() + " 101");
    }

    @Test
    void noTransactionRejectsBeforeCreatingFiles() {
        FileStorageTransactionSupport support = new FileStorageTransactionSupport(
                new LocalFileStorageAdapter(directory.toString(), 10));
        assertThatThrownBy(() -> support.save(101L, "new", null, new byte[] {2}))
                .isInstanceOf(IllegalStateException.class);
    }

    private StoredFile save(FileStorageTransactionSupport support) {
        try {
            return support.save(101L, "new", null, new byte[] {2});
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** DB-free manager delegates synchronization ordering to Spring, not to a mocked transaction status. */
    private static class SynchronizingTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // No external resources; Spring still owns synchronization initialization.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // Spring invokes afterCompletion(COMMITTED) after this boundary.
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // Spring invokes afterCompletion(ROLLED_BACK) after this boundary.
        }
    }
}
