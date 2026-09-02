package kr.ac.knue.commonfoundation.basic43;

import java.util.List;

public record ObjectionOpinionSearchResponse(
        List<ObjectionOpinionRow> opinions,
        int page,
        int size,
        long totalElements) {
}
