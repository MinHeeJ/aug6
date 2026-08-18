package kr.ac.knue.commonfoundation.organizations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record OrganizationParentRelationRequest(
        @NotBlank(message = "상위조직코드는 필수입니다.") String parentOrganizationCode,
        @NotNull(message = "적용 시작일은 필수입니다.") LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        @NotBlank(message = "변경 사유는 필수입니다.") String changeReason) {
}
