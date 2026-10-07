package kr.ac.knue.commonfoundation.common.storage;

import java.io.IOException;

/** Stores real uploaded bytes and resolves only opaque identifiers owned by the caller. */
public interface FileStoragePort {
    StoredFile save(Long ownerUserId, String originalFileName, String contentType, byte[] bytes) throws IOException;

    StoredFile find(String fileId, Long ownerUserId) throws IOException;

    byte[] read(String fileId, Long ownerUserId) throws IOException;

    void delete(String fileId, Long ownerUserId) throws IOException;

    /** Keeps a durable retry record when rollback compensation cannot remove a new file. */
    void recordCleanupFailure(String fileId, Long ownerUserId) throws IOException;
}
