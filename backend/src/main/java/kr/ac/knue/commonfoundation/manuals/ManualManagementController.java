package kr.ac.knue.commonfoundation.manuals;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManualManagementController {
    private final ManualManagementService service;

    public ManualManagementController(ManualManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/manuals")
    public ApiResponse<ManualSearchResponse> listManuals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String manualType,
            @RequestParam(required = false) String targetUser,
            @RequestParam(required = false) LocalDate effectiveDate) {
        return ApiResponse.ok(service.listManuals(page, size, manualType, targetUser, effectiveDate));
    }

    @PostMapping("/api/admin/manuals")
    public ApiResponse<ManualRow> createManual(@Valid @RequestBody ManualCreateRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.createManual(request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/admin/manuals/{manualId}/download")
    public ResponseEntity<ByteArrayResource> downloadManualFile(@PathVariable Long manualId,
            HttpServletRequest servletRequest) {
        currentUser(servletRequest);
        ManualDownload download = service.downloadManualFile(manualId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.originalFileName()).build().toString())
                .body(new ByteArrayResource(download.fileContent()));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
