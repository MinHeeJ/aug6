package kr.ac.knue.commonfoundation.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Safe local storage with durable ownership metadata and a system-temp default for existing deployments. */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {
    private static final Pattern OPAQUE_ID = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private final Path root;
    private final long maxBytes;

    public LocalFileStorageAdapter(
            @Value("${file-storage.directory:${java.io.tmpdir}/common-foundation-files}") String directory,
            @Value("${file-storage.max-bytes:10485760}") long maxBytes) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("파일 크기 제한은 양수여야 합니다.");
        }
        this.maxBytes = maxBytes;
    }

    /** Writes uploaded bytes only, cleaning partial new files if metadata persistence fails. */
    @Override
    public StoredFile save(Long ownerUserId, String originalFileName, String contentType, byte[] bytes)
            throws IOException {
        requireOwner(ownerUserId);
        if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
            throw invalidFile();
        }
        if (originalFileName == null || originalFileName.isBlank() || originalFileName.length() > 255
                || originalFileName.contains("/") || originalFileName.contains("\\")
                || originalFileName.codePoints().anyMatch(Character::isISOControl)) {
            throw invalidFile();
        }
        String safeContentType = contentType == null ? "application/octet-stream" : contentType;
        if (safeContentType.length() > 100 || safeContentType.codePoints().anyMatch(Character::isISOControl)) {
            throw invalidFile();
        }
        ensureRoot();
        String id = UUID.randomUUID().toString();
        Path data = resolve(id, ".bin");
        Path metadata = resolve(id, ".properties");
        try {
            Files.write(data, bytes, StandardOpenOption.CREATE_NEW);
            Properties properties = new Properties();
            properties.setProperty("owner", ownerUserId.toString());
            properties.setProperty("name", originalFileName);
            properties.setProperty("contentType", safeContentType);
            properties.setProperty("size", String.valueOf(bytes.length));
            try (OutputStream output = Files.newOutputStream(metadata, StandardOpenOption.CREATE_NEW)) {
                properties.store(output, "opaque uploaded file");
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(metadata);
                Files.deleteIfExists(data);
            } catch (IOException cleanup) {
                exception.addSuppressed(cleanup);
                recordCleanupFailure(id, ownerUserId);
            }
            throw exception;
        }
        return new StoredFile(id, ownerUserId, originalFileName, safeContentType, bytes.length);
    }

    /** Rejects absent, malformed, tampered or foreign references identically to avoid ownership leakage. */
    @Override
    public StoredFile find(String fileId, Long ownerUserId) throws IOException {
        requireOwner(ownerUserId);
        Path metadata = resolve(fileId, ".properties");
        Path data = resolve(fileId, ".bin");
        requireRegularFile(metadata);
        requireRegularFile(data);
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(metadata, LinkOption.NOFOLLOW_LINKS)) {
            properties.load(input);
        }
        try {
            Long storedOwner = Long.valueOf(properties.getProperty("owner"));
            long size = Long.parseLong(properties.getProperty("size"));
            if (!storedOwner.equals(ownerUserId) || size <= 0 || size > maxBytes || Files.size(data) != size) {
                throw invalidFile();
            }
            return new StoredFile(fileId, storedOwner, properties.getProperty("name"),
                    properties.getProperty("contentType"), size);
        } catch (IllegalArgumentException exception) {
            throw invalidFile();
        }
    }

    @Override
    public byte[] read(String fileId, Long ownerUserId) throws IOException {
        StoredFile file = find(fileId, ownerUserId);
        try (InputStream input = Files.newInputStream(resolve(fileId, ".bin"), LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes((int) Math.min(file.sizeBytes() + 1, Integer.MAX_VALUE));
            if (bytes.length != file.sizeBytes()) {
                throw invalidFile();
            }
            return bytes;
        }
    }

    @Override
    public void delete(String fileId, Long ownerUserId) throws IOException {
        find(fileId, ownerUserId);
        Files.delete(resolve(fileId, ".bin"));
        Files.delete(resolve(fileId, ".properties"));
    }

    @Override
    public synchronized void recordCleanupFailure(String fileId, Long ownerUserId) throws IOException {
        requireOwner(ownerUserId);
        resolve(fileId, ".bin");
        ensureRoot();
        Path journal = root.resolve("cleanup-pending.log");
        if (Files.isSymbolicLink(journal)) {
            throw new IOException("파일 정리 기록을 저장할 수 없습니다.");
        }
        try (OutputStream output = Files.newOutputStream(journal, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS)) {
            output.write((fileId + " " + ownerUserId + System.lineSeparator())
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private Path resolve(String fileId, String suffix) {
        if (fileId == null || !OPAQUE_ID.matcher(fileId).matches()) {
            throw invalidFile();
        }
        return root.resolve(fileId + suffix);
    }

    private void ensureRoot() throws IOException {
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root) || !root.toRealPath().equals(root)) {
            throw new IOException("파일 저장소를 사용할 수 없습니다.");
        }
    }

    private void requireRegularFile(Path path) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            throw invalidFile();
        }
        ensureRoot();
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw invalidFile();
        }
    }

    private void requireOwner(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw invalidFile();
        }
    }

    private BusinessValidationException invalidFile() {
        return new BusinessValidationException("첨부 파일을 확인할 수 없습니다.",
                List.of(new ValidationError("attachmentIds", "실제 본인 소유 파일을 선택하세요.")));
    }
}
