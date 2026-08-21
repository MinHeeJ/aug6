package kr.ac.knue.commonfoundation.operations;

import java.util.List;

public record PositionAssignmentSearchResponse(
        List<PositionAssignmentRow> assignments,
        int page,
        int size,
        int totalElements
) {
}
