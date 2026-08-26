package kr.ac.knue.commonfoundation.excel;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ExcelOperationsController {
    private final ExcelOperationsService service;

    public ExcelOperationsController(ExcelOperationsService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/excel-upload-templates")
    public ApiResponse<ExcelTemplateSearchResponse> listUploadTemplates(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String effectiveDate, HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(service.listUploadTemplates(page, size, businessType, effectiveDate));
    }

    @PostMapping("/api/admin/excel-upload-templates")
    public ApiResponse<ExcelTemplateRow> saveUploadTemplate(@RequestBody UploadTemplateSaveRequest body, HttpServletRequest request) {
        CurrentUser user = requireAdmin(request);
        return ApiResponse.ok(service.saveUploadTemplate(body, user.userId()));
    }

    @GetMapping("/api/admin/excel-upload-templates/{templateId}/file")
    public ResponseEntity<byte[]> downloadUploadTemplate(@PathVariable String templateId, HttpServletRequest request) {
        CurrentUser user = requireAdmin(request);
        return download(service.downloadUploadTemplate(templateId, user.userId()));
    }

    @PostMapping(value = "/api/admin/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExcelUploadResult> createExcelUpload(@RequestParam String businessType,
            @RequestParam(required = false) String templateId, @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        CurrentUser user = requireAdmin(request);
        return ApiResponse.ok(service.createExcelUpload(businessType, templateId, file, user.userId()));
    }

    @PostMapping("/api/admin/excel-uploads/{uploadId}/commit")
    public ApiResponse<ExcelUploadCommitResult> commitExcelUpload(@PathVariable String uploadId, HttpServletRequest request) {
        CurrentUser user = requireAdmin(request);
        return ApiResponse.ok(service.commitExcelUpload(uploadId, user.userId()));
    }

    @GetMapping("/api/admin/excel-upload-histories")
    public ApiResponse<ExcelUploadHistorySearchResponse> listExcelUploadHistories(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String uploadId,
            @RequestParam(required = false) String originalFileName, HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(service.listExcelUploadHistories(page, size, uploadId, originalFileName));
    }

    @GetMapping("/api/admin/excel-upload-errors")
    public ApiResponse<ExcelUploadErrorSearchResponse> listExcelUploadErrors(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam String uploadId, HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(service.listExcelUploadErrors(page, size, uploadId));
    }

    @GetMapping("/api/admin/excel-upload-errors/download")
    public ResponseEntity<byte[]> downloadExcelUploadErrors(@RequestParam String uploadId, HttpServletRequest request) {
        CurrentUser user = requireAdmin(request);
        return download(service.downloadExcelUploadErrors(uploadId, user.userId()));
    }

    @PostMapping("/api/admin/excel-downloads")
    public ApiResponse<ExcelDownloadJobRow> createExcelDownload(@RequestBody ExcelDownloadRequest body, HttpServletRequest request) {
        CurrentUser user = requireAdmin(request);
        return ApiResponse.ok(service.createExcelDownload(body, user.userId()));
    }

    private ResponseEntity<byte[]> download(ExcelDownloadFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.originalFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    private CurrentUser requireAdmin(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) throw new UnauthenticatedException();
        if (currentUser.roles() == null || !currentUser.roles().contains("R09")) throw new ForbiddenException();
        return currentUser;
    }
}
