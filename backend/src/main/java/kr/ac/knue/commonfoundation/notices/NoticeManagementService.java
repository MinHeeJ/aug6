package kr.ac.knue.commonfoundation.notices;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeManagementService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private final NoticeManagementMapper mapper;

    public NoticeManagementService(NoticeManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public NoticeSearchResponse listNotices(int page, int pageSize, NoticeSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(pageSize) ? pageSize : 20;
        NoticeSearchCriteria normalized = new NoticeSearchCriteria(
                criteria == null ? null : criteria.publishStartDate(),
                criteria == null ? null : criteria.publishEndDate(),
                upperOrNull(criteria == null ? null : criteria.targetRoleCode()),
                blankToNull(criteria == null ? null : criteria.targetOrganizationCode()),
                criteria == null ? Boolean.TRUE : criteria.activeOnly());
        List<NoticeSummaryRow> notices = mapper.listNotices(normalized, safeSize, safePage * safeSize).stream()
                .map(this::hydrate)
                .toList();
        return new NoticeSearchResponse(notices, safePage, safeSize, mapper.countNotices(normalized));
    }

    @Transactional
    public NoticeRow createNotice(NoticeSaveRequest request, Long userId) {
        validateRequest(request);
        Long noticeId = mapper.nextNoticeId();
        mapper.insertNotice(noticeId, request, userId);
        saveChildren(noticeId, request, userId);
        return getNotice(noticeId);
    }

    @Transactional
    public NoticeRow saveNotice(Long noticeId, NoticeSaveRequest request, Long userId) {
        validateRequest(request);
        int updated = mapper.updateNotice(noticeId, request, userId);
        if (updated == 0) {
            throw new NotFoundException("공지사항을 찾을 수 없습니다.");
        }
        saveChildren(noticeId, request, userId);
        return getNotice(noticeId);
    }

    @Transactional(readOnly = true)
    public NoticeRow getNotice(Long noticeId) {
        NoticeRow row = mapper.findNotice(noticeId);
        if (row == null) {
            throw new NotFoundException("공지사항을 찾을 수 없습니다.");
        }
        return new NoticeRow(row.noticeId(), row.title(), row.content(), row.publishStartDate(), row.publishEndDate(),
                row.importantYn(), row.status(), row.createdAt(), row.createdBy(), row.updatedAt(), row.updatedBy(),
                mapper.listTargets(row.noticeId()), mapper.listAttachments(row.noticeId()));
    }

    @Transactional(readOnly = true)
    public NoticeAttachmentDownload downloadAttachment(Long noticeId, Long attachmentId, List<String> roleCodes, String organizationCode) {
        List<String> roles = roleCodes == null ? java.util.Collections.emptyList() : roleCodes.stream().map(String::toUpperCase).toList();
        if (roles.isEmpty()) {
            throw new ForbiddenException();
        }
        NoticeAttachmentDownload download = mapper.findAttachmentForDownload(noticeId, attachmentId, roles, blankToNull(organizationCode));
        if (download == null) {
            throw new ForbiddenException();
        }
        return download;
    }

    private NoticeSummaryRow hydrate(NoticeSummaryRow row) {
        return new NoticeSummaryRow(row.noticeId(), row.title(), row.content(), row.publishStartDate(), row.publishEndDate(),
                row.importantYn(), row.status(), row.updatedAt(), row.updatedBy(), mapper.listTargets(row.noticeId()), mapper.listAttachments(row.noticeId()));
    }

    private void saveChildren(Long noticeId, NoticeSaveRequest request, Long userId) {
        mapper.deleteNoticeTargets(noticeId);
        request.getTargets().forEach(target -> mapper.insertNoticeTarget(noticeId, target, userId));
        mapper.deleteNoticeAttachments(noticeId);
        request.getAttachments().forEach(attachment -> {
            byte[] content = decodeAttachment(attachment);
            mapper.insertNoticeAttachment(noticeId, sanitizeFileName(attachment.getOriginalFileName()), UUID.randomUUID().toString(),
                    "application/octet-stream", content.length, content, userId);
        });
    }

    private void validateRequest(NoticeSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "공지사항 관리에서 허용하지 않는 필드입니다.")));
        if (request.getPublishStartDate() != null && request.getPublishEndDate() != null
                && request.getPublishEndDate().isBefore(request.getPublishStartDate())) {
            fields.add(new ValidationError("publishEndDate", "게시 종료일은 게시 시작일보다 빠를 수 없습니다."));
        }
        if (request.getTargets().isEmpty()) {
            fields.add(new ValidationError("targets", "대상 역할 또는 대상 조직을 하나 이상 지정하세요."));
        }
        boolean hasRole = false;
        boolean hasOrganization = false;
        Set<String> uniqueTargets = new HashSet<>();
        for (int i = 0; i < request.getTargets().size(); i++) {
            NoticeTargetInput target = request.getTargets().get(i);
            String type = upperOrNull(target.getTargetType());
            String id = blankToNull(target.getTargetId());
            if ("ROLE".equals(type)) hasRole = true;
            if ("ORGANIZATION".equals(type)) hasOrganization = true;
            if (type != null && id != null && !uniqueTargets.add(type + ":" + id)) {
                fields.add(new ValidationError("targets[" + i + "]", "중복 대상은 저장할 수 없습니다."));
            }
        }
        if (!hasRole) {
            fields.add(new ValidationError("targets", "대상 역할을 하나 이상 지정하세요."));
        }
        if (!hasOrganization) {
            fields.add(new ValidationError("targets", "대상 조직을 하나 이상 지정하세요."));
        }
        for (int i = 0; i < request.getAttachments().size(); i++) {
            NoticeAttachmentInput attachment = request.getAttachments().get(i);
            String fileName = blankToNull(attachment.getOriginalFileName());
            if (fileName != null && (fileName.contains("/") || fileName.contains("\\"))) {
                fields.add(new ValidationError("attachments[" + i + "].originalFileName", "파일명에는 경로를 포함할 수 없습니다."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("공지사항 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private byte[] decodeAttachment(NoticeAttachmentInput attachment) {
        String base64 = blankToNull(attachment.getContentBase64());
        if (base64 != null) {
            int comma = base64.indexOf(',');
            String payload = comma >= 0 ? base64.substring(comma + 1) : base64;
            return Base64.getDecoder().decode(payload);
        }
        String text = attachment.getContentText();
        return text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
    }

    private String sanitizeFileName(String value) {
        return value == null ? "attachment.bin" : value.replace("\\", "_").replace("/", "_").trim();
    }

    private String upperOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
