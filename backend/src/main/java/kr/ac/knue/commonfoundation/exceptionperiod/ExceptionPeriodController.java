package kr.ac.knue.commonfoundation.exceptionperiod;

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
public class ExceptionPeriodController {
    private static final Set<String> ADMIN_ROLES = Set.of("R04", "R09");
    private final ExceptionPeriodService service;

    public ExceptionPeriodController(ExceptionPeriodService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/exception-periods")
    public ApiResponse<ExceptionPeriodSearchResponse> listExceptionPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) Long teacherUserId,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String targetFunctionCode,
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        validatePageSize(size);
        CurrentUser user = requireExceptionPeriodRole(currentUser(servletRequest));
        return ApiResponse.ok(service.list(user, new ExceptionPeriodSearchCriteria(page, size, evaluationYear,
                teacherUserId, areaCode, targetFunctionCode, activeYn, keyword)), effectiveRequestId(requestId));
    }

    @PostMapping("/api/admin/exception-periods/save")
    public ApiResponse<ExceptionPeriodRow> saveExceptionPeriod(
            @Valid @RequestBody SaveExceptionPeriodRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        String effectiveRequestId = effectiveRequestId(requestId);
        CurrentUser user = requireExceptionPeriodRole(currentUser(servletRequest));
        return ApiResponse.ok(service.save(request, user, effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }

    private CurrentUser requireExceptionPeriodRole(CurrentUser user) {
        if (user.roles().stream().anyMatch(ADMIN_ROLES::contains)) return user;
        throw new ForbiddenException();
    }

    private void validatePageSize(int size) {
        if (size != 20 && size != 50 && size != 100) {
            throw new BusinessValidationException("예외기간 목록 표시 건수가 올바르지 않습니다.",
                    java.util.List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
