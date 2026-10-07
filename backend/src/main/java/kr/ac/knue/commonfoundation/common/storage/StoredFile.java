package kr.ac.knue.commonfoundation.common.storage;

/** Durable opaque file metadata; no physical storage path is exposed to callers. */
public record StoredFile(
        String fileId,
        Long ownerUserId,
        String originalFileName,
        String contentType,
        long sizeBytes) {
}
