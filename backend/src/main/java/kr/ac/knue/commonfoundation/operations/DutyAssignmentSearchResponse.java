package kr.ac.knue.commonfoundation.operations;

import java.util.List;

public record DutyAssignmentSearchResponse(
        List<DutyAssignmentRow> assignments,
        int page,
        int size,
        int totalElements
) {
}
