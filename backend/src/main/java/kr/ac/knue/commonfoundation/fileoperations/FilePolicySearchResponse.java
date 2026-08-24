package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;

public record FilePolicySearchResponse(
        List<FilePolicyRow> policies,
        int page,
        int size,
        long totalElements) {
}
