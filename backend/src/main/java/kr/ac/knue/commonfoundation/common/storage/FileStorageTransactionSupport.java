package kr.ac.knue.commonfoundation.common.storage;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Couples newly uploaded files to a DB transaction without deleting pre-existing attachment references. */
@Component
public class FileStorageTransactionSupport {
    private static final Logger log = LoggerFactory.getLogger(FileStorageTransactionSupport.class);
    private final FileStoragePort storage;

    public FileStorageTransactionSupport(FileStoragePort storage) {
        this.storage = storage;
    }

    /** Saves a new file and compensates only definite rollback; unknown outcomes retain recoverable bytes. */
    public StoredFile save(Long ownerUserId, String name, String contentType, byte[] bytes) throws IOException {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("신규 파일 저장에는 활성 transaction이 필요합니다.");
        }
        StoredFile file = storage.save(ownerUserId, name, contentType, bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storage.delete(file.fileId(), file.ownerUserId());
                    } catch (Exception exception) {
                        log.warn("New uploaded file rollback cleanup requires retry: {}", file.fileId());
                        try {
                            storage.recordCleanupFailure(file.fileId(), file.ownerUserId());
                        } catch (Exception journalFailure) {
                            log.error("Unable to record file cleanup retry: {}", file.fileId());
                        }
                    }
                }
            }
        });
        return file;
    }
}
