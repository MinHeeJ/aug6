package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;

public record AttachmentSearchResponse(
        List<AttachmentFileRow> attachments,
        int page,
        int size,
        long totalElements) {
}
