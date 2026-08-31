package kr.ac.knue.commonfoundation.basic32;

import java.util.List;

public record BusinessStatusTransitionSearchResponse(
        List<BusinessStatusTransitionRow> transitions,
        int page,
        int size,
        long totalElements) {
}
