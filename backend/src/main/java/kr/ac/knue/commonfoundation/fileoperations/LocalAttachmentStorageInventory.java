package kr.ac.knue.commonfoundation.fileoperations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LocalAttachmentStorageInventory implements AttachmentStorageInventory {
    @Override
    public List<StorageObjectSnapshot> listStorageObjects(List<AttachmentFileInternalRow> metadataRows) {
        Set<Path> scanRoots = new LinkedHashSet<>();
        for (AttachmentFileInternalRow row : metadataRows) {
            if (row.storagePath() != null && !row.storagePath().isBlank()) {
                scanRoots.add(Path.of(row.storagePath()).normalize());
            }
        }
        Set<String> objectRefs = new LinkedHashSet<>();
        for (Path root : scanRoots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .map(path -> path.normalize().toString())
                        .forEach(objectRefs::add);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        return objectRefs.stream().map(StorageObjectSnapshot::new).toList();
    }
}
