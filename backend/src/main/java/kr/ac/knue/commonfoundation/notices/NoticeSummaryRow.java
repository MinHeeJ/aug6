package kr.ac.knue.commonfoundation.notices;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record NoticeSummaryRow(
        Long noticeId,
        String title,
        String content,
        LocalDate publishStartDate,
        LocalDate publishEndDate,
        String importantYn,
        String status,
        LocalDateTime updatedAt,
        Long updatedBy,
        List<NoticeTargetRow> targets,
        List<NoticeAttachmentRow> attachments) {
    public NoticeSummaryRow(Long noticeId, String title, String content, LocalDate publishStartDate,
            LocalDate publishEndDate, String importantYn, String status, LocalDateTime updatedAt, Long updatedBy) {
        this(noticeId, title, content, publishStartDate, publishEndDate, importantYn, status, updatedAt, updatedBy, Collections.emptyList(), Collections.emptyList());
    }
}
