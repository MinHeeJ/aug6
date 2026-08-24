package kr.ac.knue.commonfoundation.fileoperations;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttachmentDeleteController {
    private final AttachmentDeleteService attachmentDeleteService;

    public AttachmentDeleteController(AttachmentDeleteService attachmentDeleteService) {
        this.attachmentDeleteService = attachmentDeleteService;
    }

    @GetMapping("/api/admin/attachments/{fileId}/delete-target")
    public ApiResponse<AttachmentDeleteTargetResponse> getAttachmentDeleteTarget(@PathVariable Long fileId) {
        return ApiResponse.ok(attachmentDeleteService.getDeleteTarget(fileId));
    }

    @PostMapping("/api/admin/attachments/{fileId}/logical-delete")
    public ApiResponse<AttachmentLogicalDeleteResponse> logicallyDeleteAttachment(
            @PathVariable Long fileId,
            @RequestBody(required = false) AttachmentDeleteRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(attachmentDeleteService.logicallyDelete(fileId, request, currentUser(servletRequest).userId()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
