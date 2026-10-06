package kr.ac.knue.commonfoundation.excel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Local opaque storage; EXCEL_STORAGE_ROOT is required, and rollback deletes only new files. */
@Component
public class LocalExcelFileStorageAdapter implements FileStoragePort {
    private final Path root;

    public LocalExcelFileStorageAdapter(@Value("${EXCEL_STORAGE_ROOT:}") String configuredRoot) {
        root = configuredRoot == null || configuredRoot.isBlank() ? null
                : Path.of(configuredRoot).toAbsolutePath().normalize();
    }

    /** Writes new owner-bound bytes and compensates them in afterCompletion on rollback. */
    @Override
    public String save(byte[] content, long ownerUserId) {
        if (content == null) throw new IllegalArgumentException("Missing file content");
        Path directory = directory(ownerUserId);
        String reference = UUID.randomUUID().toString();
        Path file = directory.resolve(reference);
        boolean created = false;
        try {
            Files.createDirectories(directory);
            rejectSymlinks(directory);
            Files.createFile(file);
            created = true;
            Files.write(file, content, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException ex) {
            // Never compensate a pre-existing file, even when CREATE_NEW collides.
            if (created) compensate(file, ex);
            throw new IllegalStateException("Excel file storage failed", ex);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) compensate(file, null);
                }
            });
        }
        return reference;
    }

    /** Rejects traversal, unconfigured storage, symlinks and references belonging to another owner. */
    @Override
    public boolean isOwned(String reference, long ownerUserId) {
        if (root == null || reference == null || !reference.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) return false;
        Path directory = directory(ownerUserId);
        rejectSymlinks(directory);
        return Files.isRegularFile(directory.resolve(reference), LinkOption.NOFOLLOW_LINKS);
    }

    /** Reads a regular file only through a validated owner-bound reference. */
    @Override
    public byte[] readOwned(String reference, long ownerUserId) {
        if (!isOwned(reference, ownerUserId)) throw new IllegalArgumentException("Unowned file reference");
        try (var input = Files.newInputStream(directory(ownerUserId).resolve(reference), LinkOption.NOFOLLOW_LINKS)) {
            return input.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Excel file read failed", ex);
        }
    }

    private Path directory(long ownerUserId) {
        if (root == null) throw new IllegalStateException("EXCEL_STORAGE_ROOT must be configured");
        if (ownerUserId <= 0) throw new IllegalArgumentException("Invalid file owner");
        Path directory = root.resolve(Long.toString(ownerUserId)).normalize();
        rejectSymlinks(directory);
        return directory;
    }

    private void rejectSymlinks(Path directory) {
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IllegalStateException("Symlink storage path rejected");
        }
    }

    private void compensate(Path file, Exception original) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException failure) {
            try {
                // Durable diagnostic is independent of the rolled-back DB transaction.
                Files.writeString(root.resolve("rollback-compensation.log"),
                        Instant.now() + " " + file.getParent().getFileName() + "/" + file.getFileName()
                                + " " + failure.getClass().getName() + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS);
            } catch (IOException diagnosticFailure) {
                failure.addSuppressed(diagnosticFailure);
            }
            if (original != null) original.addSuppressed(failure);
            org.slf4j.LoggerFactory.getLogger(getClass()).error("File rollback compensation failed", failure);
        }
    }
}
