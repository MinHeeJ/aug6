package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;

public record AttachmentIntegrityResultSearchResponse(
        List<AttachmentIntegrityFindingRow> results,
        int page,
        int size,
        long totalElements) {
}
