package kr.ac.knue.commonfoundation.users;

import java.util.List;

public record UserSearchResponse(
        List<UserSummary> users,
        List<AvailableRole> availableRoles,
        int page,
        int size,
        int totalElements
) {
}
