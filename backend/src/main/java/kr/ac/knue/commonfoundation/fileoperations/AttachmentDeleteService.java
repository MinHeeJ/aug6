package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachmentDeleteService {
    private static final String EVALUATION_CONFIRMED = "EVALUATION_CONFIRMED";
    private final AttachmentFileMapper attachmentFileMapper;

    public AttachmentDeleteService(AttachmentFileMapper attachmentFileMapper) {
        this.attachmentFileMapper = attachmentFileMapper;
    }

    @Transactional(readOnly = true)
    public AttachmentDeleteTargetResponse getDeleteTarget(Long fileId) {
        AttachmentFileRow row = attachmentFileMapper.findPublicById(fileId);
        if (row == null) {
            throw new BusinessValidationException("삭제 대상 첨부파일을 찾을 수 없습니다.",
                    List.of(new ValidationError("fileId", "삭제 대상 첨부파일이 없거나 이미 삭제되었습니다.")));
        }
        return AttachmentDeleteTargetResponse.from(row);
    }

    @Transactional
    public AttachmentLogicalDeleteResponse logicallyDelete(Long fileId, AttachmentDeleteRequest request, Long currentUserId) {
        String reason = request == null ? null : request.getDeleteReason();
        if (!hasText(reason)) {
            throw new BusinessValidationException("삭제사유를 입력하세요.",
                    List.of(new ValidationError("delete_reason", "삭제사유를 입력하세요.")));
        }
        AttachmentFileInternalRow current = attachmentFileMapper.findInternalById(fileId);
        if (current == null || current.deletedAt() != null) {
            throw new BusinessValidationException("삭제 대상 첨부파일을 찾을 수 없습니다.",
                    List.of(new ValidationError("fileId", "삭제 대상 첨부파일이 없거나 이미 삭제되었습니다.")));
        }
        if (EVALUATION_CONFIRMED.equals(current.businessRecordStatus())) {
            throw new BusinessValidationException("평가확정 자료의 첨부파일은 삭제할 수 없습니다.",
                    List.of(new ValidationError("businessRecordStatus", "평가확정 자료는 최종평가처리 취소 후에만 정정할 수 있습니다.")));
        }
        int updated = attachmentFileMapper.markLogicalDeleted(fileId, reason.trim(), currentUserId);
        if (updated != 1) {
            throw new BusinessValidationException("첨부파일 논리삭제 상태가 변경되지 않았습니다.",
                    List.of(new ValidationError("fileId", "삭제 대상 상태를 다시 확인하세요.")));
        }
        attachmentFileMapper.insertDeleteHistory(fileId, reason.trim(), currentUserId);
        return new AttachmentLogicalDeleteResponse(fileId, "LOGICAL", reason.trim(), true);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
