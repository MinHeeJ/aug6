package kr.ac.knue.commonfoundation.excel;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Local evidence storage with a safe default; failed compensation leaves a cleanup marker. */
@Component
public class LocalExcelFileStorage implements FileStoragePort {
    private final Path root = Path.of(System.getProperty("java.io.tmpdir"), "common-foundation-excel");

    public String save(byte[] content) {
        String token = UUID.randomUUID().toString();
        try {
            Files.createDirectories(root);
            Files.write(path(token), content, StandardOpenOption.CREATE_NEW);
            return token;
        } catch (IOException error) {
            throw new IllegalStateException("파일을 보관할 수 없습니다.", error);
        }
    }

    public byte[] read(String token) {
        try {
            return Files.readAllBytes(path(token));
        } catch (IOException error) {
            throw new IllegalStateException("파일을 읽을 수 없습니다.", error);
        }
    }

    public void delete(String token) {
        try {
            Files.deleteIfExists(path(token));
        } catch (IOException error) {
            try {
                Files.writeString(root.resolve(token + ".cleanup"), "Retained file cleanup required");
            } catch (IOException markerError) {
                error.addSuppressed(markerError);
                throw new IllegalStateException("파일 정리 기록에 실패했습니다.", error);
            }
        }
    }

    private Path path(String token) {
        if (token == null || !token.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("유효하지 않은 파일 식별자입니다.");
        }
        return root.resolve(token);
    }
}
