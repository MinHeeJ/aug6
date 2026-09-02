package kr.ac.knue.commonfoundation.basic43;

import java.util.List;

public record DepartmentChairConfirmationSearchResponse(
        List<DepartmentChairConfirmationRow> targets,
        int page,
        int size,
        long totalElements) {
}
