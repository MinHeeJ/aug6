package kr.ac.knue.commonfoundation.fileoperations;

public record AttachmentLogicalDeleteResponse(Long fileId, String deleteMethod, String deleteReason, boolean deleted) {
}
