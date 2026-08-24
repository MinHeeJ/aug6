package kr.ac.knue.commonfoundation.fileoperations;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Component;

@Component
public class LocalAttachmentContentStore implements AttachmentContentStore {
    @Override
    public byte[] read(AttachmentFileInternalRow file) {
        Path path = Path.of(file.storagePath(), file.storedFilename()).normalize();
        try {
            if (Files.isRegularFile(path)) {
                return Files.readAllBytes(path);
            }
        } catch (java.io.IOException exception) {
            throw new UncheckedIOException(exception);
        }
        throw new BusinessValidationException("첨부파일 저장소 객체를 찾을 수 없습니다.",
                List.of(new ValidationError("fileId", "저장소 파일을 확인하세요.")));
    }
}
