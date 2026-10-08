package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Opaque local file adapter with rollback cleanup; requires no external storage configuration. */
@Component
public class EmploymentRateFileStorage {
    private final Path root = Path.of(System.getProperty("java.io.tmpdir"), "employment-rate-files");

    public String store(byte[] content, String requestId) {
        String token = UUID.randomUUID().toString();
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(token), content, StandardOpenOption.CREATE_NEW);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) remove(token, requestId);
                }
            });
        }
        return token;
    }

    public byte[] read(String token) {
        if (token == null || !token.matches("[a-f0-9-]{36}")) {
            throw new kr.ac.knue.commonfoundation.common.api.NotFoundException("파일이 없습니다.");
        }
        try {
            return Files.readAllBytes(root.resolve(token));
        } catch (IOException missing) {
            throw new kr.ac.knue.commonfoundation.common.api.NotFoundException("파일이 없습니다.");
        }
    }

    void remove(String token, String requestId) {
        try {
            Files.deleteIfExists(root.resolve(token));
        } catch (IOException failure) {
            try {
                Files.writeString(root.resolve(token + ".cleanup"), token,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException markerFailure) {
                // Structured logging remains available even if the local filesystem is unavailable.
            }
            LoggerFactory.getLogger(getClass()).warn("event=file_cleanup_failed requestId={} token={}",
                    requestId == null ? "" : requestId.replaceAll("[\\r\\n]", ""), token);
        }
    }
}
