package kr.ac.knue.commonfoundation.basic36;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FullTimeFacultyStatusController {
    private final FullTimeFacultyStatusService service;

    public FullTimeFacultyStatusController(FullTimeFacultyStatusService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/full-time-faculty-statuses")
    public ApiResponse<FullTimeFacultyStatusSearchResponse> listFullTimeFacultyStatuses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer baseYear,
            @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String name,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireFacultyStatusRole(servletRequest);
        validate(baseYear, size);
        return ApiResponse.ok(service.list(new FullTimeFacultyStatusSearchCriteria(page, size, baseYear,
                normalize(organizationCode), normalize(employeeNo), normalize(name))), effectiveRequestId(requestId));
    }

    private CurrentUser requireFacultyStatusRole(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R03") || currentUser.roles().contains("R04") || currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private void validate(Integer baseYear, int size) {
        if (baseYear == null) {
            throw new BusinessValidationException("전임교원 현황 기준연도를 입력하세요.",
                    List.of(new ValidationError("baseYear", "기준연도는 필수입니다.")));
        }
        if (baseYear < 1900 || baseYear > 2999) {
            throw new BusinessValidationException("전임교원 현황 기준연도가 올바르지 않습니다.",
                    List.of(new ValidationError("baseYear", "기준연도는 1900~2999 사이여야 합니다.")));
        }
        if (size != 20 && size != 50 && size != 100) {
            throw new BusinessValidationException("전임교원 현황 목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
