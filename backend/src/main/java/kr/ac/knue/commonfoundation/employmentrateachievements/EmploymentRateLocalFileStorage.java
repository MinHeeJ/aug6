package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/** Private application temp storage; no caller filename or path is used as a filesystem identifier. */
@Component
public class EmploymentRateLocalFileStorage implements EmploymentRateFileStoragePort {
    private final Path root = Path.of(System.getProperty("java.io.tmpdir"), "knue-employment-rate-files");

    private Path path(String token) {
        if (token == null || !token.matches("[a-f0-9-]{36}(-errors)?")) {
            throw new IllegalArgumentException("잘못된 파일 식별자입니다.");
        }
        return root.resolve(token);
    }

    public void put(String token, byte[] bytes) {
        try {
            Files.createDirectories(root);
            Files.write(path(token), bytes, java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("파일을 보존하지 못했습니다.", exception);
        }
    }

    public byte[] read(String token) {
        try {
            return Files.readAllBytes(path(token));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("보존 파일을 읽지 못했습니다.", exception);
        }
    }

    public void delete(String token) {
        try {
            Files.deleteIfExists(path(token));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("새 파일 보상 삭제를 완료하지 못했습니다.", exception);
        }
    }
}
