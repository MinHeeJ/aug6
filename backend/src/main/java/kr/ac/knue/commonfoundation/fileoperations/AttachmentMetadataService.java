package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;
import java.util.Locale;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.permissions.MenuItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachmentMetadataService {
    private static final String ATTACHMENT_ROUTE = "/admin/attachments";
    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(20, 50, 100);

    private final AttachmentFileMapper mapper;
    private final AttachmentContentStore contentStore;

    public AttachmentMetadataService(AttachmentFileMapper mapper, AttachmentContentStore contentStore) {
        this.mapper = mapper;
        this.contentStore = contentStore;
    }

    @Transactional(readOnly = true)
    public AttachmentSearchResponse listAttachments(String businessRecordId, int page, int size, CurrentUser currentUser) {
        requireAttachmentAccess(currentUser, "첨부파일 조회 권한이 없습니다.");
        String recordId = requireBusinessRecordId(businessRecordId);
        int safePage = Math.max(page, 0);
        int safeSize = validateSize(size);
        int offset = safePage * safeSize;
        List<AttachmentFileRow> attachments = mapper.listVisibleByBusinessRecordId(recordId, safeSize, offset);
        long total = mapper.countVisibleByBusinessRecordId(recordId);
        return new AttachmentSearchResponse(attachments, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public AttachmentDownloadResponse downloadAttachment(Long fileId, CurrentUser currentUser) {
        requireAttachmentAccess(currentUser, "첨부파일 다운로드 권한이 없습니다.");
        AttachmentFileInternalRow file = mapper.findInternalById(fileId);
        if (file == null || file.deletedAt() != null || isHiddenBusinessStatus(file.businessRecordStatus())) {
            throw new BusinessValidationException("다운로드할 첨부파일을 찾을 수 없습니다.",
                    List.of(new ValidationError("fileId", "조회 가능한 첨부파일이 아닙니다.")));
        }
        byte[] content = contentStore.read(file);
        return new AttachmentDownloadResponse(file.originalFilename(), contentType(file.extension()), content);
    }

    private String requireBusinessRecordId(String businessRecordId) {
        if (businessRecordId == null || businessRecordId.isBlank()) {
            throw new BusinessValidationException("업무자료 식별자를 입력하세요.",
                    List.of(new ValidationError("businessRecordId", "업무자료 식별자를 입력하세요.")));
        }
        return businessRecordId.trim();
    }

    private int validateSize(int size) {
        int effectiveSize = size == 0 ? 20 : size;
        if (!ALLOWED_PAGE_SIZES.contains(effectiveSize)) {
            throw new BusinessValidationException("목록 표시 건수는 20, 50, 100 중 하나여야 합니다.",
                    List.of(new ValidationError("size", "20, 50, 100 중 하나를 선택하세요.")));
        }
        return effectiveSize;
    }

    private void requireAttachmentAccess(CurrentUser currentUser, String message) {
        if (currentUser == null) {
            throw new ForbiddenException();
        }
        if (currentUser.roles().contains("R09") || hasMenuUrl(currentUser.menus(), ATTACHMENT_ROUTE)) {
            return;
        }
        throw new ForbiddenException();
    }

    private boolean hasMenuUrl(List<MenuItem> menus, String route) {
        if (menus == null) {
            return false;
        }
        return menus.stream().anyMatch(menu -> route.equals(menu.url()) || hasMenuUrl(menu.children(), route));
    }

    private boolean isHiddenBusinessStatus(String status) {
        return "UNPUBLISHED".equals(status) || "INACTIVE".equals(status);
    }

    private String contentType(String extension) {
        String normalized = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "zip" -> "application/zip";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
