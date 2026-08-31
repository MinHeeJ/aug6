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
public class DeletedBusinessDataController {
    private final DeletedBusinessDataService service;

    public DeletedBusinessDataController(DeletedBusinessDataService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/deleted-business-data")
    public ApiResponse<DeletedBusinessDataSearchResponse> listDeletedBusinessData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String originalKey,
            @RequestParam(required = false) Long deletedBy,
            @RequestParam(required = false) String deletedAtFrom,
            @RequestParam(required = false) String deletedAtTo,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireAdmin(servletRequest);
        return ApiResponse.ok(service.list(new DeletedBusinessDataSearchCriteria(
                page, size, businessType, originalKey, deletedBy, deletedAtFrom, deletedAtTo), user));
    }

    @DeleteMapping("/api/admin/deleted-business-data/{deletedDataId}")
    public ApiResponse<Void> rejectDeletedBusinessDataDelete() throws HttpRequestMethodNotSupportedException {
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
