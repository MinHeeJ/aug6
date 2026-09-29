package kr.ac.knue.commonfoundation.studentguidance;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIdentifierFilter;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.ExcelUploadCommitResult;
import kr.ac.knue.commonfoundation.excel.ExcelUploadHistorySearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
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

/** HTTP boundary for individual student guidance and the R07 Excel validation/commit flow. */
@RestController
public class StudentGuidanceAchievementController {
    private final StudentGuidanceAchievementService achievementService;
    private final StudentGuidanceExcelUploadService excelService;
    public StudentGuidanceAchievementController(StudentGuidanceAchievementService achievementService, StudentGuidanceExcelUploadService excelService) { this.achievementService = achievementService; this.excelService = excelService; }
    /** Lists individual student-guidance achievements using the common page contract. */
    @GetMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementSearchResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request) { return ApiResponse.ok(achievementService.list(page, pageSize, currentUser(request)), RequestIdentifierFilter.requestId(request)); }
    /** Saves a student-guidance header and students in one transaction. */
    @PostMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementRow> save(@Valid @RequestBody StudentGuidanceAchievementSaveRequest body, HttpServletRequest request) { return ApiResponse.ok(achievementService.save(body, currentUser(request)), RequestIdentifierFilter.requestId(request)); }
    /** Downloads the current R07 student-guidance template without exposing its storage location. */
    @GetMapping("/api/business/student-guidance-achievements/excel-template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) { ExcelDownloadFile file = excelService.template(currentUser(request)); return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.originalFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(file.contentType())).body(file.content()); }
    /** Uploads and validates an R07 student-guidance Excel file. */
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExcelUploadResult> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) { return ApiResponse.ok(excelService.upload(file, currentUser(request)), RequestIdentifierFilter.requestId(request)); }
    /** Commits a previously valid upload after the client confirmation dialog. */
    @PostMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/commit")
    public ApiResponse<ExcelUploadCommitResult> commit(@PathVariable String uploadId, HttpServletRequest request) { return ApiResponse.ok(excelService.commit(uploadId, currentUser(request)), RequestIdentifierFilter.requestId(request)); }
    /** Exposes R07-scoped upload history including uploader and count evidence. */
    @GetMapping("/api/business/student-guidance-achievements/excel-upload-histories")
    public ApiResponse<ExcelUploadHistorySearchResponse> histories(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String uploadId, @RequestParam(required = false) String fileName, HttpServletRequest request) { return ApiResponse.ok(excelService.histories(page, pageSize, uploadId, fileName, currentUser(request)), RequestIdentifierFilter.requestId(request)); }
    /** Returns a generated error CSV without exposing storage paths or physical names. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> downloadErrors(@PathVariable String uploadId, HttpServletRequest request) { ExcelDownloadFile file = excelService.errorFile(uploadId, currentUser(request)); return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.originalFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(file.contentType())).body(file.content()); }
    private CurrentUser currentUser(HttpServletRequest request) { Object user = request.getAttribute("currentUser"); if (user instanceof CurrentUser currentUser) return currentUser; throw new UnauthenticatedException(); }
}
