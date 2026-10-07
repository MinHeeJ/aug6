package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Transport for employment-rate CRUD, XLSX wizard and policy-blocked bulk jobs. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        Map<String, Object> filters = filters(page, pageSize);
        filters.put("managementNo", managementNo);
        filters.put("managementItemCode", managementItemCode);
        filters.put("achievementStatus", achievementStatus);
        return ApiResponse.ok(service.list(filters, user(request, "R01", "R02", "R04")), trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.detail(achievementId, user(request, "R01", "R02", "R04")), trace(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.create(body, user(request, "R01"), trace), trace);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.update(achievementId, body, user(request, "R01"), trace), trace);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        Map<String, Object> filters = filters(page, pageSize);
        filters.put("managementNo", managementNo);
        filters.put("managementItemCode", managementItemCode);
        filters.put("achievementStatus", achievementStatus);
        return workbook(service.download(filters, user(request, "R01", "R02", "R04", "R07")), "취업률실적.xlsx");
    }

    @PostMapping("/bulk-jobs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createJob(
            @Valid @RequestBody EmploymentRateBulkJobRequest body, HttpServletRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.ok(service.createJob(body, user(request, "R07")), trace(request)));
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<Map<String, Object>> job(@PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.ok(service.job(jobId, user(request, "R07")), trace(request));
    }

    @PostMapping(value = "/excel-uploads", consumes = "multipart/form-data")
    public ApiResponse<Map<String, Object>> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return ApiResponse.ok(service.upload(file, user(request, "R07"), trace(request)), trace(request));
    }

    /** D10 wizard continuation: validation never silently commits the uploaded workbook. */
    @PostMapping("/excel-uploads/{uploadId}/commit")
    public ApiResponse<Map<String, Object>> commit(@PathVariable String uploadId, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.commit(uploadId, user(request, "R07"), trace), trace);
    }

    @GetMapping("/excel-uploads/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        return workbook(service.template(user(request, "R07")), "취업률실적_양식.xlsx");
    }

    @GetMapping("/excel-uploads/histories")
    public ApiResponse<List<Map<String, Object>>> histories(HttpServletRequest request) {
        return ApiResponse.ok(service.histories(user(request, "R07")), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors")
    public ApiResponse<List<Map<String, Object>>> errors(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(service.errors(uploadId, user(request, "R07")), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> errorFile(@PathVariable String uploadId, HttpServletRequest request) {
        return workbook(service.errorFile(uploadId, user(request, "R07")), "취업률실적_오류.xlsx");
    }

    /** Exposes only DB-resolved editable choices for the selected evaluation year, not runtime fixture options. */
    @GetMapping("/management-items")
    public ApiResponse<List<Map<String, Object>>> managementItems(
            @RequestParam String evaluationYear, HttpServletRequest request) {
        return ApiResponse.ok(service.managementItems(evaluationYear, user(request, "R01", "R02", "R04")), trace(request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> invalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ApiError.validation(
                List.of(new ValidationError("achievementDate", "날짜 형식과 입력값을 확인하세요.")))));
    }

    private CurrentUser user(HttpServletRequest request, String... roles) {
        Object principal = request.getAttribute("currentUser");
        CurrentUser user = principal instanceof CurrentUser current ? current : null;
        EmploymentRateAchievementService.requireRole(user, roles);
        return user;
    }

    private Map<String, Object> filters(int page, int size) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("page", page);
        filters.put("pageSize", size);
        return filters;
    }

    private String trace(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
    }

    private ResponseEntity<byte[]> workbook(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CONTENT_TYPE, EmploymentRateXlsxCodec.CONTENT_TYPE)
                .body(content);
    }
}
