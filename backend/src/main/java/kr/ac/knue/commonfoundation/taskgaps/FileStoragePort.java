package kr.ac.knue.commonfoundation.taskgaps;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stores opaque file payloads outside business tables so upload history can
 * retain only a non-path file token and user-facing metadata.
 */
public interface FileStoragePort {
    /**
     * Persists the supplied stream and returns metadata containing an opaque,
     * externally safe token. Implementations must not expose the disk path.
     *
     * @param originalFileName client-provided display name after boundary validation
     * @param contentType declared media type, if available
     * @param content stream to persist; the caller retains ownership and closes it
     * @return immutable token and persisted-file metadata
     * @throws IOException when the file cannot be durably stored
     */
    StoredFile store(
            String originalFileName,
            String contentType,
            InputStream content) throws IOException;

    /**
     * Opens a previously stored payload by opaque token.
     *
     * @param fileToken externally safe file token
     * @return stream for the stored payload; the caller must close it
     * @throws IOException when the token is invalid or the payload cannot be opened
     */
    InputStream open(String fileToken) throws IOException;

    /**
     * Deletes a stored payload when its owning record has been removed.
     *
     * @param fileToken opaque token returned by {@link #store(String, String, InputStream)}
     * @throws IOException when the token is invalid or deletion fails
     */
    void delete(String fileToken) throws IOException;

    /** Metadata safe to persist in upload/error history and return to callers. */
    record StoredFile(
            String fileToken,
            String originalFileName,
            String contentType,
            long sizeBytes) {
    }
}
