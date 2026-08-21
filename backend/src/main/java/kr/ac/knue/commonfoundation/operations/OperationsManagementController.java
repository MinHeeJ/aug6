package kr.ac.knue.commonfoundation.operations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationsManagementController {
    private final OperationsManagementService service;

    public OperationsManagementController(OperationsManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/position-assignments")
    public ApiResponse<PositionAssignmentSearchResponse> searchPositionAssignments(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) LocalDate referenceDate, @RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.searchPositionAssignments(new AssignmentSearchCriteria(page, size, referenceDate, filter)));
    }

    @PostMapping("/api/admin/position-assignments")
    public ApiResponse<PositionAssignmentRow> savePositionAssignment(@Valid @RequestBody PositionAssignmentRequest request, HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        return ApiResponse.ok(service.savePositionAssignment(request, user.userId()));
    }

    @PutMapping("/api/admin/position-assignments/{positionAssignmentId}")
    public ApiResponse<PositionAssignmentRow> updatePositionAssignment(@PathVariable Long positionAssignmentId, @Valid @RequestBody PositionAssignmentRequest request, HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        return ApiResponse.ok(service.updatePositionAssignment(positionAssignmentId, request, user.userId()));
    }

    @GetMapping("/api/admin/duty-assignments")
    public ApiResponse<DutyAssignmentSearchResponse> searchDutyAssignments(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) LocalDate referenceDate, @RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.searchDutyAssignments(new AssignmentSearchCriteria(page, size, referenceDate, filter)));
    }

    @PostMapping("/api/admin/duty-assignments")
    public ApiResponse<DutyAssignmentRow> saveDutyAssignment(@Valid @RequestBody DutyAssignmentRequest request, HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        return ApiResponse.ok(service.saveDutyAssignment(request, user.userId()));
    }

    @PutMapping("/api/admin/duty-assignments/{dutyAssignmentId}")
    public ApiResponse<DutyAssignmentRow> updateDutyAssignment(@PathVariable Long dutyAssignmentId, @Valid @RequestBody DutyAssignmentRequest request, HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        return ApiResponse.ok(service.updateDutyAssignment(dutyAssignmentId, request, user.userId()));
    }

    @GetMapping("/api/admin/data-scope-rules")
    public ApiResponse<DataScopeRulesSearchResponse> searchDataScopeRules(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) LocalDate referenceDate, @RequestParam(required = false) String filter) {
        return ApiResponse.ok(service.searchDataScopeRules(new AssignmentSearchCriteria(page, size, referenceDate, filter)));
    }

    @PutMapping("/api/admin/data-scope-rules")
    public ApiResponse<DataScopeRuleRow> saveDataScopeRules(@Valid @RequestBody DataScopeRulesSaveRequest request, HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        return ApiResponse.ok(service.saveDataScopeRules(request, user.userId()));
    }

    private CurrentUser requireR09(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (!currentUser.roles().contains("R09")) {
                throw new ForbiddenException();
            }
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
