package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.storage.LocalFileStorageAdapter;
import kr.ac.knue.commonfoundation.common.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Exercises actual bytes, durable ownership and path validation without external infrastructure. */
class LocalFileStorageAdapterTest {
    @TempDir
    Path directory;

    @Test
    void storesActualBytesAndResolvesOwnershipAfterAdapterRestart() throws Exception {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString(), 1024);
        byte[] content = {1, 2, 3, 4};
        StoredFile file = storage.save(101L, "실적.xlsx", "application/octet-stream", content);
        LocalFileStorageAdapter restarted = new LocalFileStorageAdapter(directory.toString(), 1024);

        assertThat(file.fileId()).matches("[0-9a-f-]{36}");
        assertThat(restarted.find(file.fileId(), 101L)).isEqualTo(file);
        assertThat(restarted.read(file.fileId(), 101L)).containsExactly(content);
        assertThatThrownBy(() -> restarted.read(file.fileId(), 102L))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> restarted.delete(file.fileId(), 102L))
                .isInstanceOf(BusinessValidationException.class);
        assertThat(restarted.read(file.fileId(), 101L)).containsExactly(content);
        restarted.delete(file.fileId(), 101L);
        assertThatThrownBy(() -> restarted.find(file.fileId(), 101L))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void rejectsTraversalAbsentReferencesAndInvalidUploadBeforeWritingAnything() {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString(), 4);
        assertThatThrownBy(() -> storage.find("../secret", 101L))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> storage.find("00000000-0000-0000-0000-000000000000", 101L))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> storage.save(101L, "../../secret", null, new byte[] {1}))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> storage.save(101L, "file", null, new byte[5]))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> storage.save(101L, "file", null, new byte[0]))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> storage.save(null, "file", null, new byte[] {1}))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void rejectsSymbolicLinksAndTamperedSize() throws Exception {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString(), 10);
        StoredFile file = storage.save(101L, "file", null, new byte[] {1});
        Path data = directory.resolve(file.fileId() + ".bin");
        Files.write(data, new byte[] {1, 2});
        assertThatThrownBy(() -> storage.read(file.fileId(), 101L))
                .isInstanceOf(BusinessValidationException.class);
        Files.delete(data);
        Path target = directory.resolve("secret");
        Files.write(target, new byte[] {1});
        Files.createSymbolicLink(data, target);
        assertThatThrownBy(() -> storage.read(file.fileId(), 101L))
                .isInstanceOf(BusinessValidationException.class);
        assertThat(Files.readAllBytes(target)).containsExactly((byte) 1);
    }

    @Test
    void recordsCleanupRetryWithoutPhysicalPathsOrCredentials() throws Exception {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString(), 10);
        StoredFile file = storage.save(101L, "file", null, new byte[] {1});
        storage.recordCleanupFailure(file.fileId(), 101L);
        assertThat(Files.readString(directory.resolve("cleanup-pending.log")))
                .isEqualTo(file.fileId() + " 101" + System.lineSeparator());
    }

    @Test
    void springWiringUsesSafeDefaultsWithNoNewRequiredConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(LocalFileStorageAdapter.class);
            context.refresh();
            assertThat(context.getBean(LocalFileStorageAdapter.class)).isNotNull();
        }
    }
}
