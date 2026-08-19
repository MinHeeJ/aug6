package kr.ac.knue.commonfoundation.userroles;

import java.util.List;

public record UserRoleAssignmentSearchResponse(
        List<UserRoleAssignmentSummary> assignments,
        int page,
        int size,
        int totalElements
) {
}
