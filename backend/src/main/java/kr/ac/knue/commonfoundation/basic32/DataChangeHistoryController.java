package kr.ac.knue.commonfoundation.basic32;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataChangeHistoryController {
    private final DataChangeHistoryService service;

    public DataChangeHistoryController(DataChangeHistoryService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/data-change-histories")
    public ApiResponse<DataChangeHistorySearchResponse> listDataChangeHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String targetBusiness,
            @RequestParam(required = false) String targetKey,
            @RequestParam(required = false) Long changedBy,
            @RequestParam(required = false) String changedAtFrom,
            @RequestParam(required = false) String changedAtTo,
            @RequestParam(required = false) String changeType,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireAdmin(servletRequest);
        return ApiResponse.ok(service.list(new DataChangeHistorySearchCriteria(
                page, size, targetBusiness, targetKey, changedBy, changedAtFrom, changedAtTo, changeType), user));
    }

    @DeleteMapping("/api/admin/data-change-histories/{historyId}")
    public ApiResponse<Void> rejectDataChangeHistoryDelete() throws HttpRequestMethodNotSupportedException {
        throw new HttpRequestMethodNotSupportedException("DELETE");
    }

    private CurrentUser requireAdmin(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }
}
