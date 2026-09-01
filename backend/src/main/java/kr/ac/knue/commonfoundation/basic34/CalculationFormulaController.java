package kr.ac.knue.commonfoundation.basic34;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
public class CalculationFormulaController {
    private final CalculationFormulaService service;

    public CalculationFormulaController(CalculationFormulaService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/calculation-formulas")
    public ApiResponse<CalculationFormulaSearchResponse> listCalculationFormulas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId,
            @RequestParam(required = false) String formulaCode,
            @RequestParam(required = false) String calculationType,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String roundingRule,
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireCalculationFormulaAdmin(servletRequest);
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException("계산식 목록 표시 건수가 올바르지 않습니다.",
                    java.util.List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
        return ApiResponse.ok(service.list(new CalculationFormulaSearchCriteria(page, pageSize, ruleVersionId,
                formulaCode, calculationType, evaluationYear, roundingRule, activeYn, keyword)), effectiveRequestId(requestId));
    }

    @PostMapping("/api/admin/calculation-formulas")
    public ApiResponse<CalculationFormulaRow> saveCalculationFormulaContractOperation(
            @Valid @RequestBody SaveCalculationFormulaRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        return saveCalculationFormula(request, requestId, servletRequest);
    }

    @PostMapping("/api/admin/calculation-formulas/save")
    public ApiResponse<CalculationFormulaRow> saveCalculationFormula(
            @Valid @RequestBody SaveCalculationFormulaRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireCalculationFormulaAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireCalculationFormulaAdmin(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R04") || currentUser.roles().contains("R08") || currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
