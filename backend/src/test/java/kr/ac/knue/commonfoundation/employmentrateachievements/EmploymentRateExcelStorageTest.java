package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmploymentRateExcelStorageTest {
    @TempDir Path directory;

    @Test
    void persistsOwnerBoundOpaqueFilesAndDoesNotAllowTraversalOrOtherOwners() {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString());
        String ref = storage.save(107L, "original", new byte[] {1, 2, 3});
        assertThat(storage.read(107L, ref)).containsExactly(1, 2, 3);
        assertThat(storage.exists(108L, ref)).isFalse();
        assertThatThrownBy(() -> storage.read(107L, "../secret"))
                .isInstanceOf(IllegalArgumentException.class);
        storage.delete(107L, ref);
        assertThat(storage.exists(107L, ref)).isFalse();
    }

    @Test
    void ownerIsolationAlsoAppliesToReadAndDelete() {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString());
        String ref = storage.save(107L, "errors", new byte[] {7, 8});
        assertThatThrownBy(() -> storage.read(108L, ref)).isInstanceOf(IllegalArgumentException.class);
        storage.delete(108L, ref);
        assertThat(storage.read(107L, ref)).containsExactly(7, 8);
        assertThat(ref).matches("erf-[0-9a-f-]{36}");
    }

    @Test
    void symlinkFileCannotBeReadUsedAsAttachmentOrDeletedThroughAdapter() throws Exception {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString());
        String ref = storage.save(107L, "original", new byte[] {1});
        Path file = directory.resolve("107").resolve(ref);
        Path outside = directory.resolve("outside.xlsx");
        Files.write(outside, new byte[] {9, 9});
        Files.delete(file);
        Files.createSymbolicLink(file, outside);
        assertThat(storage.exists(107L, ref)).isFalse();
        assertThatThrownBy(() -> storage.read(107L, ref)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> storage.delete(107L, ref)).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readAllBytes(outside)).containsExactly(9, 9);
    }

    @Test
    void rejectsEmptyContentInvalidOwnerAndMalformedOpaqueReferences() {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString());
        assertThatThrownBy(() -> storage.save(107L, "original", new byte[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.save(0L, "original", new byte[] {1})).isInstanceOf(IllegalArgumentException.class);
        for (String reference : java.util.List.of("../secret", "/etc/passwd", "erf-not-a-uuid")) {
            assertThat(storage.exists(107L, reference)).isFalse();
            assertThatThrownBy(() -> storage.read(107L, reference)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> storage.delete(107L, reference)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsSymlinkOwnerDirectories() throws Exception {
        Path elsewhere = Files.createTempDirectory(directory, "elsewhere");
        Files.createSymbolicLink(directory.resolve("107"), elsewhere);
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(directory.toString());
        assertThatThrownBy(() -> storage.save(107L, "errors", new byte[] {1}))
                .isInstanceOf(IllegalStateException.class);
    }
}
