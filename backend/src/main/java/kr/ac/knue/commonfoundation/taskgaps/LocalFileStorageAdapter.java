package kr.ac.knue.commonfoundation.taskgaps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * File-system implementation of {@link FileStoragePort} for the shared Excel
 * workflow. It persists opaque-token files below one configured root and never
 * derives an on-disk path from an untrusted display name.
 */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,200}$");

    private final Path storageRoot;

    /**
     * Creates the application adapter using a configurable persistent root.
     * Container deployments should mount {@code app.file-storage.root}.
     */
    @Autowired
    public LocalFileStorageAdapter(
            @Value("${app.file-storage.root:${java.io.tmpdir}/knue-file-storage}") String storageRoot) {
        this(Path.of(storageRoot));
    }

    /** Constructor kept explicit for focused adapter tests and controlled embedding. */
    public LocalFileStorageAdapter(Path storageRoot) {
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(
            String originalFileName,
            String contentType,
            InputStream content) throws IOException {
        Objects.requireNonNull(content, "content must not be null");
        Files.createDirectories(storageRoot);

        String token = "file-" + UUID.randomUUID();
        Path target = resolveToken(token);
        Path temporary = Files.createTempFile(storageRoot, ".upload-", ".tmp");
        try {
            long sizeBytes = Files.copy(content, temporary, StandardCopyOption.REPLACE_EXISTING);
            moveAtomically(temporary, target);
            return new StoredFile(
                    token,
                    sanitizeOriginalFileName(originalFileName),
                    normalizeContentType(contentType),
                    sizeBytes);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public InputStream open(String fileToken) throws IOException {
        return Files.newInputStream(resolveToken(fileToken), StandardOpenOption.READ);
    }

    @Override
    public void delete(String fileToken) throws IOException {
        Files.deleteIfExists(resolveToken(fileToken));
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path resolveToken(String fileToken) throws IOException {
        if (fileToken == null || !TOKEN_PATTERN.matcher(fileToken).matches()) {
            throw new IOException("Invalid file token");
        }
        Path resolved = storageRoot.resolve(fileToken).normalize();
        if (!resolved.getParent().equals(storageRoot)) {
            throw new IOException("Invalid file token");
        }
        return resolved;
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "upload.bin";
        }
        String normalized = originalFileName.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String name = normalized.substring(separator + 1).trim();
        return name.isEmpty() ? "upload.bin" : name;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType.trim();
    }
}
