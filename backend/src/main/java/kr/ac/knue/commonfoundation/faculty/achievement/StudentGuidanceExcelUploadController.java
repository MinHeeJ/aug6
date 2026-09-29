package kr.ac.knue.commonfoundation.faculty.achievement;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadCommitResult;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorSearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadHistorySearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Restricts the reusable Excel upload workflow to the R07 student-guidance business template so
 * that generic administrative Excel endpoints are not exposed to the faculty workflow.
 */
@RestController
public class StudentGuidanceExcelUploadController {
    private static final String BUSINESS_TYPE = "STUDENT_GUIDANCE";
    private final ExcelOperationsService excelOperationsService;

    public StudentGuidanceExcelUploadController(ExcelOperationsService excelOperationsService) {
        this.excelOperationsService = excelOperationsService;
    }

    /** Downloads the selected student-guidance template after the same R07 authorization check used by the wizard. */
    @GetMapping("/api/business/student-guidance-achievements/excel-upload-templates/{templateId}/file")
    public ResponseEntity<byte[]> downloadStudentGuidanceExcelTemplate(@PathVariable String templateId, HttpServletRequest request) {
        CurrentUser user = requireStudentGuidanceExcelUser(request);
        ExcelDownloadFile file = excelOperationsService.downloadUploadTemplate(templateId, user.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    /** Validates an R07 student-guidance upload using the existing template, staging, error, and history services. */
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExcelUploadResult> createStudentGuidanceAchievementExcelUpload(
            @RequestParam(required = false) String templateId,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        CurrentUser user = requireStudentGuidanceExcelUser(request);
        return ApiResponse.ok(excelOperationsService.createExcelUpload(BUSINESS_TYPE, templateId, file, user.userId()));
    }

    /** Commits only a validation result that has no error rows; the shared service preserves all-or-nothing behavior. */
    @PostMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/commit")
    public ApiResponse<ExcelUploadCommitResult> commitStudentGuidanceAchievementExcelUpload(
            @PathVariable String uploadId,
            HttpServletRequest request) {
        CurrentUser user = requireStudentGuidanceExcelUser(request);
        return ApiResponse.ok(excelOperationsService.commitExcelUpload(uploadId, user.userId()));
    }

    /** Lists R07-visible student-guidance upload histories without exposing generic administrator endpoints. */
    @GetMapping("/api/business/student-guidance-achievements/excel-upload-histories")
    public ApiResponse<ExcelUploadHistorySearchResponse> listStudentGuidanceExcelUploadHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String uploadId,
            @RequestParam(required = false) String originalFileName,
            HttpServletRequest request) {
        requireStudentGuidanceExcelUser(request);
        return ApiResponse.ok(excelOperationsService.listExcelUploadHistories(page, size, uploadId, originalFileName));
    }

    /** Returns only the selected upload's validation errors for the user-visible error-result step. */
    @GetMapping("/api/business/student-guidance-achievements/excel-upload-errors")
    public ApiResponse<ExcelUploadErrorSearchResponse> listStudentGuidanceExcelUploadErrors(
            @RequestParam String uploadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        requireStudentGuidanceExcelUser(request);
        return ApiResponse.ok(excelOperationsService.listExcelUploadErrors(page, size, uploadId));
    }

    /** Downloads the same error rows shown in the wizard, after rechecking the restricted R07 role. */
    @GetMapping("/api/business/student-guidance-achievements/excel-upload-errors/download")
    public ResponseEntity<byte[]> downloadStudentGuidanceExcelUploadErrors(
            @RequestParam String uploadId,
            HttpServletRequest request) {
        CurrentUser user = requireStudentGuidanceExcelUser(request);
        ExcelDownloadFile file = excelOperationsService.downloadExcelUploadErrors(uploadId, user.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    private CurrentUser requireStudentGuidanceExcelUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) throw new UnauthenticatedException();
        if (currentUser.roles() == null || !currentUser.roles().contains("R07")) throw new ForbiddenException();
        return currentUser;
    }
}
