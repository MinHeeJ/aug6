package kr.ac.knue.commonfoundation.messages;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageManagementController {
    private final MessageManagementService service;

    public MessageManagementController(MessageManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/system-settings/messages")
    public ApiResponse<MessageSearchResponse> listMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String messageCode,
            @RequestParam(required = false) String messageType) {
        return ApiResponse.ok(service.listMessages(page, size, messageType, messageCode));
    }

    @PostMapping("/api/admin/system-settings/messages")
    public ApiResponse<MessageCodeRow> createMessage(@Valid @RequestBody MessageSaveRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.createMessage(request, currentUser(servletRequest).userId()));
    }

    @PutMapping("/api/admin/system-settings/messages/{messageCode}")
    public ApiResponse<MessageCodeRow> saveMessage(@PathVariable String messageCode,
            @Valid @RequestBody MessageSaveRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveMessage(messageCode, request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/system/messages/{messageCode}")
    public ApiResponse<MessageTextResponse> getMessageText(@PathVariable String messageCode) {
        return ApiResponse.ok(service.getMessageText(messageCode));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
