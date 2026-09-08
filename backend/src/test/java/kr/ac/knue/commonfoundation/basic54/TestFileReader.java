package kr.ac.knue.commonfoundation.basic54;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class TestFileReader {
    private TestFileReader() {
    }

    static String read(String path) {
        try {
            Path requested = Path.of(path);
            Path resolved = resolve(requested);
            return Files.readString(resolved, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("테스트 fixture 파일을 읽을 수 없습니다: " + path, ex);
        }
    }

    private static Path resolve(Path requested) {
        if (Files.exists(requested)) {
            return requested;
        }
        if (requested.getNameCount() > 1 && "backend".equals(requested.getName(0).toString())) {
            Path backendRelative = requested.subpath(1, requested.getNameCount());
            if (Files.exists(backendRelative)) {
                return backendRelative;
            }
        }
        Path parentRelative = Path.of("..").resolve(requested).normalize();
        if (Files.exists(parentRelative)) {
            return parentRelative;
        }
        return requested;
    }
}
