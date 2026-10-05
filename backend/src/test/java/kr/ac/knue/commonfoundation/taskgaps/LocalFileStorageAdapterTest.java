package kr.ac.knue.commonfoundation.taskgaps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Contract tests for opaque-token persistence used by the employment-rate Excel
 * workflow without exposing a physical storage path to upload history records.
 */
class LocalFileStorageAdapterTest {
    @TempDir
    Path storageRoot;

    @Test
    void storesOpensAndDeletesAnOpaqueTokenPayload() throws Exception {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(storageRoot);

        FileStoragePort.StoredFile stored = adapter.store(
                "../../employment-rate.csv",
                "text/csv",
                new ByteArrayInputStream("교번,관리항목코드".getBytes(StandardCharsets.UTF_8)));

        assertThat(stored.fileToken()).startsWith("file-");
        assertThat(stored.originalFileName()).isEqualTo("employment-rate.csv");
        assertThat(stored.contentType()).isEqualTo("text/csv");
        assertThat(stored.sizeBytes()).isPositive();

        try (var content = adapter.open(stored.fileToken())) {
            assertThat(new String(content.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("교번,관리항목코드");
        }

        adapter.delete(stored.fileToken());
        assertThat(Files.exists(storageRoot.resolve(stored.fileToken()))).isFalse();
    }

    @Test
    void rejectsTraversalAndNonOpaqueTokens() {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(storageRoot);

        assertThatThrownBy(() -> adapter.open("../excel_upload_files"))
                .isInstanceOf(IOException.class)
                .hasMessage("Invalid file token");
        assertThatThrownBy(() -> adapter.delete("file_unsafe"))
                .isInstanceOf(IOException.class)
                .hasMessage("Invalid file token");
    }
}
