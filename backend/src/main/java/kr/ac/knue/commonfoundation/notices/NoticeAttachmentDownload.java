package kr.ac.knue.commonfoundation.notices;

public record NoticeAttachmentDownload(Long attachmentId, Long noticeId, String originalFileName, byte[] fileContent) {
}
