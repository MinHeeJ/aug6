package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Owner-scoped files default to application temporary storage; an optional trusted directory retains them longer. */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {
    private final Path root;

    public LocalFileStorageAdapter(
            @Value("${employment-rate.storage-directory:${java.io.tmpdir}/commonfoundation/employment-rate-files}")
            String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String save(long ownerUserId, String purpose, byte[] content) {
        if (content == null || content.length == 0 || content.length > 64 * 1024 * 1024) {
            throw new IllegalArgumentException("저장할 파일 크기가 올바르지 않습니다.");
        }
        String ref = "erf-" + UUID.randomUUID();
        Path path = file(ownerUserId, ref);
        try {
            Files.createDirectories(path.getParent());
            checkDirectories(path.getParent());
            Files.write(path, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
            return ref;
        } catch (IOException exception) {
            try { Files.deleteIfExists(path); } catch (IOException cleanup) { exception.addSuppressed(cleanup); }
            throw new IllegalStateException("파일을 보존하지 못했습니다.", exception);
        }
    }

    @Override
    public byte[] read(long ownerUserId, String reference) {
        Path path = file(ownerUserId, reference);
        try {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 64 * 1024 * 1024) {
                throw new IllegalArgumentException("보존 파일을 찾을 수 없습니다.");
            }
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = input.readNBytes(64 * 1024 * 1024 + 1);
                if (bytes.length > 64 * 1024 * 1024) throw new IllegalStateException("파일 크기 제한 초과");
                return bytes;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("보존 파일을 읽지 못했습니다.", exception);
        }
    }

    @Override
    public boolean exists(long ownerUserId, String reference) {
        try { return Files.isRegularFile(file(ownerUserId, reference), LinkOption.NOFOLLOW_LINKS); }
        catch (IllegalArgumentException | IllegalStateException exception) { return false; }
    }

    @Override
    public void delete(long ownerUserId, String reference) {
        try { Files.deleteIfExists(file(ownerUserId, reference)); }
        catch (IOException exception) { throw new IllegalStateException("파일 삭제 보상 실패: " + reference, exception); }
    }

    private Path file(long ownerUserId, String reference) {
        if (ownerUserId <= 0 || reference == null
                || !reference.matches("erf-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("파일 참조가 올바르지 않습니다.");
        }
        Path owner = root.resolve(String.valueOf(ownerUserId));
        checkDirectories(owner);
        Path path = owner.resolve(reference);
        if (Files.isSymbolicLink(path)) throw new IllegalStateException("심볼릭 링크 파일은 허용하지 않습니다.");
        return path;
    }

    private void checkDirectories(Path path) {
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IllegalStateException("심볼릭 링크 저장 경로는 허용하지 않습니다.");
        }
    }
}
