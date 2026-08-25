package kr.ac.knue.commonfoundation.notices;

import java.time.LocalDateTime;

public record NoticeAttachmentRow(Long attachmentId, Long noticeId, String originalFileName, Long fileSize, LocalDateTime createdAt, Long createdBy) {
}
