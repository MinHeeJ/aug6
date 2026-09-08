package kr.ac.knue.commonfoundation.basic54;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkReportPermissionsSaveRequest(@NotEmpty(message = "저장할 보고서 권한을 선택하세요.")
                                               List<@Valid ReportPermissionSaveRequest> permissions,
                                               String changeReason) {
}
