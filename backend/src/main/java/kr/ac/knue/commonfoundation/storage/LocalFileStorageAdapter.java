package kr.ac.knue.commonfoundation.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Safe default local byte retention. System temp storage does not guarantee survival across redeployment. */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {
    private final Path root = Path.of(System.getProperty("java.io.tmpdir"), "knue-employment-files");

    @Override
    public String save(byte[] bytes) {
        String token = UUID.randomUUID().toString();
        try {
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root)) {
                throw new IOException("Unsafe storage directory");
            }
            Files.write(path(token), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return token;
        } catch (IOException exception) {
            throw new IllegalStateException("파일을 보존하지 못했습니다.", exception);
        }
    }

    @Override
    public byte[] read(String token) {
        try {
            if (!exists(token)) {
                throw new IOException("Missing file");
            }
            return Files.readAllBytes(path(token));
        } catch (IOException exception) {
            throw new IllegalStateException("파일을 읽지 못했습니다.", exception);
        }
    }

    @Override
    public boolean exists(String token) {
        return token != null && token.matches("[a-f0-9-]{36}")
                && Files.isRegularFile(path(token), LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public long size(String token) {
        try {
            return exists(token) ? Files.size(path(token)) : Long.MAX_VALUE;
        } catch (IOException exception) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public void delete(String token) {
        try {
            Files.deleteIfExists(path(token));
        } catch (IOException exception) {
            throw new IllegalStateException("고아 파일 정리가 필요합니다: " + token, exception);
        }
    }

    private Path path(String token) {
        if (token == null || !token.matches("[a-f0-9-]{36}")) {
            throw new IllegalArgumentException("유효하지 않은 파일 참조입니다.");
        }
        return root.resolve(token);
    }
}
