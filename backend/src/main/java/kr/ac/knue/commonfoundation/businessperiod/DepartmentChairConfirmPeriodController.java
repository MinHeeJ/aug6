package kr.ac.knue.commonfoundation.businessperiod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepartmentChairConfirmPeriodController {
    private static final Set<String> ADMIN_ROLES = Set.of("R03", "R04", "R09");
    private final DepartmentChairConfirmPeriodService service;

    public DepartmentChairConfirmPeriodController(DepartmentChairConfirmPeriodService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/department-chair-confirm-periods")
    public ApiResponse<DepartmentChairConfirmPeriodSearchResponse> listDepartmentChairConfirmPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) String userTypeCode,
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        validatePageSize(size);
        CurrentUser user = requireBusinessPeriodRole(currentUser(servletRequest));
        return ApiResponse.ok(service.list(user, new BusinessPeriodSearchCriteria(page, size,
                evaluationYear, areaCode, organizationCode, userTypeCode, activeYn, keyword)), effectiveRequestId(requestId));
    }

    @PostMapping("/api/admin/department-chair-confirm-periods/save")
    public ApiResponse<BusinessPeriodSettingRow> saveDepartmentChairConfirmPeriod(
            @Valid @RequestBody SaveDepartmentChairConfirmPeriodRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        String effectiveRequestId = effectiveRequestId(requestId);
        CurrentUser user = requireBusinessPeriodRole(currentUser(servletRequest));
        return ApiResponse.ok(service.save(request, user, effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }

    private CurrentUser requireBusinessPeriodRole(CurrentUser user) {
        if (user.roles().stream().anyMatch(ADMIN_ROLES::contains)) {
            return user;
        }
        throw new ForbiddenException();
    }

    private void validatePageSize(int size) {
        if (size != 20 && size != 50 && size != 100) {
            throw new BusinessValidationException("학과장 확인기간 목록 표시 건수가 올바르지 않습니다.",
                    java.util.List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
